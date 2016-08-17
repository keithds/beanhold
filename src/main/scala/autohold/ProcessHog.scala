package autohold

/**
  * Created by ltf on 7/8/16.
  */
object ProcessHog {
  def hogCPU(millis: Int): Unit = {
    val quitTime = System.currentTimeMillis() + millis
    while (System.currentTimeMillis() < quitTime) {
      val x = 1.0 + 1.1
    }
  }
}
