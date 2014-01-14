package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import org.kiama.attribution.Attributable

class Namer(messaging: Messaging) {

  import messaging.message

  implicit def collectionDeclaration: CollectionRef => CollectionDeclaration =
    attr {
      case cr @ CollectionRef(name) => cr->lookup(name) match {
        case md: MissingDeclaration =>
          message(cr, s"Unknown collection $name")
          md
        case cd => cd
      }
    }

  implicit def fieldDeclaration: FieldRef => Field =
    attr {
      case fr @ FieldRef(collectionRef, fieldName) =>
        val collection = collectionDeclaration(collectionRef)
        collectionRef.getField(fieldName).getOrElse {
          message(fr, s"Collection ${collection.name} does not have field $fieldName")
          new UnknownField
        }
    }

  lazy val shortNameBindings: MappedCollectionTarget => Seq[CollectionRef] =
    attr {
      case JoinedCollection(a, b, _) => Seq(a, b)
      case cr: CollectionRef => Seq(cr)
    }

  lazy val lookup: String => Attributable => CollectionDeclaration =
    paramAttr {
      name => {
        case mc @ MappedCollection(target, shortNames, _) =>
          val shortNameTargets: Seq[CollectionRef] = shortNameBindings(target)
          if (shortNameTargets.size != shortNames.size) {
            message(mc, s"Wrong number of short names; expected ${shortNameTargets.size} " +
                        s"but got ${shortNames.size}")
          }
          val bindings = shortNames.zip(shortNameTargets).toMap
          bindings.get(name).map(_->collectionDeclaration).getOrElse(mc.parent->lookup(name))
        case program: Program =>
          val decl = program.declarations.find(_.name == name)
          decl.getOrElse(new MissingDeclaration())
        case n => n.parent->lookup(name)
      }
    }

}
