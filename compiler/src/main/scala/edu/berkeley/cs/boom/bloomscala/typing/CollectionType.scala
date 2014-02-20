package edu.berkeley.cs.boom.bloomscala.typing


object CollectionType extends Enumeration {
  type CollectionType = Value
  val Table, Scratch, Input, Output, Channel = Value
  val nameToType: Map[String, CollectionType] = Map(
    "table" -> Table,
    "scratch" -> Scratch,
    "input" -> Input,
    "output" -> Output,
    "channel" -> Channel
  )
  val validLHSTypes = Set(Table, Output, Channel, Scratch)
  val validRHSTypes = Set(Table, Input, Channel, Scratch)
}
