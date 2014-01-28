package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.attribution.Attribution._
import org.kiama.rewriting.PositionalRewriter._
import org.kiama.util.{Positioned, Messaging}
import org.kiama.attribution.Attributable

class Namer(messaging: Messaging) {

  import messaging.message

  def resolveNames(program: Program): Program = {
    rewrite(everywhere(bindCollectionRef) <* everywhere(bindFieldRef))(program)
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

  private implicit def bind: CollectionRef => BoundCollectionRef =
    attr {
      case bound: BoundCollectionRef =>
        bound
      case tv @ FreeTupleVariable(name) => tv->lookupTupleVar(name) match {
        case md: MissingDeclaration =>
          message(tv, s"Unknown tuple variable $name")
          BoundCollectionRef(name, md)
        case cd =>
          BoundCollectionRef(name, cd)
      }
      case cr @ FreeCollectionRef(name) => cr->lookup(name) match {
        case md: MissingDeclaration =>
          message(cr, s"Unknown collection $name")
          BoundCollectionRef(name, md)
        case cd =>
          BoundCollectionRef(name, cd)
      }
    }

  private implicit def bindField: FreeFieldRef => BoundFieldRef =
    attr {
      case fr @ FreeFieldRef(cr @ BoundCollectionRef(colName, decl), fieldName) =>
        val field = decl.getField(fieldName).getOrElse {
          message(fr, s"Collection $colName does not have field $fieldName")
          new UnknownField
        }
        new BoundFieldRef(cr, fieldName, field)
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

  private lazy val lookupTupleVar: String => Attributable => CollectionDeclaration =
    paramAttr {
      name => {
        case mej @ MappedEquijoin(a, b, _, _, tupleVars, _) =>
          val targets = tupleVarBindingTargets(mej)
          checkTupleVarCount(targets.size, tupleVars, mej)
          val bindings = tupleVars.zip(targets).toMap
          bindings.get(name).map(bind(_).collection).getOrElse(new MissingDeclaration)
        case mc @ MappedCollection(target, tupleVars, _) =>
          val targets = tupleVarBindingTargets(target)
          checkTupleVarCount(targets.size, tupleVars, mc)
          val bindings = tupleVars.zip(targets).toMap
          bindings.get(name).map(bind(_).collection).getOrElse(new MissingDeclaration)
        case n =>
          n.parent->lookupTupleVar(name)
        case program: Program =>
          throw new IllegalStateException("A tuple variable appeared in an invalid context")
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
