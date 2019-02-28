package org.seekloud.breakout.components

import mhtml._
import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.scalajs.dom.window
import org.seekloud.breakout.{Main, Routes}
import org.seekloud.breakout.ptcl.SuccessRsp
import org.seekloud.breakout.utils._
import io.circe.generic.auto._
import scala.util.Random
import scala.xml.Elem
import scala.concurrent.ExecutionContext.Implicits.global
import org.seekloud.breakout.style.HomeStyle._
import io.circe.generic.auto._
import io.circe.syntax._
/**
  * Created by haoshuhan on 2019/2/12.
  */
object Header extends Page{

  val home = "Home"
  case class UserInfo(userType: Int, userId: String, userName: String, userImg: String)
  val loginS: Option[UserInfo] = None
  val manageDisplay = Var(false)

  val loginState = Var(loginS)
  val userInfo = Var("")

  val manageIcon = manageDisplay.map {
    case false =>
      <img class="header-logout"  style="height: 21px; padding-top: 15px; padding-left: 5px;" src="/breakout/static/img/管理下.png" onclick={()=>manageDisplay:=true}></img>
    case true =>
      <img class="header-logout" style="height: 21px; padding-top: 15px; padding-left: 5px;" src="/breakout/static/img/管理上.png" onclick={()=>manageDisplay:=false}></img>
  }
  val loginStateRx = loginState.map {
    case None => emptyHTML
    case Some(userInfo) =>
      userInfo.userType match {
        case 0 => //游客
          <div style="width: 100%; margin: 7px;">
            <div style="padding: 0px 17px; font-family:
      PingFangSC-Regular; font-size: 16px; color: #ffffff; letter-spacing: -0.23px;
      cursor: pointer;  right: 3%; display: inline-block; float: right;"
                 onclick={()=>window.location.hash="#/Register"}>注册</div>
            <div style="padding: 0px 17px; font-family:
      PingFangSC-Regular; font-size: 16px; color: #ffffff; letter-spacing: -0.23px;
      cursor: pointer;  right: 3%; display: inline-block; float: right;"
                 onclick={()=>window.location.hash="#/Login"}>登录</div>
            <div style="display: inline-block;float:right; color: #ffffff;">Welcome，游客({userInfo.userName})</div>
          </div>
        case 1 => //用户
          <div style ="PingFangSC-Regular; font-size: 16px; position: absolute;
          right: 2%; display: inline-flex; color: #333; letter-spacing: -0.23px; ">
            <img style="height: 30px; margin-top: 8px;" src="/breakout/static/img/user.png"></img>
            <p style="display: inline-block; margin-top: 15px;font-size: 21px; color: #ffffff;">{userInfo.userName}</p>
            {manageIcon}
            {manageUlRx}
          </div>

        case _ => emptyHTML

      }
  }

  val manageUlRx = manageDisplay.map {
    case false =>
      emptyHTML
    case true =>
      <div style="width: 0px;">
        <ul class={manageUl.htmlClass}>
          {if (dom.window.localStorage.getItem("userName") == "admin") {
          <li class="moreManage" onclick={()=>dom.window.location.hash = "#/Admin"}>
            管理
          </li>
        } else emptyHTML
          }
          <li class="moreManage" onclick={()=>logout(); manageDisplay:=false}>
            退出
          </li>
        </ul>
      </div>
  }

  def logout() : Unit = {
    Http.getAndParse[SuccessRsp](Routes.logout).map {
      case Right(msg) =>
        if (msg.errCode ==0) {
          JsFunc.alert("退出成功")
          removeUserInfo()
          initGuest()
        }
      case Left(msg) =>

    }
  }

  def initGuest(): Unit = {
    if (dom.window.localStorage.getItem("userId") != null ) {
      val userId = dom.window.localStorage.getItem("userId")
      val userName = dom.window.localStorage.getItem("userName")
      loginState := Some(UserInfo(1, userId, userName, ""))
      println(s"1===============================")
    } else if (dom.window.localStorage.getItem("guestId") != null) {
      val guestId = dom.window.localStorage.getItem("guestId")
      val guestName = dom.window.localStorage.getItem("guestName")
      loginState := Some(UserInfo(0, guestId, guestName, ""))
      println(s"2===============================")
    } else if (dom.window.localStorage.getItem("userId") == null && dom.window.localStorage.getItem("guestId") == null) {
      val guestId = "guest" + System.currentTimeMillis()
      val guestName = Main.guestName(scala.util.Random.nextInt(Main.guestName.length))
      loginState := Some(UserInfo(0, guestId, guestName, ""))
      dom.window.localStorage.setItem("guestId", guestId)
      dom.window.localStorage.setItem("guestName", guestName)
      println(s"3===============================")
    } else {
      println(s"4===============================")

    }

  }

  def removeUserInfo(): Unit = {
    dom.window.localStorage.removeItem("headImg")
    dom.window.localStorage.removeItem("userName")
    dom.window.localStorage.removeItem("userId")
  }

  def removeGuestInfo(): Unit = {
    dom.window.localStorage.removeItem("guestName")
    dom.window.localStorage.removeItem("guestId")
  }


  def render: Elem = {
    initGuest()
    <div style ="height: 6%; width: 100%; background-color: #4b4646;">
    <div style ="width: 100%; display: inline-flex;">
      <div style="font-size: 30px; padding: 5px; padding-left: 46px;position: relative; font-weight: normal; color:#fff;">Breakout</div>
      {loginStateRx}
    </div>
    </div>
  }
}