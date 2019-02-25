package org.seekloud.breakout.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.breakout.ptcl.UserProtocol._
import org.seekloud.breakout.ptcl._

import scala.concurrent.Future
import org.seekloud.breakout.Boot.system
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.breakout.core.{RegisterActor, RoomManager}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.seekloud.breakout.utils.CirceSupport
import org.seekloud.breakout.Boot.timeout
import org.seekloud.breakout.Boot.executor
import org.seekloud.breakout.Boot.scheduler
import org.seekloud.breakout.http.SessionBase.BreakoutSession
import org.seekloud.breakout.Boot.roomManager
import org.seekloud.breakout.Boot.materializer
import org.seekloud.breakout.ptcl.RoomProtocol._
import org.seekloud.breakout.ptcl.UserProtocol._

import scala.language.postfixOps
//import org.seekloud.breakout.models.dao.AdminDao._
import org.seekloud.breakout.models.UserInfoRepo._
import org.seekloud.breakout.ptcl.AdminProtocol._
/**
  * Created by haoshuhan on 2019/2/12.
  */
trait UserService extends ServiceUtils with CirceSupport with SessionBase {

  private val getRoomState: Route = (path("getRoomState") & get) {
    val roomStateF: Future[RoomStateRsp] = roomManager ? RoomManager.GetRoomState
    dealFutureResult {
      roomStateF.map { rst =>
        complete(rst)
        //            case _ => complete(ErrorRsp(100002, "error"))
      }
    }
  }

  private val register: Route = (path("register") & post) {
    entity(as[Either[Error, RegisterReq]]) {
      case Right(data) =>
        val registerActor = system.spawn(RegisterActor.create(), "RegisterActor_" + System.currentTimeMillis())
        val registerFuture: Future[CommonRsp] = registerActor ?
          (RegisterActor.Register(data.name, data.pwd, data.gender, _))
        dealFutureResult {
          registerFuture.map {
            case rst: SuccessRsp => complete(rst)
            case rst: ErrorRsp => complete(rst)
            case _ => complete(ErrorRsp(-1, ""))
          }
        }

      case Left(error) =>
        complete(s"data parse error: $error")
    }
  }

  private val login: Route = (path("login") & post) {
    entity(as[Either[Error, LoginReq]]) {
      case Right(data) =>
        val registerActor = system.spawn(RegisterActor.create(), "RegisterActor_" + System.currentTimeMillis())
        val loginF: Future[CommonRsp] = registerActor ?
          (RegisterActor.Login(data.name, data.pwd, _))
        dealFutureResult(
          loginF.map {
            case rst: LoginRsp =>
              val session = BreakoutSession(UserBaseInfo(rst.userInfo.id, rst.userInfo.name), System.currentTimeMillis())
              setSession(session.toSessionMap) { r =>
                r.complete(rst)
              }
            //              complete(rst)
            case rst: ErrorRsp => complete(rst)
            case _ => complete(ErrorRsp(-1, ""))
          }
        )
      case Left(error) =>
        complete(s"data parse error: $error")
    }
  }

  private val logout: Route = path("logout") {
    optionalBreakoutSession {
      case Some(session) =>
        invalidateSession {
          roomManager ! RoomManager.Logout(session.userInfo.id)
          redirect("http://localhost:40110/breakout#/Home", StatusCodes.SeeOther)
        }
        complete(SuccessRsp())

      case None =>
        complete(SuccessRsp())
    }
  }


  private val getAll: Route = path("getAllUsers") {
    optionalBreakoutSession {
      case Some(session) =>
        if (session.userInfo.name == "admin") {
          dealFutureResult{
            getAllUsers().map {list =>
              val users = list.map(l => AdminProtocol.User("user"+l.id, l.username, l.forbidden)).toList.filterNot(_.userName == "admin")
              complete(AllUsersRsp(users))
            }
          }

        } else complete(ErrorRsp(13001, "您没有管理员权限！"))


      case None =>
        complete(ErrorRsp(13002, "请登录！（若无管理员权限，无法查看）"))
    }
  }

  private val forbidden: Route = path("forbidden") {
    parameter(
      'id.as[String],
      'forbid.as[Int]
    ) { (id, forbidden) =>
      optionalBreakoutSession {
        case Some(session) =>
          if (session.userInfo.name == "admin") {
            dealFutureResult{
              forbidUser(id, forbidden).map {l =>
                complete(SuccessRsp())
              }
            }
          } else complete(ErrorRsp(13001, "您没有管理员权限！"))

        case None =>
          complete(ErrorRsp(13002, "请登录！（若无管理员权限，无法查看）"))
      }
    }

  }

  val userRoutes: Route = pathPrefix("user") {
    register ~ login ~ logout ~ getRoomState ~ getAll ~ forbidden
  }
}
