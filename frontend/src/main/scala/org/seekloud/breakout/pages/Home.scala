package org.seekloud.breakout.pages

import org.seekloud.breakout.utils._
import mhtml._
import org.seekloud.breakout.{Main, Routes}
import org.seekloud.breakout.ptcl.RoomProtocol._
import org.seekloud.breakout.ptcl.UserProtocol._
import org.seekloud.breakout.ptcl._
import io.circe.{Decoder, Error}
import org.scalajs.dom
import org.seekloud.breakout.style.HomeStyle._
import io.circe.generic.auto._
import org.scalajs.dom.html.{Document, Input}
import org.seekloud.breakout.components.Header
import org.scalajs.dom.raw._

import scala.util.Random
import scalatags.JsDom.short.s
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import org.seekloud.breakout.Main.host

/**
  * Created by haoshuhan on 2019/2/12.
  */
object Home extends Page {
  override val locationHashString: String = "#/Home"

  private var stateLoopId = -1

  var joinRoomId = -1

  var myId = ""

  val roomStateInit = (1001 to 1020).map { i =>
    (i, None.asInstanceOf[Option[RoomState]])
  }.toMap
  val roomState = Var(roomStateInit)

  def getRoomState(): Unit = {
//    val data = RoomStateReq(dom.window.localStorage.getItem("userId")).asJson.noSpaces
    Http.getAndParse[RoomStateRsp](Routes.getRoomState).map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          val newState = roomStateInit ++ rsp.roomState.map(r => r.roomId -> r.roomState).toMap
          roomState := newState
          if (joinRoomId != -1 && newState.get(joinRoomId).nonEmpty &&
            newState(joinRoomId).nonEmpty && newState(joinRoomId).get.users.toList.length == 2) {
            val name1 = newState(joinRoomId).get.users.filter(_._2.seat == 0.toByte).head._2.info.name
            dom.window.localStorage.setItem("name1", name1)
//            DataStore.name1 = name
//            println(s"name:::::::$name")
            val name2 = newState(joinRoomId).get.users.filter(_._2.seat == 1.toByte).head._2.info.name
            dom.window.localStorage.setItem("name2", name2)
//            println(s"${Main.name2}")
            joinGame()
            //加入游戏
          }
//          roomState.map { state =>
//            val newState = state ++ rsp.roomState.map(r => r.roomId -> r.roomState).toMap
//            roomState := newState
//            println(s"newState:::::$newState")
//          }
        } else {
          JsFunc.alert("get room state error!")
          dom.window.clearInterval(stateLoopId)
        }

      case Left(e) =>
        JsFunc.alert("get room state error!")
        dom.window.clearInterval(stateLoopId)

    }
  }

  def joinGame(): Unit = {
    dom.window.location.href=s"http://$host:47010/breakout/game#/Game/$myId"
//    dom.window.location.href=s"http://192.168.1.103:47010/breakout/game#/Game/$myId"
//    dom.window.location.hash=s"#/Game/$myId"

  }


  def joinRoom(seatId: String): Unit = { //!!!!返回类型Unit
    val split = seatId.split("_")
    val room = split(0)
    val seat = split(1).toByte
    val userId = dom.window.localStorage.getItem("userId")
    val userName = dom.window.localStorage.getItem("userName")
    val (id, userType) = if (userId != null) (userId, 1) else (dom.window.localStorage.getItem("guestId"), 0)
    myId = id
    val name = if (userName != null) userName else dom.window.localStorage.getItem("guestName")
//    val name = dom.window.localStorage.getItem("userName")
    if (id == null) {
      Header.initGuest()
      println(s"!!!!error: id 不存在")

    } else {
      Http.getAndParse[SuccessRsp](Routes.Game.joinRoom(userType.toByte, id, name, room, seat)).map {
        case Right(rst) =>
          if (rst.errCode == 0) {
            println(s"加入成功！")
            joinRoomId = room.toInt
            getRoomState()
          } else {
            JsFunc.alert(rst.msg)
          }
        case Left(err) =>
          JsFunc.alert("解码失败！")

      }
    }


  }

//  def getRandomName: Unit = {
//    val random = new Random(System.currentTimeMillis())
//    dom.document.getElementById("nickName").asInstanceOf[Input].value = Main.guestName(random.nextInt(Main.guestName.length))
//  }

  val roomRx = roomState.map { states =>
//    println(s"states:::::::::::$states")
    states.toList.sortBy(_._1).map { s =>
//      println(s"3333333===========")
      val img1Id = s"${s._1.toString}_0"
      val img2Id = s"${s._1.toString}_1"
      s._2 match {
        case Some(state) =>
          <div style="display: inline-block; width: 20%; margin: 2.5%; text-align: center;">

            {state.users.find(_._2.seat == 0) match {
            case Some(user) => //todo user.img
              <div class="head_img">
              <img src="/breakout/static/img/head.png" style="width: 50%;margin-bottom: 5%;"></img>
                  {
                if (user._2.info.name.length > 6) {
                <div style="display: inline-flex;">
                {user._2.info.name.take(6)}
                  <br />
                  {user._2.info.name.drop(6)}
                  </div>} else
                <div>{user._2.info.name}</div>}
              </div>


            case None =>
              <img id={img1Id} src="/breakout/static/img/question.png"
                   class="question" onclick={()=>joinRoom(img1Id)}></img>

            }
            }



          <img src="/breakout/static/img/breakout_blue.png" class={breakoutImgStyle.htmlClass}></img>

          {val length = 6
            state.users.find(_._2.seat == 1) match {
            case Some(user) => //todo user.img
              <div class="head_img">
                <img src="/breakout/static/img/head.png" style="width: 50%;margin-bottom: 5%;"></img>
                {if (user._2.info.name.length > length) {
                <div style="display: inline-flex;">
                  {user._2.info.name.take(length)}
                  <br />
                  {user._2.info.name.drop(length)}
                </div>} else <div>{user._2.info.name}</div>}
              </div>



            case None =>
              <img id={img2Id} src="/breakout/static/img/question.png"
                   class="question" onclick={()=>joinRoom(img2Id)}></img>
          }}
          </div>

        case None =>
          //          val img1Id = s"${s._1}_1"
          //          val img2Id = s"${s._1}_2"
          <div style="display: inline-block; width: 20%; margin: 2.5%;text-align: center;">
            <img id={img1Id} src="/breakout/static/img/question.png"
                 class="question" onclick={()=>joinRoom(img1Id)}></img>
            <img src="/breakout/static/img/breakout_grey.png" class={breakoutImgStyle.htmlClass}></img>
            <img id={img2Id} src="/breakout/static/img/question.png"
                 class="question" onclick={()=>joinRoom(img2Id)}></img>

          </div>

      }

    }


  }

  def startStateLoop = {
    getRoomState()
    stateLoopId = dom.window.setInterval(() => getRoomState(), 3000)
  }

  def getWebSocketUri(document: Document, nameOfChatParticipant: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/breakout/netSnake/join?name=$nameOfChatParticipant"
  }

  val a = Var(List.empty[Elem])

  val rx = a.map {a =>

  }

  def getValue(): Unit = {
    a.update(p => p:::List(<img src="/breakout/static/img/biaoqing.png" style="width: 25px;"></img>))
  }
//  <div id = "chat" style="width: 200px; height: 30px; overflow: auto; border: 2px solid #a7a2a2;
//      left: 10px;position: relative; margin: 10px;" contenteditable="true">{a}</div>
//    <button onclick={()=> getValue()}>查看</button>

  override def render: Elem = {
//    println(s"888888")
    startStateLoop
    <div id="home">
      <div>
        {Header.render}
        {roomRx}
      </div>

    </div>
  }
}
