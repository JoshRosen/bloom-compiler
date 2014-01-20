package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attribution._
import org.kiama.rewriting.Rewriter._
import org.kiama.util.Messaging
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

  private lazy val shortNameBindings: MappedCollectionTarget => Seq[CollectionRef] =
    attr {
      case MappedEquijoin(a, b, _, _, _, _) => Seq(a, b)
      case JoinedCollection(a, b, _) => Seq(a, b)
      case cr: CollectionRef => Seq(cr)
    }

  private lazy val lookup: String => Attributable => CollectionDeclaration =
    paramAttr {
      name => {
        // TODO: When inside of mappedCollection body, we should ONLY allow reference to the short names
        case mej @ MappedEquijoin(a, b, _, _, shortNames, _) =>
          if (shortNames.size != 2) {
            message(mej, s"Wrong number of short names; expected 2 " +
              s"but got ${shortNames.size}")
          }
          val bindings = Map(shortNames(0) -> a, shortNames(1) -> b)
          bindings.get(name).map(bind(_).collection).getOrElse(mej.parent->lookup(name))
        case mc @ MappedCollection(target, shortNames, _) =>
          val shortNameTargets: Seq[CollectionRef] = shortNameBindings(target)
          if (shortNameTargets.size != shortNames.size) {
            message(mc, s"Wrong number of short names; expected ${shortNameTargets.size} " +
                        s"but got ${shortNames.size}")
          }
          val bindings = shortNames.zip(shortNameTargets).toMap
          bindings.get(name).map(bind(_).collection).getOrElse(mc.parent->lookup(name))
        case program: Program =>
          val decl = program.declarations.find(_.name == name)
          decl.getOrElse(new MissingDeclaration())
        case n => n.parent->lookup(name)
      }
    }

}
