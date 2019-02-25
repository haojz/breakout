package org.seekloud.breakout.http

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive1, RequestContext}
import akka.http.scaladsl.server.directives.BasicDirectives
import org.seekloud.breakout.common.AppSettings
import org.seekloud.breakout.ptcl.ErrorRsp
import org.seekloud.breakout.ptcl.UserProtocol._
import org.seekloud.breakout.utils.CirceSupport
import org.seekloud.breakout.utils.SessionSupport
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
/**
  * Created by haoshuhan on 2018/4/25.
  */
object SessionBase extends CirceSupport{

  val SessionTypeKey = "STKey"
  object SessionKeys {
    val sessionType = "breakout_session"
    //    val userType = "jelly_userType"
    val userId = "breakout_userId"
//    val bbsId = "breakout_bbsId"
    val cookieStore = "breakout_cookieStore"
    val name = "breakout_name"
    //    val shortName = "jelly_shortName"
    //    val mobile = "jelly_mobile"
    //    val sex = "jelly_sex"
    val timestamp = "breakout_timestamp"
  }

  val log = LoggerFactory.getLogger(this.getClass)

  case class BreakoutSession(
                            userInfo: UserBaseInfo,
                            time: Long
                          ) {
    def toSessionMap: Map[String, String] = {
      Map(
        SessionTypeKey -> SessionKeys.sessionType,
        //        SessionKeys.userType -> userInfo.userType,
        SessionKeys.userId -> userInfo.id,
//        SessionKeys.bbsId -> userInfo.bbsId,
        //        SessionKeys.cookieStore -> userInfo.cookieStore,
        SessionKeys.name -> userInfo.name,
        //        SessionKeys.shortName -> userInfo.shortName,
        //        SessionKeys.mobile -> userInfo.mobile,
        //        SessionKeys.sex -> userInfo.sex.toString,
        SessionKeys.timestamp -> time.toString
      )
    }
  }

}

trait SessionBase extends SessionSupport{

  import SessionBase._

  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig
  private val timeout = 24 * 60 * 60 * 1000
  private val log = LoggerFactory.getLogger(this.getClass)

  implicit class SessionTransformer(sessionMap: Map[String, String]) {
    def toBreakoutSession:Option[BreakoutSession] = {
      //      log.debug(s"toAdminSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionTypeKey).exists(_.equals(SessionKeys.sessionType))) {
          if(sessionMap(SessionKeys.timestamp).toLong - System.currentTimeMillis() > timeout){
            None
          }else {
            Some(BreakoutSession(
              UserBaseInfo(
                //                sessionMap(SessionKeys.userType),
                sessionMap(SessionKeys.userId),
                sessionMap(SessionKeys.name)
                //                sessionMap(SessionKeys.cookieStore)
                //                sessionMap(SessionKeys.shortName),
                //                sessionMap(SessionKeys.mobile),
                //                sessionMap(SessionKeys.sex).toShort
              ),
              sessionMap(SessionKeys.timestamp).toLong
            ))
          }
        } else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toAdminSession: ${e.getMessage}")
          None
      }
    }
  }
  protected val optionalBreakoutSession: Directive1[Option[BreakoutSession]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toBreakoutSession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }

  private def loggingAction: Directive[Tuple1[RequestContext]] = extractRequestContext.map { ctx =>
    log.info(s"Access uri: ${ctx.request.uri} from ip ${ctx.request.uri.authority.host.address}.")
    ctx
  }

  def noSessionError(message:String = "no session") = ErrorRsp(1000102,s"$message")

  //管理员
  //  def adminAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
  //    optionalJellySession {
  //      case Some(session) =>
  //        if(session.userInfo.userType == UserRolesType.devManager){
  //          f(session.userInfo)
  //        } else{
  //          complete(noSessionError("you don't have right."))
  //        }
  //
  //      case None =>
  //        complete(noSessionError())
  //    }
  //  }
  //
  //  //厂商
  //  def manufacturerAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
  //    optionalJellySession {
  //      case Some(session) =>
  //        if(session.userInfo.userType == UserRolesType.devMANUFACTURERS){
  //          f(session.userInfo)
  //        } else{
  //          complete(noSessionError("you don't have right."))
  //        }
  //
  //      case None =>
  //        complete(noSessionError())
  //    }
  //  }
  //
  //  //会员
  //  def memberAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
  //    optionalJellySession {
  //      case Some(session) =>
  //        if(session.userInfo.userType == UserRolesType.comMember){
  //          f(session.userInfo)
  //        } else{
  //          complete(noSessionError("you don't have right."))
  //        }
  //
  //      case None =>
  //        complete(noSessionError())
  //    }
  //  }
  //
  //  //厂商或管理员
  //  def managersAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
  //    optionalJellySession {
  //      case Some(session) =>
  //        if(session.userInfo.userType == UserRolesType.devManager || session.userInfo.userType == UserRolesType.devMANUFACTURERS){
  //          f(session.userInfo)
  //        } else{
  //          complete(noSessionError("you don't have right."))
  //        }
  //
  //      case None =>
  //        complete(noSessionError())
  //    }
  //  }
  //
  //  //厂商和会员
  //  def customerAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
  //    optionalJellySession {
  //      case Some(session) =>
  //        if(session.userInfo.userType == UserRolesType.devMANUFACTURERS || session.userInfo.userType == UserRolesType.comMember){
  //          f(session.userInfo)
  //        } else{
  //          complete(noSessionError("you don't have right."))
  //        }
  //
  //      case None =>
  //        complete(noSessionError())
  //    }
  //  }

  def parseBreakoutSession(f: UserBaseInfo => server.Route) = loggingAction { ctx =>
    optionalBreakoutSession {
      case Some(session) =>
        f(session.userInfo)

      case None =>
        complete(noSessionError())
    }
  }


}

