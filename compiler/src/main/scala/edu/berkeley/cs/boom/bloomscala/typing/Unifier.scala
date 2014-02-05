package edu.berkeley.cs.boom.bloomscala.typing

import scala.util.{Success, Failure, Try}
import edu.berkeley.cs.boom.bloomscala.UnificationError


object Unifier {

  type Unifier = Map[BloomType, BloomType]

  /**
   * Returns a set of substitutions that makes the types equal,
   * or fails if no unifier exists.
   */
  def unify(a: BloomType, b: BloomType): Try[Unifier] =
    unify(a, b, Map.empty)

  private def unify(ta: BloomType, tb: BloomType, bindings: Unifier): Try[Unifier] = {
    // This basic unification algorithm is described on Slide 9 of
    // http://inst.eecs.berkeley.edu/~cs164/sp11/lectures/lecture22.pdf
    val a = bindings.getOrElse(ta, ta)
    val b = bindings.getOrElse(tb, tb)
    if (a eq b)
      Success(Map.empty)
    else if (a.isInstanceOf[TypeParameter])
      Success(Map(a -> b))
    else if (b.isInstanceOf[TypeParameter])
      Success(Map(b -> a))
    else {
      val bindings = Map(b -> a)  // Prevents infinite recursion
      // Check that the binding of b to a was actually okay.
      // Presumably both A and B are type constructors, so check that we can unify each pair of
      // type parameters
      a match {
        case FunctionType(aArgTypes, aReturnType, _) =>
          b match {
            case FunctionType(bArgTypes, bReturnType, _) =>
              val aTypes = aArgTypes ++ List(aReturnType)
              val bTypes = bArgTypes ++ List(bReturnType)
              val recursiveUnifiers = aTypes.zip(bTypes).map(x => unify(x._1, x._2, bindings).get).flatMap(_.toSeq).toSet
              // Ensure that none of the bindings conflict:
              val unifier = recursiveUnifiers.toMap
              if (unifier.size != recursiveUnifiers.size)
                Failure(new UnificationError("Unification failed"))
              else
                Success(unifier)
            case _ =>
              Failure(new UnificationError("Cannot unify functions with primitives"))
          }
        case _ =>
          Failure(new UnificationError("Unification failed"))
      }
    }
  }

}
