package autohold

import java.time.Duration

import akka.actor.{Address, Props, ActorRef}
import beanmessages.Beanhold.PholdParameters
import devsmodel.RootCoordinator
import dmfmessages.DMFSimMessages._

/**
  * The top-level coordinator for a bean simulation
  *
  * @param i  Initial time
  * @param randSeed Random seed for the simulation
  * @param randStreamSize Number of values in the random stream for the [[simutils.random.SimRandom]] object
  * @param simLogger The logger for this simulation
  */
class BeanRoot(i: Duration,
               randSeed: Long,
               randStreamSize: Long,
               val remoteWorkersListOption: Option[List[Address]],
               val jumpCalculator: ActorRef,
               override val designPointIteration: DesignPointIteration,
               override val simLogger: ActorRef,
               val pHoldParameters: PholdParameters,
               stopTime: Duration) extends RootCoordinator(i, stopTime, randSeed, randStreamSize, designPointIteration, simLogger) {

  override val topCoordinator = context.actorOf(Props(new BeansCoordinator(initialTime, remoteWorkersListOption, jumpCalculator, randomActor, simLogger, pHoldParameters)), name = "PHoldCoordinator")
}
