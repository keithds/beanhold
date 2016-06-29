package autohold

import java.time.Duration

import scala.collection.immutable.ListSet


trait BeansModelImpl { this: BeansSimulator#BeansModel =>
  setDebug(true)

  def getDestination(loc: LPLoc) = PholdParameters.random match {
    case true => randomJumpDestination(loc)
    case false => deterministicJumpDesination(loc)
  }

  def deterministicJumpDesination(loc: LPLoc): LPLoc = {
    loc match {
      case p1 if p1.x == PholdParameters.gridSizeX - 1 && p1.y == PholdParameters.gridSizeY - 1 => loc
      case p2 =>
        LPLoc(Math.min(p2.x + 1, PholdParameters.gridSizeX - 1) , Math.min(p2.y + 1, PholdParameters.gridSizeY - 1))
    }
  }

  def randomJumpDestination(loc: LPLoc): LPLoc = {
    val neighbors = PholdParameters.getNeighbors(properties.location)
    val index = random.nextInt(neighbors.size)
    neighbors(index)
  }

  def timeIncrement(): Duration = PholdParameters.random match {
    case true => randomTimeIncrement
    case false => deterministicTimeIncrement
  }

  def deterministicTimeIncrement = Duration.ofSeconds(1)

  def randomTimeIncrement = Duration.ofSeconds(random.nextInt(PholdParameters.maxTimeIncrement) + 1)

  def delay(millis: Long) = {
    val count: Int = millis.toInt * 1333333
    for (x <- 1 to count) {
      val y = x + 1
    }
  }


  def handleInitialBeanJumpData(initialBeanJumpData: InitialBeanJumpData, t: Duration): Boolean = {
    addOutput(BeanOutData(properties.location, getDestination(properties.location)), t.plus(Duration.ofSeconds(1)))
    true
  }
  def handleBeanOutData(beanOutData: BeanOutData, t: Duration): Boolean = {
    addOutput(beanOutData, t)
    logMessage("Handling BeanOutData.  Removing the following bean " + beanOutData.beanNumber)
    val newBeanList = state.beansState.getLatestState.filterNot(bean => bean == beanOutData.beanNumber)
    state.beansState.setState(newBeanList, t)
    logMessage("Bean inventory " + state.beansState.getLatestState)
    true
  }

  def handleLPLoc(beanIn: LPLoc, t: Duration): Boolean = {
    logMessage("Handling LPLoc. Bean " + beanIn + " jumped in")
//    state.beansState.setState(beanIn :: state.beansState.getLatestState, t)
    state.beansState.setState(state.beansState.getLatestState + beanIn, t)
    val destination = getDestination(properties.location)
    val start = (System.currentTimeMillis() - BeansSimulation.startTime)/1000.0
    PholdParameters.jumpCount ! JumpCount(0)
    println(properties.location + " with thread " + Thread.currentThread().getId + " going to delay at " + start)
    delay(PholdParameters.defaultComputationGrain.millis)
    val back = (System.currentTimeMillis() - BeansSimulation.startTime)/1000.0
    println(properties.location + " with thread " + Thread.currentThread().getId + " is back at " + back)
    val beanOutData = BeanOutData(beanIn, destination)
    val ti = timeIncrement()
    logMessage("Time increment is " + ti.getSeconds)
    addEvent(beanOutData, t.plus(ti))
    true
  }

  override def modelPreTerminate = {
    logMessage("Bean inventory " + state.beansState.getLatestState)
  }

}
