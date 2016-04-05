package autohold

import java.time.Duration

import akka.actor.{Props, ActorRef}
import devsmodel.RootCoordinator
import simutils.DesignPointIteration

/**
  * The top-level coordinator for a bean simulation
  *
  * @param i  Initial time
  * @param stopDuration Stopping time for the simulation
  * @param randSeed Random seed for the simulation
  * @param randStreamSize Number of values in the random stream for the [[simutils.random.SimRandom]] object
  * @param simLogger The logger for this simulation
  */
class BeanRoot(i: Duration,
               stopDuration: Duration,
               randSeed: Long,
               randStreamSize: Long,
               override val designPointIteration: DesignPointIteration,
               override val simLogger: ActorRef) extends RootCoordinator(i, stopDuration, randSeed, randStreamSize, designPointIteration, simLogger) {

  override val topCoordinator = context.actorOf(Props(new BeansCoordinator(initialTime, randomActor, simLogger)), name = "PHoldCoordinator")
}
