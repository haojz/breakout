package org.seekloud.breakout.client

import java.util.concurrent.atomic.AtomicInteger

import net.sf.ehcache.config.SearchAttribute
import org.seekloud.breakout.Grid
import org.slf4j.LoggerFactory
import org.seekloud.breakout._
import org.seekloud.breakout.Boot.roomManager
import org.seekloud.breakout.core.RoomActor

import scala.util.Random

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 9:55 PM
  */
class GridOnServer(override val boundary: Point) extends Grid {


  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)

  private[this] var waitingJoin = List.empty[String]

  var currentRank = List.empty[Score]

  var newInfo = List.empty[(Byte, Ball)]

  val ballIdGenerator: AtomicInteger = new AtomicInteger(1)

  def addSnake(id: String): Unit = {
    waitingJoin = id :: waitingJoin
  }

  def addBall() = {
    val newBalls = waitingJoin.map {id =>
      println(s"snakes===============$snakes")
      val snake = snakes.filter(_._2.id == id).head
      val breakoutId = snake._1
      val ballId = (ballIdGenerator.getAndIncrement() % Byte.MaxValue).toByte

      val newBall = Ball(breakoutId, 2, (30 + scala.util.Random.nextInt(30)) * angleUnit,
//      val newBall = Ball(breakoutId, 10, - scala.util.Random.nextInt(90),
//        Point(snake._2.paddleLeft, paddleY))
        Point(snake._2.paddleLeft + paddleWidth / 2 - ballRadius, paddleY - 2 * ballRadius))
      balls += (ballId -> newBall)
      ballId -> newBall
    }
    waitingJoin = Nil
    newBalls

  }

  def waitingListState: Boolean = waitingJoin.nonEmpty

  def initBricks() = {
    (0 to 1).foreach { seat =>
      initBrick(seat)
    }

  }

  def initBrick(seat: Int) = {
    val initBrick = (0 until numEveryRow * rowNum).map{i =>
      (i, 0)
    }.toMap
    val sampleGreen = sample((0 until numEveryRow * rowNum).toList, 1).head
    val sampleGreenMap = Map(sampleGreen -> 2)

    val sampleMap = sample((0 until numEveryRow * rowNum).toList.filter(_ != sampleGreen), 3).map((_, 1)).toMap

    val initBrick1 = (initBrick ++ sampleMap ++ sampleGreenMap).map(b => ((b._1 + seat * numEveryRow * rowNum).toByte, (seat.toByte, b._2.toByte)))
    bricks ++= initBrick1
    initBrick1
  }

  def updateInService(roomId: Int) = {
    val deadSnakes = super.update()
    if (waitingJoin.nonEmpty) newInfo = addBall()
    deadSnakes
//    if (deadSnakes.nonEmpty) {
//      val deadSnakesInfo = deadSnakes.map { i d =>
//        if (currentRank.exists(_.id == id)) {
//          val info = currentRank.filter(_.id == id).head
//          (id, info.k, info.area)
//        } else (id, -1.toShort, -1.toShort)
//      }
//      roomManager ! RoomActor.UserDead(roomId, deadSnakes.map(_.id))
//    }
//    updateRanks()
  }


  def sample(list: List[Int], num: Int) = {
    var temp = list
    (0 until num).map {i =>
      val index = (new Random).nextInt(temp.length - 1)
      val num = list(index)
      temp = list.take(index) ++ list.drop(index + 1)
      num
    }.toList
  }


  def generateBreakoutId(carnieIdGenerator: AtomicInteger, existsId: Iterable[Byte]): Byte = {
    var newId = (carnieIdGenerator.getAndIncrement() % Byte.MaxValue).toByte
    while (existsId.exists(_ == newId)) {
      newId = (carnieIdGenerator.getAndIncrement() % Byte.MaxValue).toByte
    }
    newId
  }

//  private[this] def genWaitingSnake() = {
//    val newInfo = waitingJoin.filterNot(kv => snakes.contains(kv._1)).map { case (id, (name, bodyColor, img, carnieId)) =>
//      val indexSize = 5
//      val basePoint = randomEmptyPoint(indexSize)
//      val newFiled = (0 until indexSize).flatMap { x =>
//        (0 until indexSize).map { y =>
//          val point = Point(basePoint.x + x, basePoint.y + y)
//          grid += Point(basePoint.x + x, basePoint.y + y) -> Field(id)
//          point
//        }.toList
//      }.toList
//      val startPoint = Point(basePoint.x + indexSize / 2, basePoint.y + indexSize / 2)
//      val snakeInfo = SkDt(id, name, bodyColor, startPoint, startPoint, img = img, carnieId = carnieId) //img: Int
//      snakes += id -> snakeInfo
//      killHistory -= id
//      (id, snakeInfo, newFiled)
//    }.toList
//    waitingJoin = Map.empty[String, (String, String, Int, Byte)]
//    newInfo
//  }

//  private[this] def updateRanks(): Unit = {
//    val areaMap = grid.filter { case (p, spot) =>
//      spot match {
//        case Field(id) if snakes.contains(id) => true
//        case _ => false
//      }
//    }.map {
//      case (p, f@Field(_)) => (p, f)
//      case _ => (Point(-1, -1), Field((-1L).toString))
//    }.filter(_._2.id != -1L).values.groupBy(_.id).map(p => (p._1, p._2.size))
//    currentRank = snakes.values.map(s => Score(s.id, s.name, s.kill, areaMap.getOrElse(s.id, 0).toShort)).toList.sortBy(_.area).reverse
//
//  }

//  def randomColor(): String = {
//    var color = randomHex()
//    val exceptColor = snakes.map(_._2.color).toList ::: List("#F5F5F5", "#000000", "#000080", "#696969") ::: waitingJoin.map(_._2._2).toList
//    val similarityDegree = 2000
//    while (exceptColor.map(c => colorSimilarity(c.split("#").last, color)).count(_ < similarityDegree) > 0) {
//      color = randomHex()
//    }
//    //    log.debug(s"color : $color exceptColor : $exceptColor")
//    "#" + color
//  }

//  def randomHex(): String = {
//    val h = getRandom(94).toHexString + getRandom(94).toHexString + getRandom(94).toHexString
//    String.format("%6s", h).replaceAll("\\s", "0").toUpperCase()
//  }

//  def getRandom(start: Int): Int = {
//    val end = 226
//    val rnd = new scala.util.Random
//    start + rnd.nextInt(end - start)
//  }

//  def colorSimilarity(color1: String, color2: String): Int = {
//    var target = 0.0
//    var index = 0
//    if (color1.length == 6 && color2.length == 6) {
//      (0 until color1.length / 2).foreach { _ =>
//        target = target +
//          Math.pow(hexToDec(color1.substring(index, index + 2)) - hexToDec(color2.substring(index, index + 2)), 2)
//        index = index + 2
//      }
//    }
//    target.toInt
//  }

//  def hexToDec(hex: String): Int = {
//    val hexString: String = "0123456789ABCDEF"
//    var target = 0
//    var base = Math.pow(16, hex.length - 1).toInt
//    for (i <- 0 until hex.length) {
//      target = target + hexString.indexOf(hex(i)) * base
//      base = base / 16
//    }
//    target
//  }




}
