package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.analysis.{Stratum, Stratifier}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.{immutable, mutable}
import edu.berkeley.cs.boom.bloomscala.ast.CollectionDeclaration
import edu.berkeley.cs.boom.bloomscala.typing.CollectionType

class DataflowGraph(stratifier: Stratifier) {
  val nextElementId = new AtomicInteger(0)
  val elements = mutable.HashSet[DataflowElement]()  // Not thread-safe (doesn't matter for now)

  def stratifiedElements: Seq[(Stratum, mutable.Set[DataflowElement])] = {
    elements.groupBy(_.stratum).toSeq.sortBy(_._1)
  }

  def statefulElements: Set[StatefulDataflowElement] =
    elements.filter(_.isInstanceOf[StatefulDataflowElement]).map(_.asInstanceOf[StatefulDataflowElement]).toSet


  def invalidationLookupTable: immutable.Map[StatefulDataflowElement, Set[StatefulDataflowElement]] = {
    statefulElements.map(elem => (elem, InvalidationAnalyzer.invalidateSet(Set(elem)) -- Set(elem))).toMap
  }

  def rescanLookupTable: immutable.Map[StatefulDataflowElement, Set[StatefulDataflowElement]] = {
    statefulElements.map(elem => (elem, InvalidationAnalyzer.rescanSet(Set(elem)) -- Set(elem))).toMap
  }

  val tables: mutable.Map[CollectionDeclaration, Table] =
    mutable.HashMap[CollectionDeclaration, Table]().withDefault { decl =>
      val table = Table(decl)(this, stratifier.collectionStratum(decl))
      tables(decl) = table
      elements += table
      table
    }

  val inputs: mutable.Map[CollectionDeclaration, InputElement] =
    mutable.HashMap[CollectionDeclaration, InputElement]().withDefault { decl =>
      val input = InputElement(decl)(this, stratifier.collectionStratum(decl))
      inputs(decl) = input
      elements += input
      input
    }

  val outputs: mutable.Map[CollectionDeclaration, OutputElement] =
    mutable.HashMap[CollectionDeclaration, OutputElement]().withDefault { decl =>
      val output = OutputElement(decl)(this, stratifier.collectionStratum(decl))
      outputs(decl) = output
      elements += output
      output
    }

  val scannableCollections: mutable.Map[CollectionDeclaration, ScannableDataflowElement] =
    mutable.HashMap[CollectionDeclaration, ScannableDataflowElement]().withDefault { decl =>
      val newElem = decl.collectionType match {
        case CollectionType.Table => tables(decl)
        case CollectionType.Input => inputs(decl)
      }
      scannableCollections(decl) = newElem
      newElem
    }

  val collections: mutable.Map[CollectionDeclaration, DataflowElement] =
    mutable.HashMap[CollectionDeclaration, DataflowElement]().withDefault { decl =>
      val newElem = decl.collectionType match {
        case CollectionType.Table => tables(decl)
        case CollectionType.Input => inputs(decl)
        case CollectionType.Output => outputs(decl)
      }
      newElem
    }

}