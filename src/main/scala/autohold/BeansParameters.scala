package autohold

import java.time.Duration

import akka.actor._

/**
 * Created by ltf on 5/17/15.
 */


case class ComputationGrain(millis: Long, nanos: Int)

case class JumpCount(jumpCount: Int)
object PholdParameters {
  var jumps = 0
  val gridSizeX = 4
  val gridSizeY = 4
  val messagesPerGrid = 1
  val neighborDistance = 1
  val jumpOutProbability = 0.1
  val maxTimeIncrement = 1
  val randomStopTime = Duration.ofSeconds(10)
  val random = false
  val distributed = false
  val remoteAddress = Address("akka.tcp", "BeanSystem", "192.168.4.40", 5150)
  def stopTime: Duration = {
    random match {
      case true => randomStopTime
      case false => Duration.ofSeconds(Math.max(gridSizeX, gridSizeY) - 1)
    }
  }
  val zeroComputationGrain = ComputationGrain(0, 0)
  val defaultComputationGrain = ComputationGrain(100, 0)
  val gridData = for (i <- 0 to PholdParameters.gridSizeX - 1) yield {
    val locations = for (j <- 0 to PholdParameters.gridSizeY - 1) yield {
      LPLoc(i, j)
    }
    locations.toVector
  }

  val grid = new Grid[LPLoc](gridData.toVector)

  def getNeighbors(loc: LPLoc) = {
    grid.getNeighbors(loc, neighborDistance)
  }

  class JumpCounter extends Actor {
    var jumps: Int = 0
    def receive = {
      case JumpCount(_) =>
        jumps = jumps + 1
        println("Jump " + jumps)
    }
  }

  val jumpCount = BeansSimulation.system.actorOf(Props(new JumpCounter))

}
