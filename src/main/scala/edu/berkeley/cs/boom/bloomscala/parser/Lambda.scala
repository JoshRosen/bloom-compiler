package edu.berkeley.cs.boom.bloomscala.parser

import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.typing.BloomType

/**
 * Represents a UDF in Bloom program, such as map functions.
 *
 * The type of the UDF was inferred by resolving collection references.
 */
abstract class Lambda(
    paramTypes: List[BloomType],
    paramNames: List[String],
    returnType: BloomType,
    expr: Expr)