package autohold

import akka.actor.{Props, Actor}
import beanmessages.Beanhold.CalculateJump

object JumpCalculator {

  def props = Props(new JumpCalculator())

}
class JumpCalculator extends Actor {
  import JumpCalculator._
  def receive = {
    case cj: CalculateJump => //(maxJump, randomDouble) =>
      val jumpInMillis = (cj.getRandomDouble * cj.getMaxJumpMillis).toInt
      ProcessHog.hogCPU(jumpInMillis)
      println("Jump took " + jumpInMillis + " milis")
      sender ! BeansSimulator.buildJumpVal(jumpInMillis)
  }
}

