package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.attribution.Attribution._
import org.kiama.rewriting.PositionalRewriter._
import org.kiama.util.{Positioned, Messaging}
import org.kiama.attribution.Attributable
import edu.berkeley.cs.boom.bloomscala.stdlib.{UnknownFunction, BuiltInFunctions}

/**
 * Rewrites the AST to bind field, function, and collection references.
 */
class Namer(messaging: Messaging) {

  import messaging.message

  def resolveNames(program: Program): Program = {
    rewrite(everywhere(bindCollectionRef) <* everywhere(bindFieldRef) <* everywhere(bindFunctionRef))(program)
  }

  private val bindCollectionRef =
    rule {
      case fc: FreeCollectionRef =>
        bind(fc)
      case ftv: FreeTupleVariable =>
        bind(ftv)
    }

  private val bindFieldRef =
    rule {
      case fr: FreeFieldRef =>
        bindField(fr)
    }

  private val bindFunctionRef =
    rule {
      case fr: FreeFunctionRef =>
        bindFunction(fr)
    }

  private implicit def bind: CollectionRef => BoundCollectionRef =
    attr {
      case bound: BoundCollectionRef =>
        bound
      case tv @ FreeTupleVariable(name) => tv->lookupTupleVar(name) match {
        case (md: MissingDeclaration, _) =>
          message(tv, s"Unknown tuple variable $name")
          BoundCollectionRef(name, md, -1)
        case (cd, lambdaArgNumber) =>
          BoundCollectionRef(name, cd, lambdaArgNumber)
      }
      case cr @ FreeCollectionRef(name) => cr->lookup(name) match {
        case md: MissingDeclaration =>
          message(cr, s"Unknown collection $name")
          BoundCollectionRef(name, md, 0)
        case cd =>
          BoundCollectionRef(name, cd, 0)
      }
    }

  private implicit def bindField: FreeFieldRef => BoundFieldRef =
    attr {
      case fr @ FreeFieldRef(cr @ BoundCollectionRef(colName, decl, _), fieldName) =>
        val field = decl.getField(fieldName).getOrElse {
          message(fr, s"Collection $colName does not have field $fieldName")
          new UnknownField
        }
        new BoundFieldRef(cr, fieldName, field)
    }

  private implicit def bindFunction: FreeFunctionRef => BoundFunctionRef =
    attr {
      case fr @ FreeFunctionRef(name) =>
        val func = BuiltInFunctions.nameToFunction.getOrElse(name, {
          message(fr, s"Could not find function named $name")
          UnknownFunction
        })
        BoundFunctionRef(name, func)
    }

  private lazy val tupleVarBindingTargets: MappedCollectionTarget => Seq[CollectionRef] =
    attr {
      case MappedEquijoin(a, b, _, _, _, _) => Seq(a, b)
      case JoinedCollection(a, b, _) => Seq(a, b)
      case cr: CollectionRef => Seq(cr)
    }

  private def checkTupleVarCount(expectedSize: Int, actual: List[String], loc: Positioned) {
    if (actual.size != expectedSize) {
      message(loc, s"Wrong number of tuple vars; expected $expectedSize but got ${actual.size}")
    }
  }

  private lazy val lookupTupleVar: String => Attributable => (CollectionDeclaration, Int) =
    paramAttr {
      name => {
        case mej @ MappedEquijoin(a, b, _, _, tupleVars, _) =>
          val targets = tupleVarBindingTargets(mej)
          checkTupleVarCount(targets.size, tupleVars, mej)
          val lambdaArgNumber = tupleVars.indexOf(name)
          if (lambdaArgNumber == -1) {
            (new MissingDeclaration, -1)
          } else {
            (bind(targets(lambdaArgNumber)).collection, lambdaArgNumber)
          }
        case mc @ MappedCollection(target, tupleVars, _) =>
          val targets = tupleVarBindingTargets(target)
          checkTupleVarCount(targets.size, tupleVars, mc)
          if (tupleVars.head != name) {
            (new MissingDeclaration, -1)
          } else {
            (bind(targets.head).collection, 0)
          }
        case program: Program =>
          throw new IllegalStateException("A tuple variable appeared in an invalid context")
        case n =>
          n.parent->lookupTupleVar(name)
      }
    }

  private lazy val lookup: String => Attributable => CollectionDeclaration =
    paramAttr {
      name => {
        case program: Program =>
          val decl = program.declarations.find(_.name == name)
          decl.getOrElse(new MissingDeclaration())
        case n => n.parent->lookup(name)
      }
    }

}
