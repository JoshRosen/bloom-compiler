package edu.berkeley.cs.boom.bloomscala

import org.scalatest.{Matchers, BeforeAndAfterEach, FunSuite}
import org.kiama.util.Messaging


class BloomScalaSuite extends FunSuite with BeforeAndAfterEach with Matchers {
  protected implicit val messaging = new Messaging

  override def afterEach() {
    messaging.resetmessages()
  }
}
