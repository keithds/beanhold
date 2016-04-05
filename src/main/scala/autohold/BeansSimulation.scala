package autohold

import java.time.Duration
import com.typesafe.config.ConfigFactory
import devsmodel._
import akka.actor._
import simutils._

object BeansSimulation extends App {

  val config: String =
    """
      akka {
        loggers = ["akka.event.slf4j.Slf4jLogger"]
        loglevel = "DEBUG"
        logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
        debug {
          receive = on
          autoreceive = on
          lifecycle = on
        }
      }
    """
  val configuration = ConfigFactory.parseString(config)

  /**
    * Start the ActorSystem
    */
  val system = ActorSystem("BeanSystem", configuration)

  /**
    * Create an [[UnhandledMessagesListener]] that logs unhandled messages and stops the simulation.  This behavior
    * can be overridden, but unhandled messages are likely an error in the simulation
    */
  val unhandledMessageListener = system.actorOf(Props(new UnhandledMessagesListener), name = "UnhandledMessageLister")
  system.eventStream.subscribe(unhandledMessageListener, classOf[UnhandledMessage])

  /**
    * Start the [[BeanSimulation]]
    */
    val startTime = System.currentTimeMillis()
  val beanSimulation = system.actorOf(Props(new BeanSimulation()))
  beanSimulation ! StartSimulation()
}

/**
  * A class to start execution of a Bean Simulation.  This object creates a [[SimLogger]] for the model and then
  * creates to [[BeanRoot]] top level coordinator.  It then starts the simulation by sending a [[StartSimulation]]
  * message.  Upon receipt of a [[Terminate]] messsage, it shuts the actor system down.
  */
class BeanSimulation extends LoggingActor {
  val initialTime = Duration.ofSeconds(0)
  val dataLogger = context.actorOf(Props(new FileLogger("beanOutput.csv")))
  val simLogger = context.actorOf(Props(new SimLogger(dataLogger, initialTime)), name = "Logger")
  def receive = {
    case StartSimulation() =>
      val sim = context.actorOf(Props(new BeanRoot(initialTime, PholdParameters.stopTime, 0, 1000000000, DesignPointIteration(2,3), simLogger)), name = "PHoldRoot")
      context.watch(sim)
      sim ! StartSimulation()

    case Terminated( child ) =>
      dataLogger ! new CloseFile()

    case FileClosed() =>
      context.stop(self)
  }


  override def postStop() = {
    // This allows program to exit
    context.system.shutdown()
    val seconds = (System.currentTimeMillis() - BeansSimulation.startTime)/1000.0
    println("Run time is " + seconds)
  }

}
