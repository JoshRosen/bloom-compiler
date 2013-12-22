package edu.berkeley.cs.boom.bloomscala.examples

import edu.berkeley.cs.boom.bloomscala.{Rule, Bud}
import edu.berkeley.cs.boom.bloomscala.collections.Table

object ShortestPaths {
  implicit val bud = new Bud()

  type Node = Char
  type Cost = Int

  case class Link(from: Node, to: Node, cost: Cost)
  implicit def linkFromTuple(tuple: (Node, Node, Cost)): Link = Link.tupled(tuple)
  case class Path(from: Node, to: Node, next: Node, cost: Cost)
  implicit def pathFromTuple(tuple: (Node, Node, Node, Cost)): Path = Path.tupled(tuple)

  val link = new Table[Link]
  val path = new Table[Path]
  val shortest = new Table[Path]

  val j = link.join(path, _.from, (x: Path) => x.to)

  val strata0Rules = Seq[Rule](
    link <= ('a', 'b', 1),
    link <= ('a', 'b', 3),
    link <= ('b', 'c', 1),
    link <= ('c', 'd', 1),
    link <= ('d', 'e', 1),
    path <= link.map {l => (l.from, l.to, l.to, l.cost) },
    path <= j.map { case (l, p) =>
      (l.from, p.to, p.from, p.cost + l.cost)
    },
    shortest <= path.reduceByKey{ Seq(_, _).minBy(_.cost)

    }
  )
  bud.addStrata(strata0Rules)

  def main(args: Array[String]): Unit = {
    bud.tick()
    link.foreach(println)
    println()
    path.foreach(println)
  }
}