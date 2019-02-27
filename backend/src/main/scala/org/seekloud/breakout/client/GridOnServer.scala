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

  var newInfo = List.empty[(Byte, Ball)]

  val ballIdGenerator: AtomicInteger = new AtomicInteger(1)

  def addSnake(id: String): Unit = {
    waitingJoin = id :: waitingJoin
  }

  def addBall() = {
    val newBalls = waitingJoin.map {id =>
      val snake = snakes.filter(_._2.id == id).head
      val breakoutId = snake._1
      val ballId = (ballIdGenerator.getAndIncrement() % Byte.MaxValue).toByte

      val newBall = Ball(breakoutId, 2, (30 + scala.util.Random.nextInt(30)) * angleUnit,
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

}
