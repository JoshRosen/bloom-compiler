package edu.berkeley.cs.boom.bloomscala

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.kiama.util.Messaging


class BloomScalaSuite extends FunSuite with BeforeAndAfterEach {
  protected implicit val messaging = new Messaging

  override def afterEach() {
    messaging.resetmessages()
  }
}
