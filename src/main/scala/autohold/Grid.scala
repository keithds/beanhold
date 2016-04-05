package autohold

/**
 * Class to hold the x,y location of a bean
 * @param x
 * @param y
 */
case class LPLoc(x: Int, y: Int) {
  override def toString: String = "(" + x + ", " + y + ")"
  override def equals(otherLocation: Any): Boolean = {
    otherLocation match {
      case LPLoc(x1, y1) => x == x1 && y == y1
      case _ => false
    }
  }
}


class Grid[T](private val grid: Vector[Vector[T]]) {
  val xSize: Int = grid.length
  val ySize: Int = grid(0).length
  def itemAt(x: Int, y: Int): T = grid(Math.abs(x % xSize))(Math.abs(y % ySize))
  def getNeighbors(location: LPLoc, distance: Int): List[T] = {
    val x = for (i <- location.x - distance to location.x + distance) yield {
      val items = for (j <- location.y - distance to location.y + distance) yield itemAt(i, j)
      items.toIterator
    }
    x.flatten.toList
  }
}

