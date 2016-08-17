package autohold

import java.time.Duration
import beanmessages.Beanhold.PholdParameters


object BeanParameters {


  def stopTime(p: PholdParameters): Duration = {
    p.getRandom() match {
      case true => Duration.ofSeconds(p.getRandomStopTime())
      case false => Duration.ofSeconds(Math.max(p.getGridSizeX, p.getGridSizeY) - 1)
    }
  }
/*
  val gridData = for (i <- 0 to gridSizeX - 1) yield {
    val locations = for (j <- 0 to gridSizeY - 1) yield {
      LPLoc(i, j)
    }
    locations.toVector
  }

  val grid = new Grid[LPLoc](gridData.toVector)

  def getNeighbors(loc: LPLoc) = {
    grid.getNeighbors(loc, neighborDistance)
  }
*/

}
