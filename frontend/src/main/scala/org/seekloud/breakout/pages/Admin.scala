package org.seekloud.breakout.pages

import com.sun.net.httpserver.Authenticator.Success
import org.seekloud.breakout.Routes
import org.seekloud.breakout.utils.{Http, JsFunc, Page}
import org.seekloud.breakout.ptcl.{AdminProtocol, SuccessRsp}
import mhtml._

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto._
import org.scalajs.dom

import scala.xml.Elem

/**
  * Created by haoshuhan on 2019/2/21.
  */
object Admin extends Page{
  val users = Var(List.empty[AdminProtocol.User])

  def getAllUsers(): Unit = {
    Http.getAndParse[AdminProtocol.AllUsersRsp](Routes.getAllUsers).map{
      case Right(rsp) =>
        users := rsp.users

      case Left(error) =>
        JsFunc.alert(s"您没有权限或者您未登录！")
        dom.window.location.hash = "#/Home"

    }
  }

  def forbiddenUser(id: String, forbidden: Boolean): Unit = {
    val forbid = forbidden match {
      case false => 1
      case true => 0
    }
    Http.getAndParse[SuccessRsp](Routes.forbidden(id, forbid)).map{
      case Right(rsp) =>
        if (rsp.errCode == 0) {
          forbid match {
            case 1 => JsFunc.alert("禁用成功！")
            case 0 => JsFunc.alert("取消禁用成功！")
          }
          getAllUsers()
        }
        else JsFunc.alert(rsp.msg)

      case Left(error) =>
        JsFunc.alert(s"您没有权限或者您未登录！")
        dom.window.location.hash = "#/Home"

    }
  }
  def getForbbidenButton(id: String, forbidden: Boolean) =
    <button id ={s"{forbidden_$id"} class="btn btn-primary"
            onclick = {() => forbiddenUser(id, forbidden)}>{if(forbidden) "取消禁用" else "禁用"}</button>

  val usersRx = users.map {users =>
    <div style ="position: absolute;left: 30%;width: 40%; top: 170px;">
      <table class="table" style ="background-color: #efeeee;">
        <tr>
          <th style="text-align: center;">id</th>
          <th style="text-align: center;">name</th>
          <th style="text-align: center;">操作</th>
        </tr>
        {
        users.sortBy(_.userId).map{ r =>
          <tr>
            <td>{r.userId}</td>
            <td>{r.userName}</td>
            <td>{getForbbidenButton(r.userId, r.forbidden)}</td>
          </tr>
        }
        }
      </table>
    </div>

  }

  override def render: Elem = {
    getAllUsers()
    <div id="admin">
      <div>
        <button class="btn btn-primary" style ="float: left;" onclick={()=> dom.window.location.hash = "#/Home"}>返回</button>
        <div style="font-size: 30px; text-align: center; top: 100px; position: absolute; width: 100%; margin-left: 50px;">用户信息</div>
        {usersRx}
      </div>
    </div>
  }
}
