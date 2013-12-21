package edu.berkeley.cs.boom.bloomscala.collections

import edu.berkeley.cs.boom.bloomscala.Bud

class Scratch[T](implicit bud: Bud) extends Table[T] {

  private[bloomscala] def clear() {
    storage.clear()
  }

  override def tick() {
    clear()
    super.tick()
  }

}