package edu.berkeley.cs.boom.bloomscala.typing

import scala.util.{Success, Failure, Try}
import scalaz._
import Scalaz._
import scalaz.contrib.std.utilTry._
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
              if (aTypes.size != bTypes.size)
                return Failure (new UnificationError("Cannot unify functions of different arities"))
              val recursiveUnifiers: Try[Set[(BloomType, BloomType)]] =
                aTypes.zip(bTypes).map(x => unify(x._1, x._2, bindings)).sequence.map(_.flatMap(_.toSeq).toSet)
              recursiveUnifiers.flatMap { recUnifiers =>
                // Ensure that none of the bindings conflict:
                val unifier = recUnifiers.toMap
                if (unifier.size != recUnifiers.size)
                  Failure(new UnificationError("Unification failed"))
                else
                  Success(unifier)
              }
            case _ =>
              Failure(new UnificationError("Cannot unify functions with primitives"))
          }
        case _ =>
          Failure(new UnificationError("Unification failed"))
      }
    }
  }

}
