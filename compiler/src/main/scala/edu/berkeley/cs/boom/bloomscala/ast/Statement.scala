package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.ast.BloomOp.BloomOp
import edu.berkeley.cs.boom.bloomscala.typing.FunctionTypes

case class Statement(
    lhs: CollectionRef,
    op: BloomOp,
    rhs: StatementRHS,
    number: Int = -1)
  extends Node


/** Valid RHS's of statements */
trait StatementRHS extends Node

/** Valid targets of the map ({|| []}) operation */
trait MappedCollectionTarget extends Node

/** Collections that are derived through operations like map and join */
trait DerivedCollection extends StatementRHS with MappedCollectionTarget with Node

case class MappedCollection(collection: MappedCollectionTarget, tupleVars: List[String],
                            rowExpr: RowExpr) extends DerivedCollection

case class NotIn(a: CollectionRef, b: CollectionRef) extends DerivedCollection

case class JoinedCollections(collections: List[CollectionRef],
                             predicate: List[Predicate],
                             tupleVars: List[String],
                             rowExpr: RowExpr) extends DerivedCollection

/**
 * Implements GROUP BY with exemplary aggregates.
 *
 * @param collection the collection being aggregated
 * @param groupingCols the grouping columns
 * @param chooseExpr the expression that's used in the aggregation function
 * @param func a function of type [[FunctionTypes.exemplaryAggregate]]
 */
case class ChooseCollection(collection: CollectionRef,
                            groupingCols: List[FieldRef],
                            chooseExpr: ColExpr,
                            func: FunctionRef) extends DerivedCollection