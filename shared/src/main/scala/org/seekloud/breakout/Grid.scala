package org.seekloud.breakout

import java.awt.event.KeyEvent

import org.seekloud.breakout.Protocol.TotalData

import scala.util.Random
import scala.math._
import org.seekloud.breakout.Constant._


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())
//  val angleUnit = math.Pi / 180
  val angleUnit = 0.01745
  var historyStateMap = Map.empty[Int, (Map[Byte, (Byte, Byte)], Map[Byte, Ball], Map[Byte, SkDt], Map[Byte, Int])]
  var frameCount = 0l
  val maxDelayed = 11 //最大接收10帧以内的延时
  var snakes = Map.empty[Byte, SkDt]
  var balls = Map.empty[Byte,  Ball] //单数为左方，双数为右方
  var bricks = Map.empty[Byte, (Byte, Byte)]//id，(seat，属性)
  var actionMap = Map.empty[Long, Map[String, Byte]]//帧号，（id，action:0up 1left 2right）
  val paddleWidth = 8
  val paddleHeight = 2
  var lastAction = Map.empty[Byte, Int]
  val paddleY = Boundary.h - 10
  val moveSpeed = 4
  val startX = 5
  val startY = 4
  var greenBallId = 10


  def addAction(id: String, keyCode: Byte) = {
    addActionWithFrame(id, keyCode, frameCount)
  }

  def addActionWithFrame(id: String, keyCode: Byte, frame: Long) = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (frame -> tmp)
  }

  def setGridInGivenFrame(frame: Int): Unit = {
    frameCount = frame
    val state = historyStateMap(frame)
    bricks = state._1
    balls = state._2
    snakes = state._3
    lastAction = state._4
  }


  def update() = {
    val deadSnakes = updateSnakes()
    frameCount += 1
    deadSnakes
  }



   def updateSnakes() = {
    def updateASnake(snake: SkDt, actMap: Map[String, Byte]): Either[SkDt, SkDt] = {
      var score = snake.score
      var dead = false
      val (leftBoundary, rightBoundary) = snake.color match {
        case 0 => (Boundary.start1, Boundary.end1)
        case 1 => (Boundary.start2, Boundary.end2)
      }

      val pWidth = snake.characterLife match {
        case 0 => paddleWidth
        case _ => (1.5 * paddleWidth).toInt
      }

      var isCrashYellow = false
      //碰撞检测
      balls.filter(_._2.bId == snake.bId).foreach { ball =>
        val theta = ball._2.theta
        val speed = ball._2.speed
        val point = ball._2.point
        val speedX = (speed * cos(theta)).formatted("%.4f")
        val speedY = (-speed * sin(theta)).formatted("%.4f")

        val newPoint = (point + Point(speedX.toFloat, speedY.toFloat)).format
        val isDead = if (newPoint.y > Boundary.h) true else false

        //判断是否撞墙
        if (!isDead) {
          newPoint match {
            case Point(x, y) if (x <= leftBoundary || x >= rightBoundary - 2 * ballRadius) && y <= 0 =>
              balls += ((ball._1, ball._2.copy(theta = theta - 180 * angleUnit, point = Point(min(max(leftBoundary, x), rightBoundary - 2 * ballRadius), 0))))
            case Point(x, y) if x <= leftBoundary || x >= rightBoundary - 2 * ballRadius =>
              balls += ((ball._1, ball._2.copy(theta = 180 * angleUnit - theta, point = Point(min(max(leftBoundary, x), rightBoundary - 2 * ballRadius), y))))
            case Point(x, y) if y <= 0 =>
              balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, 0))))

            case Point(x, y) if y < paddleY + 1 && y > paddleY - 1  && x <= snake.paddleLeft + pWidth +1 && x >= snake.paddleLeft -1=>
              balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, paddleY - 2 * ballRadius))))
            case Point(x, y) =>
              val width = 4
              val height = 2
              var isCrash = false
              bricks.filter(_._2._1 == snake.color).toList.sortBy(_._1).foreach {b =>
                val offY = (b._1 - snake.color * rowNum * numEveryRow) / numEveryRow
                val offX = (b._1 - snake.color * rowNum * numEveryRow) % numEveryRow
                val bX = startX + offX * width + snake.color * Boundary.start2
                val bY = (startY + offY * height + snake.off * snake.level) % paddleY

//                if (y < bY + height && y > bY - ballRadius * 2 && x > bX - ballRadius * 2 && x < bX + width) {
                if (y <= bY + height && y >= bY - ballRadius * 2 && x >= bX - ballRadius * 2 && x <= bX + width) {
                  bricks -= b._1
//                  println(s"frame: $frameCount, 打掉砖块${b._1}")
                  score += 10
                  if (b._2._2 == 1.toByte) isCrashYellow = true
                  if (b._2._2 == 2.toByte) {
                    balls += ((greenBallId.toByte, ball._2.copy(theta = theta - 180 * angleUnit, point = Point(x, y))))
                    greenBallId += 1
                  }

//                    snakes += snake.bId -> snake.copy(characterLife = 200)
                  isCrash = true
                  if ((point.y > bY + height && y <= bY + height) || (point.y < bY - ballRadius * 2 && y >= bY - ballRadius * 2)) {
                    balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, y))))
                  } else if ((point.x < bX - ballRadius * 2 && x >= bX - ballRadius * 2) || point.x > bX + width && x <= bX + width) {
                    balls += ((ball._1, ball._2.copy(theta = 180 * angleUnit - theta, point = Point(x, y))))
                  } else balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, y))))
                }
              }
              if (!isCrash)  balls += ((ball._1, ball._2.copy(point = Point(x, y))))
          }
        } else {
          dead = true
          balls -= ball._1
        }
      }

      val characterLife = snake.characterLife match {
        case life if isCrashYellow => life + 200
        case life if life > 0 => life - 1
        case _ => 0
      }
      val paddleLeft = if (snake.paddleLeft + pWidth > rightBoundary) rightBoundary - pWidth else snake.paddleLeft
      if (dead) Left(snake.copy(characterLife = characterLife, score = score, paddleLeft = paddleLeft)) else {
        val keyCode = actMap.get(snake.id)
//        println(s"frame: $frameCount, keyCode: $keyCode")
        val key = keyCode match {
          case Some(1) => lastAction += (snake.bId -> 1) ; 1
          case Some(2) => lastAction += (snake.bId -> 2) ; 2
          case Some(0) => lastAction += (snake.bId -> 0) ; 0
          case None if lastAction.get(snake.bId).nonEmpty && lastAction(snake.bId) == 1 => 1
          case None if lastAction.get(snake.bId).nonEmpty && lastAction(snake.bId) == 2 => 2
//          case None if lastAction == -1 => 0
          case _ => 0
        }
//        println(s"backend: $frameCount, action:$key ")

        key match {
          case 1 => //向左
            if (snake.paddleLeft - moveSpeed >= leftBoundary) {
              Right(snake.copy(paddleLeft = snake.paddleLeft - moveSpeed, characterLife = characterLife, score = score))
            } else Right(snake.copy(paddleLeft = leftBoundary, score = score, characterLife = characterLife))

          case 2 => //向右
            if (snake.paddleLeft + pWidth + moveSpeed <= rightBoundary) {
              Right(snake.copy(paddleLeft = snake.paddleLeft + moveSpeed, characterLife = characterLife, score = score))
            }
            else Right(snake.copy(paddleLeft = rightBoundary - pWidth, characterLife = characterLife, score = score))

          case 0 => //不动
            Right(snake.copy(characterLife = characterLife, score = score, paddleLeft = paddleLeft))
        }
      }
      }

    var updatedSnakes = List.empty[SkDt]
    var deadSnakes = List.empty[SkDt]

    val acts = actionMap.getOrElse(frameCount, Map.empty[String, Byte])

    snakes.values.map(updateASnake(_, acts)).foreach {
      case Right(s) =>
        if (s.characterLife == 200 && (s.paddleLeft + (1.5 * paddleWidth).toInt) > s.color * 80 + 50) {
          updatedSnakes ::= s.copy(paddleLeft = s.color * 80 + 50 - (1.5 * paddleWidth).toInt)
        } else updatedSnakes ::= s
      case Left(s) => deadSnakes ::= s
//      case Left(s) => deadSnakes ::= s.copy(life = (s.life -1).toByte)
//        mapKillCounter += killerId -> (mapKillCounter.getOrElse(killerId, 0) + 1)
    }

//    snakes = (updatedSnakes ++ deadSnakes.filterNot(_.life == 0)).map(s => (s.bId, s)).toMap
    snakes = (updatedSnakes ++ deadSnakes).map{s =>
      val off = if (frameCount % 10 == 0) s.off +1 else s.off
      (s.bId, s.copy(off = off))}.toMap
    deadSnakes
//      .filter(s => s.life == 0 && !balls.exists(_._2.bId == s.bId))

  }


  def updateAndGetGridData() = {
    update()
    getGridData
  }

  def resetTotalData(data: TotalData) = {
    snakes = data.snakes.toMap
    bricks = data.bricks.toMap
    balls = data.balls.toMap
    frameCount = data.frame

  }

  def getGridData = {
    Protocol.TotalData(
      frameCount.toInt,
      snakes.toList,
      balls.toList,
      bricks.toList
    )
  }

  def getGridData4Draw = {
    Protocol.GridDataSync(
      frameCount,
      snakes.values.toList,
      bricks,
      balls.values.toList
    )
  }

  def getBall4Draw = {
    Protocol.GridBallData(
      snakes.values.toList,
      balls.values.toList
    )
  }


}
