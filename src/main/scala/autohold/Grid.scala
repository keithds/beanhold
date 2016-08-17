package autohold

import beanmessages.Beanhold.LPLoc

class Grid[T](private val grid: Vector[Vector[T]]) {
  val xSize: Int = grid.length
  val ySize: Int = grid(0).length
  def itemAt(x: Int, y: Int): T = grid(Math.abs(x % xSize))(Math.abs(y % ySize))
  def getNeighbors(location: LPLoc, distance: Int): List[T] = {
    val x = for (i <- location.getX - distance to location.getX + distance) yield {
      val items = for (j <- location.getY - distance to location.getY + distance) yield itemAt(i, j)
      items.toIterator
    }
    x.flatten.toList
  }
}

