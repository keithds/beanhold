package autohold

import java.time.Duration
import java.util

import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import beanmessages.Beanhold._
import com.google.protobuf.{GeneratedMessage, Any}
import devsmodel._
import dmfmessages.DMFSimMessages._
import simutils._
import scala.collection.immutable.ListSet
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

class BeansCoordinator(i: Duration, remoteWorkersListOption: Option[List[Address]], jumpCalculator: ActorRef, randActor: ActorRef, override val simLogger: ActorRef, pHoldParameters: PholdParameters)
  extends ModelCoordinator(i, randActor, simLogger) with BeanMessageConverter {
    debug = true
  class JumpCounter extends Actor {
    var jumps: Int = 0
    def receive = {
      case j: JumpCount =>
        jumps = jumps + 1
        println("Jump " + jumps)
    }
  }


  var index = 0
  val jumpCount = BeansSimulation.system.actorOf(Props(new JumpCounter))
    val gridData = for (i <- 0 to pHoldParameters.getGridSizeX - 1) yield {
      val actors = for (j <- 0 to pHoldParameters.getGridSizeY - 1) yield {
        val location = BeansSimulator.buildLPLoc(i, j)
        val lpProperties = BeansSimulator.buildBeanProperties(pHoldParameters, location)
        val lpState = BeansSimulator.buildBeans(initialTime, List())
        val initialBeanJump = ModelSimulator.buildDEVSEventData(DEVSEventData.EventType.INTERNAL, initialTime, BeansSimulator.buildInitialBeanJumpData)
        val initialEvents = ModelSimulator.buildInitialEvents(Seq(initialBeanJump))

        val lpSimulator =  (pHoldParameters.getDistributed == true)  match {
          case false =>
            context.actorOf(Props(new BeansSimulator(lpProperties, initialTime, lpState, initialEvents, randActor, simLogger, jumpCount, jumpCalculator)), name = "LP" + i + "-" + j)
          case true =>
            val remoteWorkersList = remoteWorkersListOption.getOrElse(throw new SynchronizationException("Distributed simulation with no remote workers list"))
            if (remoteWorkersList.size == 0) throw new SynchronizationException("Distributed simulation with no workers on remote workers list")
            implicit val timeout = Timeout(1 seconds)
            println("Creating remote actor")
            val address = remoteWorkersList(index)
            index = index match {
              case x if x < remoteWorkersList.size - 1 => x+1
              case _ => 0
            }
            val props = Props(classOf[BeansSimulator], lpProperties, initialTime, lpState, initialEvents, randActor, simLogger, jumpCount, jumpCalculator)
              .withDeploy(Deploy(scope = RemoteScope(address)))
            println(props)
            context.actorOf(props, name = "LP" + i + "-" + j)

        }
        println(lpSimulator.path)
        nextMap.put(lpSimulator, initialTime)
        lpSimulator
      }
      actors.toVector
    }

    val grid = new Grid[ActorRef](gridData.toVector)



  /**
   * This is another important abastract method that must be overridden by subclasses of ModelCoordinator.  This function
   * will route any external event messages to the appropriate subordinate models.  It will process those events
   * through a translation function as necessary.
   *
   * If an [[EventMessage]] is sent to a model, send it using the [[sendEventMessage()]] method.
   *
   * @param externalEvent  The external event to be handled
   */

  override def handleExternalEvent[E <: GeneratedMessage](externalEvent: ExternalEvent[E]): Unit = {}

  /**
   * This is a very important abstract method that must be overridden by any subclasses of ModelCoordinator.  This function
   * will route output messages from subordinate models to the appropriate destinations, whether they be other subordinate models
   * or to the parent model, transitioning the outputs through a translation function as necessary.  In the words of Ziegler
   * and Chow's paper, output is received from a specific subordinate model, i.  For all j models in the influence set Ii,
   * first send the output through an i to j translation Zij before sending the message to j.
   *
   * As an [[EventMessage]] is sent to the influenced model j, assign a unique eventIndex to the message and add the appropriate
   * [[AwaitingEventBagging]] data to the [[awaitingBagEvents]] list.  In addition, add the model to the [[influences]] list
   *
   * Furthermore, if self is also  in the influence set Ii, the message must also be transmitted upward to the parent
   * coordinator after going through an i to self translation Zi,self.
    *
    * @param eventSender  The subordinate model from which the output is recieved
   * @param output  The output message received
   */
  override def handleOutputEvent(eventSender: ActorRef, output: OutputMessage): Unit = {
    convertOutput(output.getOutput) match {
      case b: BeanOutData =>
          val t = Duration.parse(output.getTimeString)
          val receiveActor: ActorRef = grid.itemAt(b.getDestination.getX, b.getDestination.getY)
          sendEventMessage(b.getBeanNumber, t, receiveActor)
    }
  }
}
