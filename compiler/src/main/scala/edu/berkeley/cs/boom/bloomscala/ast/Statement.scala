package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.ast.BloomOp.BloomOp

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

case class ChooseCollection(collection: CollectionRef,
                            groupingCols: List[FieldRef],
                            func: FunctionCall) extends DerivedCollection