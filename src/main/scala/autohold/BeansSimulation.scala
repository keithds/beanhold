package autohold

import java.time.Duration
import beanmessages.Beanhold.PholdParameters
import com.typesafe.config.{ConfigException, ConfigFactory}
import dmfmessages.DMFSimMessages._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.Exception._
import devsmodel._
import akka.actor._
import simutils._
import MessageRouter._
import scala.collection.JavaConversions._

object BeansSimulation extends App {

  val configuration = ConfigFactory.load()


  def configValue[T](value: String, f: String => T, default: T): T = {
    (catching(classOf[ConfigException])
      opt { f(value)}).getOrElse(default)
  }

  /* The size of the bean jump grid x direction */
  val gridSizeX = configValue("beanhold.gridSizeX", configuration.getInt, 4)
  /* The size of the bean jump grid x direction */

  /* The size of the bean jump grid y direction */
  val gridSizeY = configValue("beanhold.gridSizeY", configuration.getInt, 4)

  /* When a bean jumps to a neighbor, this is the number of cells away it considers to be neighbors */
  val neighborDistance = configValue("beanhold.neighborDistance", configuration.getInt, 1)

  /**
    *  To simulate computation delays, this is the max amount of time required to calculate the next jump location.
    *  The actual time required is a random number of milliseconds between 0 and jumpComputationDelayMillis
    */
  val jumpComputationDelayMillis = configValue("beanhold.jumpComputationDelayMillis", configuration.getInt, 1)

  /**
    * In random mode, the next jump for a bean occurs between 1 and maxTimeIncrement seconds
    */
  val maxTimeIncrement = configValue("beanhold.maxTimeIncrement", configuration.getInt, 1)

  /**
   * In random mode, the number of seconds to run the simulation
   */
  val randomStopTime: Duration = Duration.ofSeconds(configValue("beanhold.randomStopTime", configuration.getInt, 10))

  /**
    * In deterministic mode, all beans will jump at 1 second intervals one cell up and to the right.  The simulation
    * terminate when all beans have reached the upper right cell.  This is good for testing.
    *
    * In random mode, beans will jump to a random neighbor within [[neighborDistance]] at random inrements
    * between 1 and [[maxTimeIncrement]], and the simulation will terminate at [[randomStopTime]]
    */
  val random = configValue("beanhold.random", configuration.getBoolean, false)

  /**
    * The flag tests distributed actor systems.  In distributed mode, a series of distributed workers
    * will be available on a MessageRouter to create [[BeansSimulator]] actors
    */
  val distributed = configValue("beanhold.distributed", configuration.getBoolean, false)

  /**
    * The number of distributed actor workers to await before starting simulation
    */
  val distributedWorkers = configValue("beanhold.distributedWorkers", configuration.getInt, 1)

  /**
    * This flag tests a distributed calculation service used by all beans in the simulation.  Each bean calls this
    * service before jumping to the next cell.  The service of on will be one of the [[jumpWorkers]]
    */
  val jumpService = configValue("beanhold.jumpService", configuration.getBoolean, false)


  /**
    * The number of jump service workers to await before starting simulation
    */
  val jumpWorkers = configValue("beanhold.jumpWorkers", configuration.getInt, 1)

  /**
    * If this flag is true, the actor system will run as remote.  A remote system will start the ActorSystem and then
    * create a CreateActor actor to await remote creation of BeanSimulator actors
    */
  val remote = configValue("beanhold.remote", configuration.getBoolean, false)

  /**
    * A remote system will contact the simulation at this IP
    */
  val beanIP = configValue("beanhold.beanIP", configuration.getString, "127.0.0.1")


  /**
    * A remote system will contact the simulation at this port
    */
  val beanPort = configValue("beanhold.beanPort", configuration.getInt, 2552)

  val pHoldParameters: PholdParameters = PholdParameters.newBuilder()
    .setGridSizeX(gridSizeX)
    .setGridSizeY(gridSizeY)
    .setNeighborDistance(neighborDistance)
    .setJumpCountDelayMillis(jumpComputationDelayMillis)
    .setMaxTimeIncrement(maxTimeIncrement)
    .setRandomStopTime(randomStopTime.getSeconds.toInt)
    .setRandom(random)
    .setDistributed(distributed)
    .setDistributedWorkers(distributedWorkers)
    .setJumpService(jumpService)
    .setJumpWorkers(jumpWorkers).build
  /*val pHoldParameters = PholdParameters(
    gridSizeX,
    gridSizeY,
    neighborDistance,
    jumpComputationDelayMillis,
    maxTimeIncrement,
    randomStopTime,
    random,
    distributed,
    distributedWorkers,
    jumpService,
    jumpWorkers
  )*/

  /**
    * Start the ActorSystem
    */
  val system = ActorSystem("BeanSystem", configuration)

  var ready = true
  val jumpCalculator: Option[ActorRef] = jumpService match {
    case true =>
      ready = false
      class WorkersReadyActor extends Actor {
        def receive = {
          case wr: WorkersReady => ready = true
        }
      }
      val workersReadyActor = system.actorOf(Props[WorkersReadyActor])
      val messageRouter = system.actorOf(MessageRouter.props(workersReadyActor, jumpWorkers), name = "messagerouter")
      Some(messageRouter)
    case false =>
      None
  }

  /**
    * Create an [[UnhandledMessagesListener]] that logs unhandled messages and stops the simulation.  This behavior
    * can be overridden, but unhandled messages are likely an error in the simulation
    */
  //val unhandledMessageListener = system.actorOf(Props(new UnhandledMessagesListener), name = "UnhandledMessageLister")
  //system.eventStream.subscribe(unhandledMessageListener, classOf[UnhandledMessage])

  remote match {
    case false =>
      distributed match {
        case true =>
          val simStarter = system.actorOf (SimStarter.props)
          val remoteWorkers = system.actorOf (RemoteWorkers.props(simStarter, distributedWorkers), name = "remoteworkers")
        case false =>
          val beanSimulation = system.actorOf (BeanSimulation.props (None) )
          // wait for jump workers to be ready
          while (!ready) Thread.sleep(100)
          beanSimulation ! ModelSimulator.buildStartSimulation
      }
    case true =>
      val actorPath:String = "akka.tcp://BeanSystem@" + beanIP + ":"  + beanPort  + "/user/remoteworkers"
      val actorSelection = system.actorSelection(actorPath)
      val remoteWorkersFuture = actorSelection.resolveOne(10 seconds)
      val remoteWorkers = Await.result(remoteWorkersFuture, 10 seconds)
      val address = Address("akka.tcp", "BeanSystem", configuration.getString("akka.remote.netty.tcp.hostname"), configuration.getInt("akka.remote.netty.tcp.port"))
      remoteWorkers ! RemoteWorkers.buildNewRemoteWorker(address)
  }
}

object SimStarter {

  def props = Props(new SimStarter())
}
class SimStarter extends Actor {

  def receive = {

    case rwr: RemoteWorkersReady =>
      val addresses = rwr.getAddressesList.map (a => RemoteWorkers.translateAkkaAddres(a)).toList
      val beanSimulation = context.actorOf(BeanSimulation.props(Some(addresses)))
      // wait until jump workes are ready
      while (!BeansSimulation.ready) Thread.sleep(100)
      beanSimulation ! ModelSimulator.buildStartSimulation
  }
}

class BeanSimLogger(dataLogger: ActorRef, initialTime: Duration, designPoint: DesignPointIteration = SimLogger.buildDesignPointIteration(1,1)) extends SimLogger(dataLogger, initialTime, designPoint) with BeanMessageConverter

/**
  * Companion object for [[BeanSimulation]]
  */
object BeanSimulation {
  def props(remoteWorkersList: Option[List[Address]]) = Props(new BeanSimulation(remoteWorkersList))
}
/**
  * A class to start execution of a Bean Simulation.  This object creates a [[SimLogger]] for the model and then
  * creates to [[BeanRoot]] top level coordinator.  It then starts the simulation by sending a [[StartSimulation]]
  * message.  Upon receipt of a [[Terminate]] messsage, it shuts the actor system down.
  */
class BeanSimulation(val remoteWorkersListOption: Option[List[Address]]) extends LoggingActor {
  val initialTime = Duration.ofSeconds(0)
  val dataLogger = context.actorOf(Props(new FileLogger("beanOutput.csv")))
  val simLogger = context.actorOf(Props(new BeanSimLogger(dataLogger, initialTime)), name = "Logger")
  val startTime = System.currentTimeMillis()

  def receive = {
    case s: StartSimulation =>
      val jumpCalculator = BeansSimulation.jumpCalculator.getOrElse(context.actorOf(JumpCalculator.props))
      val sim = context.actorOf(Props(new BeanRoot(initialTime, 0, 1000000000, remoteWorkersListOption, jumpCalculator,
        SimLogger.buildDesignPointIteration(2, 3), simLogger, BeansSimulation.pHoldParameters, BeanParameters.stopTime(BeansSimulation.pHoldParameters))), name = "PHoldRoot")
      context.watch(sim)
      sim ! s

    case Terminated( child ) =>
      dataLogger ! FileLogger.buildCloseFile

    case fc: FileClosed =>
      context.stop(self)
      Thread.sleep(60 * 60 * 1000)
  }


  override def postStop() = {
    // This allows program to exit
    context.system.terminate()
    val seconds = (System.currentTimeMillis() - startTime)/1000.0
    println("Run time is " + seconds)
  }

}
