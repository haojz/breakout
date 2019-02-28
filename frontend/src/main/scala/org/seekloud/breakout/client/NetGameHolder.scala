package org.seekloud.breakout.client

import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import org.seekloud.breakout.Protocol.{GameMessage, GridBallData, GridDataSync}
import org.seekloud.breakout._
import org.seekloud.breakout.utils.{Http, JsFunc, Page}
import mhtml._
import org.seekloud.breakout.Main.host
import scala.xml.Elem
import org.seekloud.breakout.client.DrawElements._
import scala.math.{cos, sin}

/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  */
class NetGameHolder(id: String) extends Page {


  val bounds = Point(Boundary.w, Boundary.h)
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
  var waiting4Bricks = Map(0 -> 0, 1 -> 0)
  var renderId = 0

  var textareaValue = ""

  val display = Var(false)
  var oneMoreList = Var(List.empty[Byte])
  var ifVictory = Var(false)
  private var logicFrameTime = System.currentTimeMillis()

  val watchKeys = Set(
    KeyCode.Space,
    KeyCode.Left,
    KeyCode.Right
  )

  private[this] lazy val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] lazy val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val rankCanvas = dom.document.getElementById("RankView").asInstanceOf[Canvas] //排行榜canvas
  private[this] lazy val rankCtx = rankCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val brickCanvas = dom.document.getElementById("BrickView").asInstanceOf[Canvas] //排行榜canvas
  private[this] val borderCanvas = dom.document.getElementById("BorderView").asInstanceOf[Canvas] //排行榜canvas
  private[this] lazy val borderCtx = borderCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val addBall = dom.document.getElementById("add_ball").asInstanceOf[HTMLAudioElement]

  private val border = dom.document.getElementById("border").asInstanceOf[Image]

  private[this] val webSocketClient: WebSocketClient = new WebSocketClient(connectOpenSuccess, connectError, messageHandler, connectClose)

  private def connectOpenSuccess(event0: Event) = {
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
          println(s"keyUp: ${e.keyCode}")
          val key = e.keyCode match {
            case KeyCode.Left => 1
            case KeyCode.Right => 2
            case _ => -1
          }
          if (lastKey == key) {
            val msg: Protocol.UserAction = Protocol.KeyUp(grid.frameCount.toInt)
            webSocketClient.sendMessage(msg)
            lastKey = 0
            e.preventDefault()
          }
        }
      }
    }
    event0
  }

  private def connectError(e: Event) = {
    e
  }

  private def connectClose(e: Event, s: Boolean) = {
    dom.window.location.href=s"http://10.1.29.250:47010/breakout"
    e
  }

  def gameRender(): Double => Unit = { _ =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    draw(offsetTime)
    if (isContinue)
      renderId = dom.window.requestAnimationFrame(gameRender())
  }

  def init(): Unit = {
    println(s"version: 20190226")
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
    resetRankCanvas()
    webSocketClient.setUp(id)
  }

  def run(): Unit = {
    gameLoopId = dom.window.setInterval(() => gameLoop(), Protocol.frameRate)
    dom.window.requestAnimationFrame(gameRender())

    brickCanvas.width = canvasBoundary.x.toInt
    brickCanvas.height = canvasBoundary.y.toInt

  }


  def gameLoop(): Unit = {
    logicFrameTime = System.currentTimeMillis()
    if (webSocketClient.getWsState) {
      if (syncFrame.nonEmpty) {
        val frontend = grid.frameCount
        val backend = syncFrame.get
        val advancedFrame = backend - frontend
        if (advancedFrame == 1) {
          update()
        } else if (advancedFrame < 0 && grid.historyStateMap.get(backend).nonEmpty) {
          println(s"frontend advanced backend,frontend$frontend,backend:$backend")
          grid.setGridInGivenFrame(backend)
          if (grid.deadList.nonEmpty && grid.snakes.get(grid.deadList.head).nonEmpty){
            DrawElements.init(canvasUnit, grid.snakes.filterNot(s => grid.deadList.contains(s._1)).values.toList, grid.bricks.filterNot(b => grid.snakes(grid.deadList.head).color == b._2._1).toList)
          } else {
            DrawElements.init(canvasUnit, grid.snakes.values.toList, grid.bricks.toList)
          }
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
  }

  def update(): Unit = {
    grid.historyStateMap += grid.frameCount.toInt -> (grid.bricks, grid.balls, grid.snakes, grid.lastAction)
    val deadUsers = grid.update()
    val totalScore = 500
    val waitingBricksSnakes = grid.snakes.filter(s => s._2.score % totalScore == 0 && !grid.bricks.exists(_._2._1 == s._2.color))
    waitingBricksSnakes.foreach {s =>
      waiting4Bricks = waiting4Bricks ++ Map(s._2.color.toInt -> (waiting4Bricks(s._2.color.toInt) + 1))
    }

    if (waiting4Bricks.exists(_._2 >= 5)) {
      println(s"==========未接收到新砖块，请求同步！")
      waiting4Bricks.filter(_._2 >= 5).foreach {s =>
        waiting4Bricks = waiting4Bricks ++ Map(s._1 -> 0)
      }
      val msg: Protocol.UserAction = Protocol.NeedToSync
      webSocketClient.sendMessage(msg)
    }
    val filterDead = deadUsers.filter(u => u.life == 0 && !grid.balls.exists(_._2.bId == u.bId))
    if (filterDead.nonEmpty) grid.deadList = grid.deadList ::: filterDead.map(_.bId)
    filterDead.foreach { s =>
      grid.lastAction += s.bId -> 0
      rankCtx.fillStyle = Color.Black.toString()
      rankCtx.globalAlpha = 0.4
      rankCtx.fillRect(s.color * Boundary.start2 * canvasUnit, 0, 50 * canvasUnit, 60 * canvasUnit)
      rankCtx.globalAlpha = 1
    }

    if (newBallMap.contains(grid.frameCount.toInt)) {
      dealWithNewBalls()
    }
    if (newBricksMap.contains(grid.frameCount.toInt)) {
      dealWithNewBricks()
    }

    val limitFrameCount = grid.frameCount - (grid.maxDelayed + 1)
    grid.actionMap = grid.actionMap.filterKeys(_ >= limitFrameCount)
    grid.historyStateMap = grid.historyStateMap.filter(_._1 > limitFrameCount)
    newBricksMap = newBricksMap.filter(_._1 > limitFrameCount)
    newBallMap = newBallMap.filter(_._1 > limitFrameCount)
  }

  def dealWithNewBalls(): Unit = {
    val newBalls = newBallMap(grid.frameCount.toInt)
    grid.balls ++= newBalls.toMap
    if (newBalls.exists(_._2.bId == myId)) addBall.play()
    val allBalls = newBalls.map(_._2.bId)
    val filterBalls = allBalls.distinct
    val countOfBalls = filterBalls.map(b => (b, allBalls.count(_ == b)))
    countOfBalls.foreach {
      case (bId, count) =>
        val newLife = grid.snakes(bId).life - count
        grid.snakes += bId -> grid.snakes(bId).copy(life = newLife.toByte)
        drawLife(grid.snakes(bId).color, newLife)
      case _ =>
    }
  }

  def dealWithNewBricks(): Unit = {
    val bricks = newBricksMap(grid.frameCount.toInt).toMap
    grid.bricks ++= bricks
    if (bricks.contains((numEveryRow * rowNum).toByte)) {
      dealWithNewBricksOfSomeone(1, bricks.toList)
    } else {
      dealWithNewBricksOfSomeone(0, bricks.toList)
    }
  }

  def dealWithNewBricksOfSomeone(seat: Int, bricks: List[(Byte, (Byte, Byte))]): Unit = {
    val snakeF = grid.snakes.find(_._2.color == seat)
    if (snakeF.nonEmpty) {
      grid.snakes += snakeF.head._1 -> snakeF.head._2.copy(level = snakeF.head._2.level + 1, off = 0)
      drawLevel(snakeF.head._2.color, snakeF.head._2.level + 1)
      drawBricks(seat, grid.snakes.values.toList, bricks)
      waiting4Bricks = waiting4Bricks ++ Map(seat -> 0)
    }
  }

  def dealWithLoseMap(bId: Byte): Unit = {
    drawDefeat(firstCome, ctx, myId, grid.getBall4Draw, bId, grid.snakes)
    ifVictory := true
    dom.window.clearInterval(gameLoopId)
    isContinue = false
    dom.window.clearInterval(renderId)
  }

  def draw(offsetTime: Long): Unit = {
    if (webSocketClient.getWsState) {
      if (loseMap.get(grid.frameCount.toInt).nonEmpty) {
        dealWithLoseMap(loseMap(grid.frameCount.toInt))
      } else {
        val data = grid.getBall4Draw
        drawGrid(ctx, firstCome, myId, data, offsetTime)
      }
    } else {
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

      case Protocol.SyncFrame(frame) =>
        syncFrame = Some(frame)

      case msg: Protocol.TotalData =>
        println(s"======================recv total data")
        grid.resetTotalData(msg)
        if (grid.deadList.nonEmpty && grid.snakes.get(grid.deadList.head).nonEmpty){
          DrawElements.init(canvasUnit, grid.snakes.filterNot(s => grid.deadList.contains(s._1)).values.toList, grid.bricks.filterNot(b => grid.snakes(grid.deadList.head).color == b._2._1).toList)
        } else {
          DrawElements.init(canvasUnit, grid.snakes.values.toList, grid.bricks.toList)
        }
        waiting4Bricks = Map(0 -> 0, 1 -> 0)
        if (firstCome) {
          firstCome = false
          run()
          DrawElements.init(canvasUnit, msg.snakes.map(_._2), msg.bricks) //test
        }

      case Protocol.UserDead(frame, users) =>
        println(s"!!!!!!!!!!!!!!!usersDead:$users")

      case Protocol.UserLeft(bId) =>
        if (grid.snakes.get(bId).nonEmpty) {
          val seat = grid.snakes(bId).color
          drawLeft(firstCome, ctx, seat)
          grid.snakes -= bId
          grid.bricks = grid.bricks.filterNot(_._2._1 == seat)
          grid.balls = grid.balls.filterNot(_._2.bId == bId)

        }

      case Protocol.NewSnake(snake) =>
        println(s"new snake::::$snake")
        grid.snakes += snake.bId -> snake

      case Protocol.CloseWs =>
        dom.window.location.href=s"http://${Main.host}:47010/breakout#/Home"

      case Protocol.InitBricks(frame, bricks) =>
        newBricksMap += frame -> bricks
        if (grid.frameCount == frame) {
          dealWithNewBricks()
        }
        else if (grid.frameCount > frame) {
          syncFrame = Some(frame.toInt)
        }

      case Protocol.NewBalls(frame, balls) =>
        newBallMap += frame -> balls
        if (grid.frameCount == frame) {
          dealWithNewBalls()
        }
        else if (grid.frameCount > frame) {
          syncFrame = Some(frame.toInt)
        }

      case a@Protocol.SnakeAction(bId, keyCode, frame) =>
        if (frame < grid.frameCount.toInt) {
          println(s"!!! got snake action=$a whem i am in frame=${grid.frameCount}")
          syncFrame = Some(frame)
        }
        grid.addActionWithFrame(grid.snakes(bId).id, keyCode, frame.toLong)

      case data: Protocol.Text =>
        textList.update(t => t :+ data)
        dom.document.getElementById("text_list").scrollTop = dom.document.getElementById("text_list").scrollHeight

      case data@Protocol.SomeOneLose(frame, id, score) =>
        println(s"recv data:::==========================:$data; front: ${grid.snakes.map(s => (s._2.color, s._2.score))}")
        if (frame <= grid.frameCount.toInt) {
          dealWithLoseMap(id)
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
        waiting4Bricks = Map(0 -> 0, 1 -> 0)
        renderId = 0
        logicFrameTime = System.currentTimeMillis()
        resetRankCanvas()

      case _ =>

    }
  }

  def resetRankCanvas(): Unit = {
    rankCanvas.width = canvasBoundary.x.toInt
    rankCanvas.height = canvasBoundary.y.toInt
    rankCtx.fillStyle = Color.White.toString()
    rankCtx.fillRect(50 * canvasUnit, 0, 30 * canvasUnit, Boundary.h * canvasUnit)
    rankCtx.beginPath()
    rankCtx.lineWidth = 3
    rankCtx.strokeStyle = Color.Black.toString()
    rankCtx.moveTo(50 * canvasUnit, 0)
    rankCtx.lineTo(80 * canvasUnit, 0)
    rankCtx.stroke()
    rankCtx.closePath()
    rankCtx.beginPath()
    rankCtx.moveTo(50 * canvasUnit, Boundary.h * canvasUnit)
    rankCtx.lineTo(80 * canvasUnit, Boundary.h * canvasUnit)
    rankCtx.stroke()
    rankCtx.closePath()
  }

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
        <p style="color: red;">请在15秒内点击哦..等待超过15秒, 房间将关闭..</p>
      </div>
    case (true, list) if list.lengthCompare(1) == 0 && list.contains(myId)=>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" disbaled ="true">再来一局</button>
        <p>您已请求再来一局, 等待对方玩家请求..等待超过15秒, 房间将关闭..</p>
      </div>

    case (true, list) if list.lengthCompare(1) == 0 && !list.contains(myId) =>
      <div style ={s"position: absolute; margin-left: 25px; margin-top: 55px; left: ${50 * canvasUnit}px; top: ${61 * canvasUnit}px; text-align: center; width: ${30 * canvasUnit}px;"}>
        <button class="btn btn-primary" onclick={() => reJoin();}>再来一局</button>
        <p style="color: red;">对方玩家已请求再来一局, 等待您的请求..等待超过15秒, 房间将关闭..</p>
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

    <div>
      <button class="btn btn-primary" style={s"position: absolute; left: ${dom.window.innerWidth - 100}px; top: 10px; width: 85px;"} onclick={() => dom.window.location.href = s"http://$host:47010/breakout"}>返回大厅</button>
      <div>
      </div>
      <div>
        {detail}
      </div>
      <div id ="text_list" style={s"z-index: 1000; position: absolute; top:${top}px; left: ${left - 6 * canvasUnit}px; width: ${width + 8 * canvasUnit}px; height: ${height}px; overflow-y: scroll; overflow-x: hidden;margin-top: 50px; margin-left: 25px;"}>
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
