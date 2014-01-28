package edu.berkeley.cs.boom.bloomscala.typing


object CollectionType extends Enumeration {
  type CollectionType = Value
  val Table, Scratch = Value
  val nameToType: Map[String, CollectionType] = Map(
    "table" -> Table,
    "scratch" -> Scratch
  )
}
