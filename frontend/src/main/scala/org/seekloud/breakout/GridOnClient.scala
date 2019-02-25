package org.seekloud.breakout

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLAudioElement

import scala.math.{cos, max, min, sin}
import org.seekloud.breakout.client.{DrawRank, NetGameHolder}
//import org.seekloud.breakout.{Grid, Point}
import org.seekloud.breakout.client.DrawRank._

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 10:13 PM
  */
class GridOnClient(override val boundary: Point) extends Grid {

  private[this] val crashBrick = dom.document.getElementById("crash_brick").asInstanceOf[HTMLAudioElement]
  private[this] val crashYellow = dom.document.getElementById("crash_yellow").asInstanceOf[HTMLAudioElement]
  private[this] val crashGreen = dom.document.getElementById("crash_green").asInstanceOf[HTMLAudioElement]
  private[this] val crashPaddle = dom.document.getElementById("crash_paddle").asInstanceOf[HTMLAudioElement]

  var myId = 0.toByte
  var deadList = List[Byte]()


  override def debug(msg: String): Unit = println(msg)

  override def info(msg: String): Unit = println(msg)

//  override def feedApple(appleCount: Int): Unit = {} //do nothing.

  def resetGrid() = {
    historyStateMap = Map.empty[Int, (Map[Byte, (Byte, Byte)], Map[Byte, Ball], Map[Byte, SkDt], Map[Byte, Int])]
    frameCount = 0l
    snakes = Map.empty[Byte, SkDt]
    balls = Map.empty[Byte,  Ball] //单数为左方，双数为右方
    bricks = Map.empty[Byte, (Byte, Byte)]//id，(seat，属性)
    actionMap = Map.empty[Long, Map[String, Byte]]//帧号，（id，action:0up 1left 2right）
    lastAction = Map.empty[Byte, Int]
    deadList = List[Byte]()
  }


  override def updateSnakes() = {
    var drawPaddleMap = Map.empty[Byte, Boolean]

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
//        println(s"frame: $frameCount, speedX: $speedX, speedY: $speedY")


        val newPoint = (point + Point(speedX.toFloat, speedY.toFloat)).format
//        println(s"front: frame: $frameCount, new point:$newPoint")
        val isDead = if (newPoint.y > Boundary.h) true else false

        //判断是否撞墙
        if (!isDead) {
          newPoint match {
            case Point(x, y) if (x <= leftBoundary || x >= rightBoundary - 2 * ballRadius) && y <= 0 =>
              balls += ((ball._1, ball._2.copy(theta = theta - 180 * angleUnit, point = Point(min(max(leftBoundary, x), rightBoundary - 2 * ballRadius), 0))))
            case Point(x, y) if x <= leftBoundary || x >= rightBoundary - 2 * ballRadius=>
              //              println(s"bounds.w::::${Boundary.w}, ball.x::::$x, theta:::$theta, newY:${newPoint.y}")
              balls += ((ball._1, ball._2.copy(theta = 180 * angleUnit - theta, point = Point(min(max(leftBoundary, x), rightBoundary - 2 * ballRadius), y))))
            case Point(x, y) if y <= 0 =>
              balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, 0))))

            case Point(x, y) if y < paddleY + 1 && y > paddleY - 1  && x <= snake.paddleLeft + pWidth +1 && x >= snake.paddleLeft -1=>
              //              println(s"theta::::$theta")
              if (myId == snake.bId) crashPaddle.play()
//              crashBrick.play()
              balls += ((ball._1, ball._2.copy(theta = - theta, point = Point(x, paddleY - 2 * ballRadius))))
            case Point(x, y) =>
              val width = 4
              val height = 2
              var isCrash = false
              var isCrashGreen = false
              bricks.filter(_._2._1 == snake.color).toList.sortBy(_._1).foreach {b =>
                val offY = (b._1 - snake.color * rowNum * numEveryRow) / numEveryRow
                val offX = (b._1 - snake.color * rowNum * numEveryRow) % numEveryRow
                val bX = startX + offX * width + snake.color * Boundary.start2
                val bY = (startY + offY * height + snake.off * snake.level) % paddleY

                //                if (y < bY + height && y > bY - ballRadius * 2 && x > bX - ballRadius * 2 && x < bX + width) {
                if (y <= bY + height && y >= bY - ballRadius * 2 && x >= bX - ballRadius * 2 && x <= bX + width) {
                  bricks -= b._1
//                  println(s"frame: $frameCount, 打掉砖块${b._1}")
                  val offY4Draw = snake.level * snake.off
                  if (snake.level == 0) removeBrick(snake.color, b._1, offY4Draw)
                  score += 10
                  if (b._2._2 == 1.toByte) isCrashYellow = true; drawPaddleMap += snake.bId -> true
                  if (b._2._2 == 2.toByte) {
                    balls += ((greenBallId.toByte, ball._2.copy(theta = theta - 180 * angleUnit, point = Point(x, y))))
                    greenBallId += 1
                    isCrashGreen = true
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
              if (!isCrash)  balls += ((ball._1, ball._2.copy(point = Point(x, y))))  else {
                if (frameCount % 10 != 0 && !deadList.contains(snake.bId)) DrawRank.drawBricks(snake.color, snakes.values.toList, bricks.filter(_._2._1 == snake.color).toList)
                if (myId == snake.bId)
                  if (isCrashGreen) crashGreen.play()
                  else if (isCrashYellow) crashYellow.play()
                  else crashBrick.play()
              }
          }
        } else {
          dead = true
          balls -= ball._1
        }
      }

      if (score > snake.score) drawScore(snake.color, score)

      val characterLife = snake.characterLife match {
        case life if isCrashYellow => life + 200
        case life if life > 0 =>
          if (life == 1) drawPaddleMap += snake.bId -> true
          life - 1
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
//        println(s"front: $frameCount, action:$key ")
        key match {
          case 1 => //向左
            if (snake.paddleLeft - moveSpeed >= leftBoundary) {
              drawPaddleMap += snake.bId -> true
//              drawPaddle(snake.color, snake.paddleLeft - moveSpeed, characterLife)
              Right(snake.copy(paddleLeft = snake.paddleLeft - moveSpeed, characterLife = characterLife, score = score))
            } else {
              if (snake.paddleLeft > leftBoundary || isCrashYellow)
                drawPaddleMap += snake.bId -> true
//                drawPaddle(snake.color, leftBoundary, characterLife)
              Right(snake.copy(paddleLeft = leftBoundary, score = score, characterLife = characterLife))
            }


          case 2 => //向右
            if (snake.paddleLeft + pWidth + moveSpeed <= rightBoundary) {
              drawPaddleMap += snake.bId -> true
//              drawPaddle(snake.color, snake.paddleLeft + moveSpeed, characterLife)
              Right(snake.copy(paddleLeft = snake.paddleLeft + moveSpeed, characterLife = characterLife, score = score))
            }
            else {
              if (snake.paddleLeft + pWidth < rightBoundary || isCrashYellow)
                drawPaddleMap += snake.bId -> true
//                drawPaddle(snake.color, rightBoundary - pWidth, characterLife)
              Right(snake.copy(paddleLeft = rightBoundary - pWidth, characterLife = characterLife, score = score))}

          case 0 => //不动
//              drawPaddle(snake.color, snake.paddleLeft, characterLife)
            Right(snake.copy(characterLife = characterLife, score = score, paddleLeft = paddleLeft))
        }
      }
    }

    var updatedSnakes = List.empty[SkDt]
    var deadSnakes = List.empty[SkDt]

    val acts = actionMap.getOrElse(frameCount, Map.empty[String, Byte])


    snakes.values.map(updateASnake(_, acts)).foreach {
      case Right(s) => updatedSnakes ::= s
      case Left(s) => deadSnakes ::= s
      //      case Left(s) => deadSnakes ::= s.copy(life = (s.life -1).toByte)
      //        mapKillCounter += killerId -> (mapKillCounter.getOrElse(killerId, 0) + 1)
    }

    //    snakes = (updatedSnakes ++ deadSnakes.filterNot(_.life == 0)).map(s => (s.bId, s)).toMap
    snakes = (updatedSnakes ++ deadSnakes).map{s =>
      val off = if (frameCount % 10 == 0)  {
        s.off +1
      } else s.off
      if (frameCount % 10 == 0 && s.level > 0 && !deadList.contains(s.bId)) DrawRank.drawBricks(s.color, snakes.values.toList, bricks.filter(_._2._1 == s.color).toList)
      (s.bId, s.copy(off = off))}.toMap
    snakes.foreach {s =>
      if (drawPaddleMap.get(s._1).nonEmpty && drawPaddleMap(s._1) && !deadList.contains(s._1)) drawPaddle(s._2.color, s._2.paddleLeft, s._2.characterLife)
    }
    deadSnakes
    //      .filter(s => s.life == 0 && !balls.exists(_._2.bId == s.bId))

  }
}
