package org.seekloud.breakout.pages

import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.seekloud.breakout.Routes
import org.seekloud.breakout.ptcl.SuccessRsp
import org.seekloud.breakout.ptcl.UserProtocol.RegisterReq
import org.seekloud.breakout.utils._
import org.seekloud.breakout.ptcl.UserProtocol._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalajs.dom.raw.KeyboardEvent
import org.seekloud.breakout.components.Header
import org.seekloud.breakout.components.Header.UserInfo

import scala.xml.Elem
import org.seekloud.breakout.style.RegisterPageStyle._
import org.seekloud.breakout.utils.Http.postJson

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by haoshuhan on 2019/2/12.
  */
object Register extends Page {
  override val locationHashString: String = "#/Register"

  def register(): Unit = {
    val username = dom.window.document.getElementById("username").asInstanceOf[Input].value
    val pwd = dom.window.document.getElementById("password").asInstanceOf[Input].value
    val pwd_rpt = dom.window.document.getElementById("password_repeat").asInstanceOf[Input].value
    if (pwd == pwd_rpt) {
      val data = RegisterReq(username, pwd).asJson.noSpaces
      Http.postJsonAndParse[SuccessRsp](Routes.register, data).map {
        case Right(rsp) =>
          if (rsp.errCode == 10001) {
            JsFunc.alert("db error, please try again！")
          } else if (rsp.errCode == 10002) {
            JsFunc.alert("用户名已存在")
          } else if (rsp.errCode == 0) {
            JsFunc.alert("注册成功！")
            postJson(Routes.login, data, true).map { s =>
              decode[SuccessRsp](s).map { rsp =>
                if (rsp.errCode == 10003) {
                  JsFunc.alert(rsp.msg)
                } else if (rsp.errCode == 1004) {
                  JsFunc.alert(rsp.msg)
                } else if (rsp.errCode == 0) {
                  decode[LoginRsp](s).map { r =>
                    dom.window.localStorage.setItem("headImg", r.userInfo.headImg)
                    dom.window.localStorage.setItem("userName", r.userInfo.name)
                    dom.window.localStorage.setItem("userId", r.userInfo.id)
                    Header.removeGuestInfo()
                    Header.loginState := Some(UserInfo(1, r.userInfo.id, r.userInfo.name, r.userInfo.headImg))
                    dom.window.location.hash = "#/Home"
                  }
                } else {
                  JsFunc.alert("登录请求失败，请稍后再试！")
                }
              }
            }
          } else {
            JsFunc.alert("注册请求失败，请稍后再试！")
          }


        case Left(e) =>

      }
    } else JsFunc.alert("两次密码输入不一致！")

  }

  override def render: Elem = {
    <div class={bg.htmlClass}>
      <div class={registerBox.htmlClass}>
        <div class={rgst.htmlClass} onkeydown={e: KeyboardEvent => if (e.keyCode == 13) register()}>
          <div style="float: left;">
            <p style="margin-left: 0px; font-size: 17px;">Sign up NOW!</p>
          </div>
          <input name="username" type="text" id="username" placeholder="用户名"></input>
          <input name="password" type="password" id="password" placeholder="密码"></input>
          <input name="password_repeat" type="password" id="password_repeat" placeholder="再次输入密码"></input>
          <button class="login_button" style="width: 100%; height: 50px;" type="submit" onclick={() => register()}>注册</button>


        </div>
      </div>
    </div>

  }
}
