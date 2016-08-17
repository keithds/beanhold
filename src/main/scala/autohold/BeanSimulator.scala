package autohold

import java.time.Duration;
import beanmessages.Beanhold._
import com.google.protobuf.{GeneratedMessage, Any}
import devsmodel._
import akka.actor._
import dmfmessages.DMFSimMessages._
import scala.collection.JavaConversions._

import scala.collection.immutable.ListSet


case class BeansStateManager(initialState: Beans) extends ModelStateManager[Beans](initialState) {
  val initialBeanState = DynamicStateVariable(Duration.parse(initialState.getTimeInState), initialState)
  val beansState = buildSimEntityState(initialBeanState, "beans");
  override val stateVariables = List(beansState)
}

trait BeanMessageConverter extends MessageConverter {
  override val outputList = List(BeanOutData.getDefaultInstance)
  override val eventList = List(BeanOutData.getDefaultInstance, InitialBeanJumpData.getDefaultInstance, LPLoc.getDefaultInstance)
  override val stateList = List(Beans.getDefaultInstance)
}

object BeansSimulator {
  def locToString(loc: LPLoc): String = "(" + loc.getX + ", " + loc.getY + ")"
  def locEquals(l1: LPLoc, l2: LPLoc): Boolean = {
    if (l1.getX == l2.getX && l1.getY == l2.getY) true else false
  }
  def buildLPLoc(x: Int, y: Int): LPLoc = LPLoc.newBuilder().setX(x).setY(y).build
  def buildBeanOutData(beanNumber: LPLoc, destination: LPLoc): BeanOutData = BeanOutData.newBuilder().setBeanNumber(beanNumber).setDestination(destination).build
  def buildJumpVal(jumpInMillis: Int): JumpVal = JumpVal.newBuilder().setJumpInMillis(jumpInMillis).build
  def buildCalculateJump(maxJumpMillis: Int, randomDouble: Double): CalculateJump = CalculateJump.newBuilder().setMaxJumpMillis(maxJumpMillis).setRandomDouble(randomDouble).build
  def buildBeans(beans: Seq[LPLoc]): Beans = Beans.newBuilder().addAllBeans(beans).build
  def buildBeans(t: Duration, beans: List[LPLoc]): Beans = Beans.newBuilder().setTimeInState(t.toString).addAllBeans(beans).build
  def buildJumpCount(jumpCount: Int): JumpCount = JumpCount.newBuilder().setJumpCount(jumpCount).build()
  def buildInitialBeanJumpData: InitialBeanJumpData = InitialBeanJumpData.newBuilder().build
  def buildBeanProperties(params: PholdParameters, location: LPLoc): BeanProperties = BeanProperties.newBuilder()
    .setParams(params).setLocation(location).build
}
class BeansSimulator(override val properties: BeanProperties, initialTime: Duration, initialState: Beans, initialEvents: InitialEvents, randActor: ActorRef, override val simLogger: ActorRef, jumpCount: ActorRef, jumpCalculator: ActorRef)
  extends ModelSimulator[BeanProperties, Beans, BeansStateManager](properties, initialTime, initialState, initialEvents, randActor, simLogger) with BeanMessageConverter {
  override val devs = new BeansModel(properties, initialState, initialEvents, initialTime, simLogger, jumpCount, jumpCalculator);
  class BeansModel(p: BeanProperties, iState: Beans, initialEvents: InitialEvents, iTime: Duration, simLogger: ActorRef, val jumpCount: ActorRef, val jumpCalculator: ActorRef) extends DEVSModel[BeanProperties, EmptyRandomProperties, Beans, BeansStateManager](p, iState, initialEvents, iTime, simLogger) with BeansModelImpl {
    override def initializeRandomProperties = {}
    override def buildStateManager(state: Beans): BeansStateManager = new BeansStateManager(initialState)
    override def handleInternalStateTransitionData[T](d: T, t: Duration) = d match {
      case (beanOutData: BeanOutData) =>
        if(handleBeanOutData(beanOutData, t)) internalTransitionDone(t)
      case (initialBeanJumpData: InitialBeanJumpData) =>
        if(handleInitialBeanJumpData(initialBeanJumpData, t)) internalTransitionDone(t)
      case (a : Any) =>
        throw new UnhandledEventException("Received unhandled internal event: ".+(a))
    };
    override def handleExternalStateTransitionData[T](d: T, t: Duration) = d match {
        case (lPLoc: LPLoc) =>
          if(handleLPLoc(lPLoc, t)) externalTransitionDone(t)
        case (a: Any) => throw new UnhandledEventException("Received unhandled external event: ".+(a))

    }
  }
}