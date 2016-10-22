package autohold

import java.time.Duration


import akka.actor.Actor.Receive
import beanmessages.Beanhold._
import scala.collection.JavaConversions._


trait BeansModelImpl { this: BeansSimulator#BeansModel =>
  setDebug(true)
  val pHoldParameters = properties.getParams

  val gridData = for (i <- 0 to pHoldParameters.getGridSizeX - 1) yield {
    val locations = for (j <- 0 to pHoldParameters.getGridSizeY - 1) yield {
      BeansSimulator.buildLPLoc(i, j)
    }
    locations.toVector
  }

  val grid = new Grid[LPLoc](gridData.toVector)

  def getDestination(loc: LPLoc) = pHoldParameters.getRandom match {
    case true => randomJumpDestination(loc)
    case false => deterministicJumpDesination(loc)
  }

  def deterministicJumpDesination(loc: LPLoc): LPLoc = {
    loc match {
      case p1 if p1.getX == pHoldParameters.getGridSizeX - 1 && p1.getY == pHoldParameters.getGridSizeY - 1 => loc
      case p2 =>
        BeansSimulator.buildLPLoc(Math.min(p2.getX + 1, pHoldParameters.getGridSizeX - 1) , Math.min(p2.getY + 1, pHoldParameters.getGridSizeY - 1))
    }
  }

  def randomJumpDestination(loc: LPLoc): LPLoc = {
    val neighbors = grid.getNeighbors(properties.getLocation, pHoldParameters.getNeighborDistance)
    val index = random.nextInt(neighbors.size)
    neighbors(index)
  }

  def timeIncrement(): Duration = pHoldParameters.getRandom match {
    case true => randomTimeIncrement
    case false => deterministicTimeIncrement
  }

  def deterministicTimeIncrement = Duration.ofSeconds(1)

  def randomTimeIncrement = Duration.ofSeconds(random.nextInt(pHoldParameters.getMaxTimeIncrement) + 1)


  def handleInitialBeanJumpData(initialBeanJumpData: InitialBeanJumpData, t: Duration): Boolean = {
    //addOutput(BeansSimulator.buildBeanOutData(properties.getLocation, getDestination(properties.getLocation)), t.plus(Duration.ofSeconds(1)))
    addOutput(BeanOutData(properties.getLocation, getDestination(properties.getLocation)), t.plus(Duration.ofSeconds(1)))
    true
  }
  def handleBeanOutData(beanOutData: BeanOutData, t: Duration): Boolean = {
    addOutput(beanOutData, t)
    logMessage("Handling BeanOutData.  Removing the following bean " + BeansSimulator.locToString(beanOutData.beanNumber))
    val newBeanList = state.beansState.getLatestState.beans.filterNot(bean => BeansSimulator.locEquals(bean, beanOutData.beanNumber))
    val newBeanState = BeansHere(newBeanList, t)//BeansSimulator.buildBeans(newBeanList)
    state.beansState.setState(newBeanState, t)
    logMessage("Bean inventory " + state.beansState.getLatestState)
    true
  }


  var currentBeanIn: LPLoc = properties.getLocation
  var handleLPLocTime = currentTime
  def handleLPLoc(beanIn: LPLoc, t: Duration): Boolean = {
    logMessage("Handling LPLoc. Bean " + beanIn + " jumped in")
//    state.beansState.setState(beanIn :: state.beansState.getLatestState, t)
    currentBeanIn = beanIn
    handleLPLocTime = t
    jumpCalculator.tell(BeansSimulator.buildCalculateJump(pHoldParameters.getJumpCountDelayMillis, random.nextDouble()), sim)
    false
  }

  override def modelPreTerminate = {
    logMessage("Bean inventory  " + state.beansState.getLatestState)
  }

  override def processStateTransitionMessages: Receive = {
    case jv: JumpVal =>
      val newBeanList = currentBeanIn :: (state.beansState.getLatestState.beans).toList
      val newBeanState = BeansHere(newBeanList, currentTime)//BeansSimulator.buildBeans(newBeanList)
      state.beansState.setState(newBeanState, handleLPLocTime)
      val destination = getDestination(properties.getLocation)
      jumpCount ! BeansSimulator.buildJumpCount(0)
      //val beanOutData = BeansSimulator.buildBeanOutData(currentBeanIn, destination)
      val beanOutData = BeanOutData(currentBeanIn, destination)
      val ti = timeIncrement()
      logMessage("Time increment is " + ti.getSeconds)
      addEvent(beanOutData, handleLPLocTime.plus(ti))
      externalTransitionDone(handleLPLocTime)
  }

}
