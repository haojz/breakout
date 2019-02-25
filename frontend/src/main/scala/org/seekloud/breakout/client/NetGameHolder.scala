package org.seekloud.breakout.client

import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import org.seekloud.breakout.Protocol.{GameMessage, GridBallData, GridDataSync}
import org.seekloud.breakout._
import org.seekloud.breakout.pages.Home
import org.seekloud.breakout.utils.{Http, JsFunc, Page}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.{MiddleBufferInJs, decoder}
import mhtml._
import org.seekloud.breakout.Main.host

import scala.xml.Elem
import scalatags.JsDom.short.s
import org.seekloud.breakout.client.DrawRank._

import scala.math.{cos, sin}

/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  */
//@JSExportTopLevel("snake.NetGameHolder")
class NetGameHolder(id: String) extends Page {


  val bounds = Point(Boundary.w, Boundary.h)
  //  val canvasUnit = 10
  //  private val window = Point(Window.w, Window.h)
  //  val canvasUnit = (dom.window.innerWidth.toInt / window.x).toInt
  val canvasUnit = dom.window.innerWidth.toInt / 150

  val canvasBoundary = bounds * canvasUnit
  val textLineHeight = 14

  var myId = 0.toByte
  var isContinue = true

  val grid = new GridOnClient(bounds)

  var firstCome = true
  var justSynced = false

  val startX = grid.startX * canvasUnit
  val startY = grid.startY * canvasUnit
  var lastKey = -1
  var syncFrame: scala.Option[Int] = None
  var newBallMap = Map.empty[Int, List[(Byte, Ball)]]
  var newBricksMap = Map.empty[Int, List[(Byte, (Byte, Byte))]]
  var loseMap = Map.empty[Int, Byte]
  var gameLoopId = -1
  val textList = Var(List.empty[Protocol.Text])
  var waiting4Bricks = false
  var renderId = 0

  var textareaValue = ""

  val display = Var(false)
  var oneMoreList = Var(List.empty[Byte])
  var ifVictory = Var(false)

  private var logicFrameTime = System.currentTimeMillis()


  val watchKeys = Set(
    KeyCode.Space,
    KeyCode.Left,
    //    KeyCode.Up,
    KeyCode.Right,
    //    KeyCode.Down,
    //    KeyCode.F2
  )

  object MyColors {
    val myHeader = "#FF0000"
    val myBody = "#FFFFFF"
    val otherHeader = Color.Blue.toString()
    val otherBody = "#696969"
  }

  private[this] lazy val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] lazy val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val rankCanvas = dom.document.getElementById("RankView").asInstanceOf[Canvas] //排行榜canvas
  private[this] lazy val rankCtx = rankCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val brickCanvas = dom.document.getElementById("BrickView").asInstanceOf[Canvas] //排行榜canvas
  //  private[this] val rankCtx = rankCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val borderCanvas = dom.document.getElementById("BorderView").asInstanceOf[Canvas] //排行榜canvas
  private[this] lazy val borderCtx = borderCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private val victoryImg = dom.document.getElementById("victory_img").asInstanceOf[Image]
  private val defeatImg = dom.document.getElementById("defeat_img").asInstanceOf[Image]

  private[this] val defeat = dom.document.getElementById("defeat").asInstanceOf[HTMLAudioElement]
  private[this] val victory = dom.document.getElementById("victory").asInstanceOf[HTMLAudioElement]
  private[this] val addBall = dom.document.getElementById("add_ball").asInstanceOf[HTMLAudioElement]


  private val ball = dom.document.getElementById("ball").asInstanceOf[Image]
  private val border = dom.document.getElementById("border").asInstanceOf[Image]

  private[this] val webSocketClient: WebSocketClient = new WebSocketClient(connectOpenSuccess, connectError, messageHandler, connectClose)

  private def connectOpenSuccess(event0: Event) = {
    //    run()
    rankCanvas.focus()
    rankCanvas.onkeydown = {
      (e: dom.KeyboardEvent) => {
        if (!grid.deadList.contains(myId)) {
          println(s"keydown: ${e.keyCode}")
          if (watchKeys.contains(e.keyCode)) {
            println(s"key down: [${e.keyCode}]")
            if (e.keyCode == KeyCode.Space) {
              val msg: Protocol.UserAction = Protocol.PressSpace
              webSocketClient.sendMessage(msg)
            } else {
              val key = e.keyCode match {
                case KeyCode.Left => 1
                case KeyCode.Right => 2
              }
              lastKey = key
              val msg: Protocol.UserAction = Protocol.KeyDown(key.toByte, grid.frameCount.toInt)
              webSocketClient.sendMessage(msg)
            }
            e.preventDefault()
          }
        }
      }
    }
    rankCanvas.onkeyup = {
      (e: dom.KeyboardEvent) => {
        if (!grid.deadList.contains(myId)) {
          println(s"keydown: ${e.keyCode}")
          val key = e.keyCode match {
            case KeyCode.Left => 1
            case KeyCode.Right => 2
            case _ => -1
          }
          if (lastKey == key) {
            val msg: Protocol.UserAction = Protocol.KeyUp(grid.frameCount.toInt)
            webSocketClient.sendMessage(msg)
            lastKey = 0
            //            grid.lastAction = 0 //保持不动
            e.preventDefault()
          }
        }
      }
    }
    event0
  }

  private def connectError(e: Event) = {
    drawGameOff()
    e
  }

  private def connectClose(e: Event, s: Boolean) = {
    //    drawGameOff()
    //    dom.window.location.href=s"http://0.0.0.0:40110/breakout/game#/Game/$myId"
    //    dom.window.location.href=s"http://10.1.29.250:40110/breakout"//todo

    e
  }

  def gameRender(): Double => Unit = { _ =>
    val curTime = System.currentTimeMillis()
    //    println(s"requestAnimationTime: ${curTime - lastTime1}")
    val offsetTime = curTime - logicFrameTime
    draw(offsetTime)
    //    lastTime1 = curTime
    if (isContinue)
      renderId = dom.window.requestAnimationFrame(gameRender())
  }

  def init(): Unit = {
    //    ctx.clearRect(0, 0, bounds.x * canvasUnit, bounds.y * canvasUnit)
    borderCanvas.width = canvasBoundary.x.toInt
    borderCanvas.height = canvasBoundary.y.toInt
    borderCtx.globalAlpha = 1
    border.onload = { _ =>
      borderCtx.drawImage(border, 0, 0, 50 * canvasUnit, 60 * canvasUnit)
      borderCtx.drawImage(border, 80 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
    }
    borderCtx.drawImage(border, 0, 0, 50 * canvasUnit, 60 * canvasUnit)
    borderCtx.drawImage(border, 80 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
    canvas.width = canvasBoundary.x.toInt
    canvas.height = canvasBoundary.y.toInt
    borderCtx.fillStyle = Color.White.toString()
    borderCtx.fillRect(50 * canvasUnit, 0, 30 * canvasUnit, Boundary.h * canvasUnit)
    borderCtx.beginPath()
    borderCtx.lineWidth = 3
    borderCtx.strokeStyle = Color.Black.toString()
    borderCtx.moveTo(50 * canvasUnit, 0)
    borderCtx.lineTo(80 * canvasUnit, 0)
    borderCtx.stroke()
    borderCtx.closePath()
    borderCtx.beginPath()
    borderCtx.moveTo(50 * canvasUnit, Boundary.h * canvasUnit)
    borderCtx.lineTo(80 * canvasUnit, Boundary.h * canvasUnit)
    borderCtx.stroke()
    borderCtx.closePath()
//    ctx.fillStyle = Color.White.toString()
//    ctx.lineTo()
//    ctx.fillRect(50 * canvasUnit, 0, 30 * canvasUnit, Boundary.h * canvasUnit)
    webSocketClient.setUp(id)
  }

  def run(): Unit = {
    drawGameOff()

    gameLoopId = dom.window.setInterval(() => gameLoop(), Protocol.frameRate)
    dom.window.requestAnimationFrame(gameRender())

    rankCanvas.width = canvasBoundary.x.toInt
    rankCanvas.height = canvasBoundary.y.toInt

    brickCanvas.width = canvasBoundary.x.toInt
    brickCanvas.height = canvasBoundary.y.toInt

    //    borderCanvas.width = canvasBoundary.x.toInt
    //    borderCanvas.height = canvasBoundary.y.toInt
  }

  def waitingEnd(): Unit = {

  }

  //  def drawGameOn(): Unit = {
  //    ctx.fillStyle = Color.Black.toString()
  //    ctx.fillRect(0, 0, canvas.width, canvas.height)
  //  }

  def drawGameOff(): Unit = {
//    ctx.fillStyle = Color.Black.toString()
//    ctx.fillRect(0, 0, bounds.x * canvasUnit, bounds.y * canvasUnit)
//    ctx.fillStyle = "rgb(250, 250, 250)"
//    if (firstCome) {
//      ctx.font = "36px Helvetica"
//      ctx.fillText("Welcome.", 150, 180)
//    } else {
//      ctx.font = "36px Helvetica"
//      ctx.fillText("Ops, connection lost.", 150, 180)
//    }
  }

  def drawLeft(seat: Int): Unit = {
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
      rankCtx.fillStyle = Color.Black.toString()
      rankCtx.globalAlpha = 0.4
      rankCtx.fillRect(0 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillRect(1 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillStyle = "rgb(250, 250, 250)"
      rankCtx.font = "36px Helvetica"
      rankCtx.globalAlpha = 1
      rankCtx.fillText("该玩家退出本局游戏", seat * x * canvasUnit + 50, 180)
      rankCtx.fillText("房间即将关闭...", seat * x * canvasUnit + 50, 220)
    }
  }

  def drawDefeat(bId: Byte): Unit = {
    drawGrid(-1, grid.getBall4Draw, 0)
    if (grid.snakes.get(bId).nonEmpty) {
      val seat = grid.snakes(bId).color
      rankCtx.fillStyle = Color.Black.toString()
      rankCtx.globalAlpha = 0.4
      rankCtx.fillRect(0 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillRect(1 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillStyle = "rgb(250, 250, 250)"
      rankCtx.font = "36px Helvetica"
      rankCtx.globalAlpha = 1
      rankCtx.drawImage(defeatImg, seat * Boundary.start2 * canvasUnit + 10 ,137, 50, 50)
      rankCtx.fillText("Defeat...", seat * Boundary.start2 * canvasUnit + 65, 180)
      rankCtx.drawImage(victoryImg, scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 10 ,137, 50, 50)
      rankCtx.fillText("Victory...", scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 60, 180)
      println(s"myId:$myId,,,$bId")
      if (bId == myId) {
        defeat.play()
        println(s"defeat!")
      } else {
        victory.play()
        println(s"victory")
      }


    } else if (bId == -1) {
      val seat = 0
      rankCtx.globalAlpha = 0.4
      rankCtx.fillStyle = Color.Black.toString()
      rankCtx.fillRect(0 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillRect(1 * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.fillStyle = "rgb(250, 250, 250)"
      rankCtx.font = "36px Helvetica"
      rankCtx.globalAlpha = 1
      rankCtx.fillText("平局！", seat * Boundary.start2 * canvasUnit + 50, 180)
      rankCtx.fillText("平局！", scala.math.abs(seat - 1) * Boundary.start2 * canvasUnit + 50, 180)
    } else println(s"draw defeat error")

  }


  def gameLoop(): Unit = {
    logicFrameTime = System.currentTimeMillis()
    if (webSocketClient.getWsState) {
      if (syncFrame.nonEmpty) {
        val frontend = grid.frameCount
        val backend = syncFrame.get
        val advancedFrame = backend - frontend
        if (advancedFrame == 1) {
          //          println(s"backend advanced frontend,frontend$frontend,backend:$backend")
          update()
        } else if (advancedFrame < 0 && grid.historyStateMap.get(backend).nonEmpty) {
          println(s"frontend advanced backend,frontend$frontend,backend:$backend")
          grid.setGridInGivenFrame(backend)
          DrawRank.init(canvasUnit, grid.snakes.values.toList, grid.bricks.toList)
        } else if (advancedFrame == 0) {
          println(s"frontend equal to backend,frontend$frontend,backend:$backend")
        } else if (advancedFrame > 0 && advancedFrame < (grid.maxDelayed - 1)) {
          println(s"backend advanced frontend,frontend$frontend,backend:$backend")
          val endFrame = backend
          (frontend until endFrame).foreach { _ =>
            update()
          }
          println(s"after speed,frame:${grid.frameCount}")
        } else {
          if (webSocketClient.getWsState) {
            println(s"===========请求同步数据")
            val msg: Protocol.UserAction = Protocol.NeedToSync
            webSocketClient.sendMessage(msg)
          }
        }
        syncFrame = None
      } else update()
    }
    //    draw()
  }

  def update(): Unit = {
    //    println(s"update::::::::::${grid.frameCount}")
    grid.historyStateMap += grid.frameCount.toInt -> (grid.bricks, grid.balls, grid.snakes, grid.lastAction)
    val deadUsers = grid.update()
    val filterDead = deadUsers.filter(u => u.life == 0 && !grid.balls.exists(_._2.bId == u.bId))
    if (filterDead.nonEmpty) grid.deadList = grid.deadList ::: filterDead.map(_.bId)
    filterDead.foreach { s =>
      grid.lastAction += s.bId -> 0
      rankCtx.fillStyle = Color.Black.toString()
      rankCtx.globalAlpha = 0.4
      rankCtx.fillRect(s.color * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.globalAlpha = 1
    }

    //    if (grid.snakes.exists(s => s._2.score != 0 && s._2.score )&& !waiting4Bricks)
    if (newBallMap.contains(grid.frameCount.toInt)) {
      val newBalls = newBallMap(grid.frameCount.toInt)
      grid.balls ++= newBalls.toMap
      if (newBalls.exists(_._2.bId == myId)) addBall.play()
      newBalls.map(_._2.bId).foreach { b =>
        val newLife = grid.snakes(b).life - 1
        grid.snakes += b -> grid.snakes(b).copy(life = newLife.toByte)
        drawLife(grid.snakes(b).color, newLife)
      }
      //      newBalls.map(_._2.bId).foreach(b => grid.snakes += b -> grid.snakes(b).copy(life = (grid.snakes(b).life -1).toByte))
      //      newBallMap -= grid.frameCount.toInt
    }
    if (newBricksMap.contains(grid.frameCount.toInt)) {
      val bricks = newBricksMap(grid.frameCount.toInt).toMap
      grid.bricks ++= bricks
      if (bricks.contains((numEveryRow * rowNum).toByte)) {
        val snakeF = grid.snakes.find(_._2.color == 1)
        if (snakeF.nonEmpty) {
          grid.snakes += snakeF.head._1 -> snakeF.head._2.copy(level = snakeF.head._2.level + 1, off = 0)
          drawLevel(snakeF.head._2.color, snakeF.head._2.level + 1)
          drawBricks(1, grid.snakes.values.toList, bricks.toList)

        }
      } else {
        val snakeF = grid.snakes.find(_._2.color == 0)
        if (snakeF.nonEmpty) {
          val sk = snakeF.head._2
          grid.snakes += snakeF.head._1 -> sk.copy(level = sk.level + 1, off = 0)
          drawLevel(sk.color, sk.level + 1)
          drawBricks(0, grid.snakes.values.toList, bricks.toList)

        }
      }
    }

    val limitFrameCount = grid.frameCount - (grid.maxDelayed + 1)
    grid.actionMap = grid.actionMap.filterKeys(_ >= limitFrameCount)
    grid.historyStateMap = grid.historyStateMap.filter(_._1 > limitFrameCount)
    newBricksMap = newBricksMap.filter(_._1 > limitFrameCount)
    newBallMap = newBallMap.filter(_._1 > limitFrameCount)
  }

  def draw(offsetTime: Long): Unit = {
    if (webSocketClient.getWsState) {
      if (loseMap.get(grid.frameCount.toInt).nonEmpty) {
        drawDefeat(loseMap(grid.frameCount.toInt))
        ifVictory := true
        dom.window.clearInterval(gameLoopId)
        isContinue = false
        dom.window.clearInterval(renderId)

      } else {
        val data = grid.getBall4Draw

        drawGrid(myId, data, offsetTime)
      }
    } else {
      drawGameOff()
    }
  }


  def drawGrid(uid: Long, data: GridBallData, offsetTime: Long): Unit = {
    //    println(s"===========draw:::$canvasUnit")
    val width = 4 * canvasUnit
    val height = 2 * canvasUnit

    //    ctx.fillStyle = Color.Black.toString()
    ctx.clearRect(0, 0, 50 * canvasUnit, bounds.y * canvasUnit)
    ctx.clearRect(80 * canvasUnit, 0, 50 * canvasUnit, bounds.y * canvasUnit)
//    ctx.fillStyle = Color.White.toString()
//    ctx.fillRect(50 * canvasUnit, 0, 30 * canvasUnit, Boundary.h * canvasUnit)

    //    rankCtx.fillStyle = Color.Black.toString()
    //    rankCtx.fillRect(0, 0, 50 * canvasUnit, 4 * canvasUnit)
    //    rankCtx.fillStyle = Color.White.toString()
    //    rankCtx.font = "30px Helvetica"
    //    rankCtx.fillText("rank canvas test！！！！！！！！", 0, 0 , 50 * canvasUnit)

    (0 to 1).foreach { i =>
      if (!data.snakes.exists(_.color == i)) drawLeft(i)
    }

    //    rankCtx.fillStyle = Color.Black.toString()
    //    rankCtx.fillRect(0, 0, Boundary.end1 * canvasUnit, startY) //* 5, * 2
    //    rankCtx.fillRect(Boundary.start2 * canvasUnit, 0, Boundary.end1 * canvasUnit, startY) //* 5, * 2

    //    val snakes = data.snakes
    //    val bricks = data.bricks
    val balls = data.balls

    //    ctx.fillStyle =  MyColors.myBody

    //    bricks.foreach { b =>
    //      val seat = b._2._1
    //      val snake = data.snakes.find(_.color == seat)
    //      val y = (b._1 - seat * rowNum * numEveryRow) / numEveryRow
    //      val x = (b._1 - seat * rowNum * numEveryRow) % numEveryRow
    //      val offY = if(snake.isEmpty) 0 else snake.get.level * snake.get.off
    //      val brickColor = b._2._2 match {
    //        case 0 => blueBrick
    //        case 1 => yellowBrick
    //        case _ => greenBrick
    //      }
    //      val bY = (((startY + y * height + offY * canvasUnit) / canvasUnit) % grid.paddleY) * canvasUnit
    //      ctx.drawImage(brickColor, startX + seat * Boundary.start2 * canvasUnit + x * width, bY, width - 1, height - 1)
    //
    //    }

    //    snakes.foreach { snake =>
    //      val paddle = snake.color match {
    //        case 0 => redPaddle
    //        case 1 => bluePaddle
    //      }
    //      val paddleWidth = snake.characterLife match {
    //        case l if l > 0 => (grid.paddleWidth * 1.5).toInt
    //        case _ => grid.paddleWidth
    //      }
    //
    //      ctx.drawImage(paddle, snake.paddleLeft * canvasUnit, grid.paddleY * canvasUnit,
    //        paddleWidth * canvasUnit, grid.paddleHeight * canvasUnit)

    //      val offX = width * numEveryRow + (snake.color * Boundary.start2 + 2) * canvasUnit
    //      val rightX = (Boundary.end1 + 80 * snake.color) * canvasUnit
    //      rankCtx.fillStyle = Color.Black.toString()
    //      rankCtx.globalAlpha = 1
    //      rankCtx.fillRect(offX, 0, rightX - offX , startY)

    //转到canvasUnit
    //      ctx.drawImage(life, offX, 0.75 * canvasUnit, 2.5 * canvasUnit, 2.5 * canvasUnit)
    //      ctx.drawImage(multi, offX + 3 * canvasUnit,  canvasUnit, 2 * canvasUnit, 2 * canvasUnit)
    //      ctx.fillStyle = Color.White.toString()
    //      ctx.font = "30px Helvetica"
    //      ctx.fillText(s"${snake.life}", offX + 6 * canvasUnit, 3 * canvasUnit)
    //
    //      ctx.fillText(s"SCORE: ${snake.score}", (snake.color * Boundary.start2 + 1) * canvasUnit, 3 * canvasUnit)


    //      ctx.clearRect(startX + (width * numEveryRow + snake.color * Boundary.start2) * canvasUnit, 0, rightX - offX , startY)
    //      ctx.drawImage(life, 0, 0, 1.5 * canvasUnit, 1.5 * canvasUnit)
    //      ctx.drawImage(multi, 2 * canvasUnit, 0, 1.2 * canvasUnit, 1.2 * canvasUnit)
    //      ctx.fillStyle = Color.White.toString()
    //      ctx.fillText(s"${snake.life}", 3.5 * canvasUnit, 0)
    //    }

    balls.foreach { b =>
      val theta = b.theta
      val speed = b.speed * (offsetTime / Protocol.frameRate)
      val point = b.point
      val speedX = (speed * cos(theta)).formatted("%.4f")
      val speedY = (-speed * sin(theta)).formatted("%.4f")
      //        println(s"frame: $frameCount, speedX: $speedX, speedY: $speedY")

      val newPoint = (point + Point(speedX.toFloat, speedY.toFloat)).format
      //      val x = b.point.x
      //      val y = b.point.y
      val x = newPoint.x
      val y = newPoint.y
      ctx.drawImage(ball, x * canvasUnit, y * canvasUnit,
        grid.ballRadius * 2 * canvasUnit, grid.ballRadius * 2 * canvasUnit)
    }


  }

  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

  private def messageHandler(data: GameMessage): Unit = {
    data match {
      case Protocol.Id(bId) =>
        myId = bId
        grid.myId = bId
        println(s"recv myId::$bId")
      //      case Protocol.TextMsg(message) => writeToArea(s"MESSAGE: $message")
      //      case Protocol.NewSnakeJoined(id, user) => writeToArea(s"$user joined!")
      //      case Protocol.SnakeLeft(id, user) => writeToArea(s"$user left!")

      case Protocol.SyncFrame(frame) =>
        //        println(s"【syncFrame:$frame, front: ${grid.frameCount}")
        syncFrame = Some(frame)

      case msg: Protocol.TotalData =>
        println(s"======================recv total data")
        grid.resetTotalData(msg)
        if (firstCome) {
          firstCome = false
          run()
          DrawRank.init(canvasUnit, msg.snakes.map(_._2), msg.bricks)
        }

      case Protocol.UserDead(frame, users) =>
        println(s"!!!!!!!!!!!!!!!usersDead:$users")

      case Protocol.UserLeft(bId) =>
        if (grid.snakes.get(bId).nonEmpty) {
          val seat = grid.snakes(bId).color
          drawLeft(seat)
          grid.snakes -= bId
          grid.bricks = grid.bricks.filterNot(_._2._1 == seat)
          grid.balls = grid.balls.filterNot(_._2.bId == bId)

        }

      case Protocol.NewSnake(snake) =>
        println(s"new snake::::$snake")
        grid.snakes += snake.bId -> snake

      case Protocol.CloseWs =>
        dom.window.location.href=s"http://${Main.host}:40110/breakout#/Home"
      //        dom.window.location.href=s"http://192.168.1.103:40110/breakout#/Home"

      case Protocol.InitBricks(frame, bricks) =>
        newBricksMap += frame -> bricks
        if (grid.frameCount == frame) {
          val bricks = newBricksMap(frame).toMap
          grid.bricks ++= bricks
          if (bricks.contains((numEveryRow * rowNum).toByte)) {
            val snakeF = grid.snakes.find(_._2.color == 1)
            if (snakeF.nonEmpty) {
              val sk = snakeF.head._2
              grid.snakes += snakeF.head._1 -> sk.copy(level = sk.level + 1, off = 0)
              drawLevel(sk.color, sk.level + 1)
              drawBricks(1, grid.snakes.values.toList, bricks.toList)

            }
          } else {
            val snakeF = grid.snakes.find(_._2.color == 0)
            if (snakeF.nonEmpty) {
              val sk = snakeF.head._2
              grid.snakes += snakeF.head._1 -> sk.copy(level = sk.level + 1, off = 0)
              drawLevel(sk.color, sk.level + 1)
              drawBricks(0, grid.snakes.values.toList, bricks.toList)
            }
          }
        }
        else if (grid.frameCount > frame) {
          //          println(s"~~~~~~~~~~~~~~~~~~~error: 需要回溯")
          //          println(s"===========请求同步数据 when initbricks")
          //          val msg: Protocol.UserAction = Protocol.NeedToSync
          //          webSocketClient.sendMessage(msg)
          syncFrame = Some(frame.toInt)
        }

      //      case Protocol.InitBricks(bricks) =>
      //        println(s"recv bricks")
      //        grid.bricks = bricks.toMap

      case Protocol.NewBalls(frame, balls) =>
        newBallMap += frame -> balls
        //        if (firstCome) {
        //          grid.frameCount = frame
        //          firstCome = false
        //        }
        println(s"recv newBalls:$balls, front: ${grid.frameCount}, backend: $frame")
        if (grid.frameCount == frame) {
          grid.balls ++= newBallMap(frame).toMap
          balls.map(_._2.bId).foreach { b =>
            val newLife = grid.snakes(b).life - 1
            grid.snakes += b -> grid.snakes(b).copy(life = newLife.toByte)
            drawLife(grid.snakes(b).color, newLife)
          }
          if (balls.exists(_._2.bId == myId)) addBall.play()

        }
        else if (grid.frameCount > frame) {
          //          println(s"===========请求同步数据 when initbricks")
          //          val msg: Protocol.UserAction = Protocol.NeedToSync
          //          webSocketClient.sendMessage(msg)
          syncFrame = Some(frame.toInt)
          //          println(s"~~~~~~~~~~~~~~~~~~~error: 需要回溯")
        }
      //                    grid.balls ++= balls.toMap
      //                    grid.frameCount == frame

      case a@Protocol.SnakeAction(bId, keyCode, frame) =>
        if (frame >= grid.frameCount.toInt) {
          //                      println(s"!!! got snake action=$a whem i am in frame=${grid.frameCount}")
        } else {
          println(s"!!! got snake action=$a whem i am in frame=${grid.frameCount}")
          syncFrame = Some(frame)
          //writeToArea(s"got snake action=$a")
        }
        grid.addActionWithFrame(grid.snakes(bId).id, keyCode, frame.toLong)

      case data: Protocol.Text =>
        println(s"recv data: $data")
        textList.update(t => t :+ data)

      case data: Protocol.ScoreTest =>
        println(s"=====recv data: $data")

      case data@Protocol.SomeOneLose(frame, id, score) =>
        println(s"recv data:::==========================:$data; front: ${grid.snakes.map(s => (s._2.color, s._2.score))}")
        if (frame <= grid.frameCount.toInt) {
          drawDefeat(id)
          ifVictory := true
          dom.window.clearInterval(gameLoopId)
          isContinue = false
          dom.window.clearInterval(renderId)
        } else loseMap += frame -> id

      case Protocol.UserConfirm(bId) =>
        oneMoreList.update(l => l :+ bId)


      case Protocol.ReJoin =>
        ifVictory := false
        oneMoreList := Nil
        isContinue = true
        grid.resetGrid()
        firstCome = true
        justSynced = false
        lastKey = -1
        syncFrame = None
        newBallMap = Map.empty[Int, List[(Byte, Ball)]]
        newBricksMap = Map.empty[Int, List[(Byte, (Byte, Byte))]]
        loseMap = Map.empty[Int, Byte]
        gameLoopId = -1
        waiting4Bricks = false
        renderId = 0
        logicFrameTime = System.currentTimeMillis()


      case _ =>



      //      case Protocol.NetDelayTest(createTime) =>
      //        val receiveTime = System.currentTimeMillis()
      //        val m = s"Net Delay Test: createTime=$createTime, receiveTime=$receiveTime, twoWayDelay=${receiveTime - createTime}"
      //        writeToArea(m)
    }
  }


  //  def p(msg: String) = {
  //    val paragraph = dom.document.createElement("p")
  //    paragraph.innerHTML = msg
  //    paragraph
  //  }
  def sendText(): Unit = {
    val msg = dom.document.getElementById("textarea").asInstanceOf[TextArea].value
    if (msg == "") JsFunc.alert("输入不能为空！")
    else {
      webSocketClient.sendMessage(Protocol.SendText(msg))
      dom.document.getElementById("textarea").asInstanceOf[TextArea].value = ""
    }
  }

  def addEmotion(id: Int): Unit = {
    val oldInnerHtml = dom.document.getElementById("textarea").asInstanceOf[TextArea].value
    val newInnerHtml = oldInnerHtml + s"[表情$id]"
    dom.document.getElementById("textarea").asInstanceOf[TextArea].value = newInnerHtml

  }


  val detail = display.map {
    case true =>
      <div style="margin: 20px;" onkeydown={e: KeyboardEvent => if (e.keyCode == 13) sendText()}>
        <img src="/breakout/static/img/biaoqing.png" style="width: 25px;margin-right: 8px;" onclick={() => {
          textareaValue = dom.document.getElementById("textarea").asInstanceOf[TextArea].value
          display := false
        }}></img>
        <textarea id="textarea" style="margin-right: 10px;">{textareaValue}</textarea>
        <button class="btn btn-primary" style="position: relative; top: -10px; width: 60px; height: 27px;" onclick={() => sendText()}>发送</button>
        <div style="position: absolute;">
          {(1 to 10).map { i =>
          <img src={s"/breakout/static/img/Emotion$i.jpg"} style="width: 30px; margin-right: 5px;" onclick={() => addEmotion(i)}></img>
        }}
        </div>
      </div>

    case false =>
      <div style="margin: 20px;" onkeydown={e: KeyboardEvent => if (e.keyCode == 13) sendText()}>
        <img src="/breakout/static/img/biaoqing.png" style="width: 25px;margin-right: 8px;" onclick={() => {
          textareaValue = dom.document.getElementById("textarea").asInstanceOf[TextArea].value
          display := true
        }}></img>
        <textarea id="textarea" style="margin-right: 10px;">{textareaValue}</textarea>
        <button class="btn btn-primary" style="position: relative; top: -10px; width: 60px; height: 27px;" onclick={() => sendText()}>发送</button>
      </div>
  }

  def reJoin(): Unit = {
    val msg: Protocol.UserAction = Protocol.OneMoreGame
    webSocketClient.sendMessage(msg)
  }

  val oneMore = ifVictory.zip(oneMoreList).map {
    case (false, _) => emptyHTML
    case (true, Nil) =>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" onclick={() => reJoin(); }>再来一局</button>
        <p style="color: red;">请在10秒内点击哦..等待超过15秒, 房间将关闭..</p>
      </div>
    case (true, list) if list.lengthCompare(1) == 0 && list.contains(myId)=>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" disbaled ="true">再来一局</button>
        <p>您已请求再来一局, 等待对方玩家请求..等待超过15秒, 房间将关闭..</p>
      </div>

    case (true, list) if list.lengthCompare(1) == 0 && !list.contains(myId) =>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" onclick={() => reJoin();}>再来一局</button>
        <p>对方玩家已请求再来一局, 等待您的请求..等待超过15秒, 房间将关闭..</p>
      </div>

    case (true, list) if list.lengthCompare(2) == 0 =>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" disbaled ="true">再来一局</button>
        <p>双方玩家都已确认再来一局，游戏即将重新开始..</p>
      </div>

    case _ => emptyHTML


   }

  val top = 2 * canvasUnit
  val left = (Boundary.end1 + 4) * canvasUnit
  val width = (Boundary.start2 - Boundary.end1 - 6) * canvasUnit
  val height = (Boundary.h - 2) * canvasUnit
  val left2 = (Boundary.end2 + 2) * canvasUnit
  val wid = 50 * canvasUnit
  //  val left2 = 80 * canvasUnit

  val textListRx = textList.map { list =>
    list.map { l =>
      val botStyle = if (l.seat == 0) "left: -24px; border-color: transparent #beceeb #beceeb transparent;" else s"left: ${width - 10}px; border-color: transparent transparent #beceeb #beceeb;"
      val topStyle = if (l.seat == 0) "left: -14px; border-color: transparent #ffffff #ffffff transparent;" else s"left: ${width - 20}px; border-color: transparent transparent #ffffff #ffffff;"
      <div class="text" style="z-index: 1002;">
        <span class="bot" style={botStyle}></span>
        <span class="top" style={topStyle}></span>{val text = l.msg.split("\\[|\\]")
      val a = text.map { t =>
        if (t.contains("表情")) <img src={s"/breakout/static/img/Emotion${t.drop(2)}.jpg"} style="width: 25px;"></img>
        else <p style="color: #6C748D; margin: 0px; display: inline;">
          {t}
        </p>
      }
      a.toList}
      </div>
    }


  }

  override def render(): Elem = {
    init()
    val name1 = dom.window.localStorage.getItem("name1")
    val name2 = dom.window.localStorage.getItem("name2")
    println(s"canvasUnit::::::$canvasUnit")
    println(s"name1:::$name1")
    println(s"name2:::$name2")

    <div>
      <button class="btn btn-primary" style={s"position: absolute; left: ${dom.window.innerWidth - 100}px; top: 10px; width: 85px;"} onclick={() => dom.window.location.href = s"http://$host:40110/breakout"}>返回大厅</button>
      <div>
      </div>
      <div>
        {detail}
      </div>
      <div style={s"z-index: 1000; position: absolute; top:${top}px; left: ${left - 6 * canvasUnit}px; width: ${width + 8 * canvasUnit}px; height: ${height}px; overflow-y: scroll; overflow-x: hidden;margin-top: 50px; margin-left: 25px;"}>
        <div style={s"z-index: 1001;position: relative; left: 48px; width: ${width}px;"}>
          {textListRx}
        </div>
      </div>
      <div style={s"postion: absolute; left: ${left}px;top: ${top}px;"}></div>
      <div></div>
      <div style="margin-left: 25px;">
        <div style={s"position: absolute; width: ${wid}px; top:10px; text-align: center;"}>
          <h1 style="font-size: 22px; color: gray;">
            {name1}
          </h1>
        </div>
        <div style={s"position: absolute; left: ${wid}px; top:10px; width:${30 * canvasUnit}px; text-align: center; margin-left: 25px;"}>
          <img src="/breakout/static/img/pk.png" style="width: 30px;"></img>
        </div>
        <div style={s"position: absolute; left: ${80 * canvasUnit}px; top:10px; width: ${wid}px; text-align: center"}>
          <h1 style="font-size: 22px; color: gray;">
            {name2}
          </h1>
        </div>
      </div>
      {oneMore}
      <div style={s"position: absolute; left: ${left2}px; top: ${top}px; margin-left: 25px; margin-top: 50px; width:${dom.window.innerWidth - 132 * canvasUnit - 35}px;"}>
        <h1 style="font-size: 17px;">=>玩法介绍</h1>
        <p>>>键盘操作（左右移动，空格发球）</p>
        <p>>>右上角爱心值为剩余可发球数(ps: 可同时发多个球）</p>
        <p>>>打到黄色砖块可增加接球板长度</p>
        <p>>>打到绿色砖块可多弹出一个球</p>
        <p>>>左下角输入框可聊天并输入表情</p>
        <p>>>若一方无球可发，且没有球在界面上，则判定为死亡</p>
        <p>>>若一方死亡，另一方分数超过死亡玩家，则死亡玩家失败</p>
        <p>>>若两方都死亡，则分数低者失败</p>
        <p>>>游戏结束后，可点击下方再来一局按钮！</p>
        <p>>>打开音效玩耍，游戏体验更佳！</p>
      </div>
    </div>
  }

}
