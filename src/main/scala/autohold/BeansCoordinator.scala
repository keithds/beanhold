package autohold

import java.time.Duration

import akka.actor._
import akka.remote.RemoteScope
import devsmodel._

class BeansCoordinator(i: Duration, randActor: ActorRef, override val simLogger: ActorRef) extends ModelCoordinator(i, randActor, simLogger) {
    debug = true
    val gridData = for (i <- 0 to PholdParameters.gridSizeX - 1) yield {
      val actors = for (j <- 0 to PholdParameters.gridSizeY - 1) yield {
        val location = LPLoc(i, j)
        val lpProperties = new BeanProperties(PholdParameters.defaultComputationGrain, location, PholdParameters.neighborDistance)
        val lpState = BeansState(DynamicStateVariable(initialTime, List()))
        val initialBeanJump = new InternalEvent(initialTime, InitialBeanJumpData())
        val initialEvents = InitialEvents(List(initialBeanJump))

        val lpSimulator =  (j%2 == 0 && PholdParameters.distributed == true)  match {
          case true =>
            context.actorOf(Props(new BeansSimulator(lpProperties, initialTime, lpState, initialEvents, randActor, simLogger)), name = "LP" + i + "-" + j)
          case false =>
            println("Creating remote actor")
            val props = Props(classOf[BeansSimulator], lpProperties, initialTime, lpState, initialEvents, randActor, simLogger)
              .withDeploy(Deploy(scope = RemoteScope(PholdParameters.remoteAddress)))
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
  override def handleExternalEvent(externalEvent: ExternalEvent[_]): Unit = {}

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
  override def handleOutputEvent(eventSender: ActorRef, output: OutputMessage[_]): Unit = {
    output.output match {
      case b: BeanOutData =>
          val receiveActor: ActorRef = grid.itemAt(b.destination.x, b.destination.y)
          sendEventMessage(b.beanNumber, output.t, receiveActor)
    }
  }
}
