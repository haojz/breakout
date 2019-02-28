package org.seekloud.breakout.pages

import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.seekloud.breakout.utils._
import org.seekloud.breakout.Routes
import org.seekloud.breakout.ptcl._
import org.seekloud.breakout.ptcl.UserProtocol._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.raw.KeyboardEvent
import org.seekloud.breakout.components.Header
import org.seekloud.breakout.components.Header.UserInfo
import org.seekloud.breakout.utils.Http.postJson
//import org.seekloud.breakout.components.Header
import org.seekloud.breakout.style.LoginPageStyle._
import scala.xml.Elem
import io.circe.parser.decode
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by haoshuhan on 2019/2/13.
  */
object Login extends Page {
  def login(): Unit = {
    val username = dom.window.document.getElementById("username").asInstanceOf[Input].value
    val password = dom.window.document.getElementById("password").asInstanceOf[Input].value
    val data = LoginReq(username, password).asJson.noSpaces
    postJson(Routes.login, data, true).map { s =>
      decode[SuccessRsp](s).map { rsp =>
        if (rsp.errCode == 10003) {
          JsFunc.alert(rsp.msg)
        } else if (rsp.errCode == 1004) {
          JsFunc.alert(rsp.msg)
        } else if (rsp.errCode == 0) {
          JsFunc.alert("登录成功！")
          decode[LoginRsp](s).map { r =>
            dom.window.localStorage.setItem("headImg", r.userInfo.headImg)
            dom.window.localStorage.setItem("userName", r.userInfo.name)
            dom.window.localStorage.setItem("userId", r.userInfo.id)
            Header.removeGuestInfo()
            Header.loginState := Some(UserInfo(1, r.userInfo.id, r.userInfo.name, r.userInfo.headImg))
            println(s"hahahahahhhh")
            dom.window.location.hash = "#/Home"
          }
        } else {
          JsFunc.alert(rsp.msg)
        }
      }
    }
  }

  def render: Elem = {
    <div class={bg.htmlClass}>
      <div class={loginBox.htmlClass}>
        <div class="login" onkeydown={e: KeyboardEvent => if (e.keyCode == 13) login()}>
          <div class="login_logo">
            <a href="#">
              <img src="/breakout/static/img/login_logo.png"/>
            </a>
          </div>
          <div class="login_name">
            <p>Welcome to BREAKOUT!</p>
          </div>
          <input name="username" type="text" id="username" placeholder="用户名"></input>
          <input name="password" type="password" id="password" placeholder="密码"></input>
          <button class="login_button" style="width: 100%; height: 50px;" type="submit" onclick={() => login()}>登录</button>
        </div>
      </div>
    </div>


  }
}
