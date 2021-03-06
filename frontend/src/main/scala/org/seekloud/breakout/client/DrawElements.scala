package org.seekloud.breakout.client

import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Image}
import org.scalajs.dom.raw.{CanvasRenderingContext2D, HTMLAudioElement}
import org.seekloud.breakout.{Boundary, Grid, Protocol, _}
import org.seekloud.breakout.Protocol.GridBallData

import scalatags.JsDom.short.s
import scala.math.{cos, sin}


/**
  * Created by haoshuhan on 2019/2/22.
  */
object DrawElements {
  private[this] val canvas = dom.document.getElementById("RankView").asInstanceOf[Canvas] //排行榜canvas
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val brickCanvas = dom.document.getElementById("BrickView").asInstanceOf[Canvas] //排行榜canvas
  private[this] val brickCtx = brickCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private val life = dom.document.getElementById("life").asInstanceOf[Image]
  private val multi = dom.document.getElementById("multi").asInstanceOf[Image]
  private val bluePaddle = dom.document.getElementById("bluePaddle").asInstanceOf[Image]
  private val redPaddle = dom.document.getElementById("redPaddle").asInstanceOf[Image]
  private val blueBrick = dom.document.getElementById("blueBrick").asInstanceOf[Image]
  private val greenBrick = dom.document.getElementById("greenBrick").asInstanceOf[Image]
  private val yellowBrick = dom.document.getElementById("yellowBrick").asInstanceOf[Image]
  private val ball = dom.document.getElementById("ball").asInstanceOf[Image]
  private val victoryImg = dom.document.getElementById("victory_img").asInstanceOf[Image]
  private val defeatImg = dom.document.getElementById("defeat_img").asInstanceOf[Image]

  private[this] val defeat = dom.document.getElementById("defeat").asInstanceOf[HTMLAudioElement]
  private[this] val victory = dom.document.getElementById("victory").asInstanceOf[HTMLAudioElement]

  var canvasUnit = 0
  val paddleWidth = 8
  val paddleHeight = 2
  val paddleY = Boundary.h - 10
  val startX = 5
  val startY = 4



  def drawScore(seat: Int, score: Int) = {
    ctx.clearRect(13.6 * canvasUnit + seat * Boundary.start2 * canvasUnit, 0, 16 * canvasUnit, 3.3 * canvasUnit)
    ctx.fillStyle = Color.White.toString()
    ctx.font = "30px Helvetica"
    ctx.fillText(s"${score}", (seat * Boundary.start2 + 15) * canvasUnit, 3 * canvasUnit)


  }

  def drawLife(seat: Int, life: Int) = {
    val width = 4 * canvasUnit
    val offX = width * numEveryRow + (seat * Boundary.start2 + 2) * canvasUnit
    ctx.clearRect(offX + 6 * canvasUnit, 0, 2 * canvasUnit, 3.3 * canvasUnit)
    ctx.fillStyle = Color.White.toString()
    ctx.font = "30px Helvetica"
    ctx.fillText(s"$life", offX + 6 * canvasUnit, 3 * canvasUnit)
  }

  def drawLevel(seat: Int, level: Int) = {
    println(s"drawLevel: seat: $seat, $level")
    ctx.clearRect(seat * Boundary.start2 * canvasUnit, 56 * canvasUnit, 20 * canvasUnit, 4 * canvasUnit)
    ctx.fillStyle = Color.White.toString()
    ctx.font = "30px Helvetica"
    ctx.fillText(s"Level:${level + 1}", seat * Boundary.start2 * canvasUnit + 2 * canvasUnit, 59 * canvasUnit)


  }

  def drawPaddle(seat: Int, paddleLeft: Int, characterLife: Int) = {

    ctx.clearRect(seat * Boundary.start2 * canvasUnit, paddleY * canvasUnit, 50 * canvasUnit, paddleHeight * canvasUnit)
    val paddle = seat match {
        case 0 => redPaddle
        case 1 => bluePaddle
      }
      val paddleW = characterLife match {
        case l if l > 0 => (paddleWidth * 1.5).toInt
        case _ => paddleWidth
      }

      ctx.drawImage(paddle, paddleLeft * canvasUnit, paddleY * canvasUnit,
        paddleW * canvasUnit, paddleHeight * canvasUnit)
  }

  def removeBrick(seat: Int, brickId: Byte, offY: Int) = {
    val height = 2 * canvasUnit
    val width = 4 * canvasUnit
    val y = (brickId - seat * rowNum * numEveryRow) / numEveryRow
    val x = (brickId - seat * rowNum * numEveryRow) % numEveryRow
    val bX = startX * canvasUnit + seat * Boundary.start2 * canvasUnit + x * width
    val bY = (((startY * canvasUnit + y * height + offY * canvasUnit) / canvasUnit) % paddleY) * canvasUnit
    brickCtx.clearRect(bX, bY, width -1, height -1)
  }

  def drawBricks(seat: Int, snakes: List[SkDt], bricks: List[(Byte, (Byte, Byte))]) = {
    brickCtx.clearRect(seat * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
    val snake = snakes.find(_.color == seat)
    val width = 4 * canvasUnit
    val height = 2 * canvasUnit
    bricks.foreach {b =>
      val y = (b._1 - seat * rowNum * numEveryRow) / numEveryRow
      val x = (b._1 - seat * rowNum * numEveryRow) % numEveryRow
      val offY = if(snake.isEmpty) 0 else snake.get.level * snake.get.off
      val brickColor = b._2._2 match {
        case 0 => blueBrick
        case 1 => yellowBrick
        case _ => greenBrick
      }
      val bY = (((startY * canvasUnit + y * height + offY * canvasUnit) / canvasUnit) % paddleY) * canvasUnit
      brickCtx.drawImage(brickColor, startX* canvasUnit + seat * Boundary.start2 * canvasUnit + x * width, bY, width - 1, height - 1)
    }
  }

  def drawGrid(ctx: CanvasRenderingContext2D, firstCome: Boolean, uid: Long, data: GridBallData, offsetTime: Long): Unit = {
    ctx.clearRect(0, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
    ctx.clearRect(80 * canvasUnit, 0, 50 * canvasUnit, Boundary.h * canvasUnit)

    (0 to 1).foreach { i =>
      if (!data.snakes.exists(_.color == i)) drawLeft(firstCome, ctx, i)
    }

    val balls = data.balls

    balls.foreach { b =>
      val theta = b.theta
      val speed = b.speed * (offsetTime / Protocol.frameRate)
      val point = b.point
      val speedX = (speed * cos(theta)).formatted("%.4f")
      val speedY = (-speed * sin(theta)).formatted("%.4f")
      val newPoint = (point + Point(speedX.toFloat, speedY.toFloat)).format
      val x = newPoint.x
      val y = newPoint.y
      ctx.drawImage(ball, x * canvasUnit, y * canvasUnit,
        Constant.ballRadius * 2 * canvasUnit, Constant.ballRadius * 2 * canvasUnit)
    }
  }

  def drawLeft(firstCome: Boolean, ctx: CanvasRenderingContext2D, seat: Int): Unit = {
    val x = seat match {
      case 0 => Boundary.start1
      case 1 => Boundary.start2
    }

    if (firstCome) {
      ctx.fillStyle = Color.Black.toString()
      ctx.fillRect(x * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      ctx.fillStyle = "rgb(250, 250, 250)"
      ctx.font = "36px Helvetica"
      ctx.fillText("等待该玩家进入...", seat * x * canvasUnit + 50, 180)

    } else {
      fillHalfBalck()
      ctx.fillStyle = "rgb(250, 250, 250)"
      ctx.font = "36px Helvetica"
      ctx.globalAlpha = 1
      ctx.fillText("该玩家退出本局游戏", seat * x * canvasUnit + 50, 180)
      ctx.fillText("房间即将关闭...", seat * x * canvasUnit + 50, 220)
    }
  }

  def fillHalfBalck(): Unit = {
    ctx.globalAlpha = 0.4
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
    ctx.fillRect(1 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
  }


  def drawDefeat(firstCome: Boolean, ctx: CanvasRenderingContext2D, myId: Byte,
                 balls: Protocol.GridBallData, bId: Byte, snakes: Map[Byte, SkDt]): Unit = {
    drawGrid(ctx, firstCome, -1, balls, 0)
    if (snakes.get(bId).nonEmpty) {
      val seat = snakes(bId).color
      fillHalfBalck()
      ctx.fillStyle = "rgb(250, 250, 250)"
      ctx.font = "36px Helvetica"
      ctx.globalAlpha = 1
      ctx.drawImage(defeatImg, seat * Boundary.start2 * canvasUnit + 10 ,137, 50, 50)
      ctx.fillText("Defeat...", seat * Boundary.start2 * canvasUnit + 65, 180)
      ctx.drawImage(victoryImg, scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 10 ,137, 50, 50)
      ctx.fillText("Victory...", scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 60, 180)
      if (bId == myId) {
        defeat.play()
      } else {
        victory.play()
      }

    } else if (bId == -1) {
      val seat = 0
      fillHalfBalck()
      ctx.fillStyle = "rgb(250, 250, 250)"
      ctx.font = "36px Helvetica"
      ctx.globalAlpha = 1
      ctx.fillText("平局！", seat * Boundary.start2 * canvasUnit + 50, 180)
      ctx.fillText("平局！", scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 50, 180)
    } else println(s"draw defeat error")

  }


  def init(canvasUnitRecv: Int, snakes:List[SkDt], bricks: List[(Byte, (Byte, Byte))], first: Boolean = true) = {
    canvasUnit = canvasUnitRecv
    if (first) {
      ctx.fillStyle = "rgba(255, 255, 255, 0)"
      ctx.fillRect(0, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
      ctx.fillRect(80 * canvasUnit, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
      ctx.globalAlpha = 1
      brickCtx.fillStyle = "rgba(255, 255, 255, 0)"
      brickCtx.fillRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
      brickCtx.globalAlpha = 1
    } else {
      ctx.clearRect(0, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
      ctx.clearRect(80 * canvasUnit, 0, 50 * canvasUnit, Boundary.h * canvasUnit)
      brickCtx.clearRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
    }
    val width = 4 * canvasUnit
    val height = 2 * canvasUnit

    bricks.foreach { b =>
      val seat = b._2._1
      val snake = snakes.find(_.color == seat)
      val y = (b._1 - seat * rowNum * numEveryRow) / numEveryRow
      val x = (b._1 - seat * rowNum * numEveryRow) % numEveryRow
      val offY = if(snake.isEmpty) 0 else snake.get.level * snake.get.off
      val brickColor = b._2._2 match {
        case 0 => blueBrick
        case 1 => yellowBrick
        case _ => greenBrick
      }
      val bY = (((startY * canvasUnit + y * height + offY * canvasUnit) / canvasUnit) % paddleY) * canvasUnit
      brickCtx.drawImage(brickColor, startX* canvasUnit + seat * Boundary.start2 * canvasUnit + x * width, bY, width - 1, height - 1)

    }

    snakes.foreach {snake =>
      drawLevel(snake.color, snake.level)
      val paddle = snake.color match {
        case 0 => redPaddle
        case 1 => bluePaddle
      }
      val paddleW = snake.characterLife match {
        case l if l > 0 => (paddleWidth * 1.5).toInt

        case _ => paddleWidth
      }

      ctx.drawImage(paddle, snake.paddleLeft * canvasUnit, paddleY * canvasUnit,
        paddleW * canvasUnit, paddleHeight * canvasUnit)

      ctx.clearRect(snake.color * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 6 * canvasUnit)
      val offX = width * numEveryRow + (snake.color * Boundary.start2 + 2) * canvasUnit
      ctx.drawImage(life, offX, 0.75 * canvasUnit, 2.5 * canvasUnit, 2.5 * canvasUnit)
      ctx.drawImage(multi, offX + 3 * canvasUnit,  canvasUnit, 2 * canvasUnit, 2 * canvasUnit)
      ctx.fillStyle = Color.White.toString()
      ctx.font = "30px Helvetica"
      ctx.fillText(s"${snake.life}", offX + 6 * canvasUnit, 3 * canvasUnit)

      ctx.fillText(s"SCORE: ${snake.score}", (snake.color * Boundary.start2 + 1) * canvasUnit, 3 * canvasUnit)
    }
  }

}
