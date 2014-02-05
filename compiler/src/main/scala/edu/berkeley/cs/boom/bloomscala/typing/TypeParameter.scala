package edu.berkeley.cs.boom.bloomscala.typing


/**
 * Represents a type parameter.
 *
 * Equality is based on reference equality; the name is ignored.
 * When creating a new parameterized type, please create a new TypeParameter;
 * using the same TypeParameter to define multiple parameterized types may
 * break the unification algorithm.
 *
 * @param name name used in debug messages
 */
class TypeParameter(name: String) extends BloomType {
  override def toString: String = s"TypeParameter($name)"
}