package org.seekloud.breakout.client

import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Image}
import org.seekloud.breakout.Boundary
import org.seekloud.breakout.Grid
import scalatags.JsDom.short.s
import org.seekloud.breakout._


/**
  * Created by haoshuhan on 2019/2/22.
  */
object DrawRank {
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
  var canvasUnit = 0
  val paddleWidth = 8
  val paddleHeight = 2
  val paddleY = Boundary.h - 10
  val startX = 5
  val startY = 4



  def drawScore(seat: Int, score: Int) = {
//    ctx.fillStyle = Color.Black.toString()
//    ctx.fillRect(15 * canvasUnit, 0, 16 * canvasUnit, 2.5 * canvasUnit)
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

    ctx.clearRect(seat * Boundary.start2 * canvasUnit, paddleY * canvasUnit, 62 * canvasUnit, paddleHeight * canvasUnit)
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

  def init(canvasUnitRecv: Int, snakes:List[SkDt], bricks: List[(Byte, (Byte, Byte))], first: Boolean = true) = {
    canvasUnit = canvasUnitRecv
    if (first) {
      ctx.fillStyle = "rgba(255, 255, 255, 0)"
      ctx.fillRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
      ctx.globalAlpha = 1
      brickCtx.fillStyle = "rgba(255, 255, 255, 0)"
      brickCtx.fillRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
      brickCtx.globalAlpha = 1
    } else {
      ctx.clearRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
      brickCtx.clearRect(0, 0, Boundary.w * canvasUnit, Boundary.h * canvasUnit)
    }
    val width = 4 * canvasUnit
    val height = 2 * canvasUnit

//    drawBricks(snakes, bricks)
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

      val offX = width * numEveryRow + (snake.color * Boundary.start2 + 2) * canvasUnit
      //      val rightX = (Boundary.end1 + 80 * snake.color) * canvasUnit
      //      rankCtx.fillStyle = Color.Black.toString()
      //      rankCtx.globalAlpha = 1
      //      rankCtx.fillRect(offX, 0, rightX - offX , startY)
      ctx.drawImage(life, offX, 0.75 * canvasUnit, 2.5 * canvasUnit, 2.5 * canvasUnit)
      ctx.drawImage(multi, offX + 3 * canvasUnit,  canvasUnit, 2 * canvasUnit, 2 * canvasUnit)
      ctx.fillStyle = Color.White.toString()
      ctx.font = "30px Helvetica"
      ctx.fillText(s"${snake.life}", offX + 6 * canvasUnit, 3 * canvasUnit)

      ctx.fillText(s"SCORE: ${snake.score}", (snake.color * Boundary.start2 + 1) * canvasUnit, 3 * canvasUnit)
    }
  }

//  def changeToEnd(): Unit = {
//    end = true
//  }
}
