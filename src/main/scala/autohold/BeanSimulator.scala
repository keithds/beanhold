package autohold

import java.time.Duration;
import devsmodel._
import akka.actor._

import scala.collection.immutable.ListSet

case class Beans(beans: List[LPLoc], i: Int)
case class BeanProperties(computationGrain: ComputationGrain, location: LPLoc, neigborDistance: Int) extends ModelProperties
case class BeanOutData(beanNumber: LPLoc, destination: LPLoc)
case class InitialBeanJumpData()

case class BeansState(beans: DynamicStateVariable[ListSet[autohold.LPLoc]]) extends ModelState
case class BeansStateManager(initialState: BeansState) extends ModelStateManager[BeansState](initialState) {
  val beansState = buildSimEntityState(initialState.beans, "beans");
  override val stateVariables = List(beansState)
}
class BeansSimulator(override val properties: BeanProperties, initialTime: Duration, initialState: BeansState, initialEvents: InitialEvents, randActor: ActorRef, override val simLogger: ActorRef) extends ModelSimulator[BeanProperties, BeansState, BeansStateManager](properties, initialTime, initialState, initialEvents, randActor, simLogger) {
  override val devs = new BeansModel(properties, initialState, initialEvents, initialTime, simLogger);
  class BeansModel(p: BeanProperties, iState: BeansState, initialEvents: InitialEvents, iTime: Duration, simLogger: ActorRef) extends DEVSModel[BeanProperties, RandomProperties, BeansState, BeansStateManager](p, iState, initialEvents, iTime, simLogger) with BeansModelImpl {
    override def initializeRandomProperties = {}
    override def buildStateManager(state: BeansState): BeansStateManager = new BeansStateManager(initialState)
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