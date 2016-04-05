package autohold

/**
  * Created by ltf on 12/10/15.
  */
object ProcessHog extends App {
  val in = System.currentTimeMillis()
  Thread.currentThread().setPriority(Thread.MAX_PRIORITY)
  for (x <- 1 to 200000000) {
    val y = x + 1
  }
  println("It took " + (System.currentTimeMillis() - in) + " milliseconds")
}
