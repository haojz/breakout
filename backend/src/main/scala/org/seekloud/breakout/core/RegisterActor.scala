package org.seekloud.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.seekloud.breakout.Protocol.frameRate
import org.seekloud.breakout.client.GridOnServer
import org.seekloud.breakout.models.UserInfo
import org.seekloud.breakout.{Point, Protocol}
import org.slf4j.LoggerFactory
//import org.seekloud.breakout.models.dao.UserDao
import org.seekloud.breakout.models.UserInfoRepo._
import org.seekloud.breakout.models.SlickTables
import org.seekloud.breakout.ptcl._
import scala.util.{Failure, Success}
import org.seekloud.breakout.ptcl.UserProtocol._
import org.seekloud.breakout.Boot.executor

/**
  * Created by haoshuhan on 2019/2/12.
  */
object RegisterActor {
  trait Command

  case class Register(name: String, pwd: String, gender: Int, replyTo: ActorRef[CommonRsp]) extends Command

  case class Login(name: String, pwd: String, replyTo: ActorRef[CommonRsp]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"Register Actor start...")
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers[Command] {
        implicit timer =>
          idle()
      }
    }
  }

  def idle(): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Register(name, pwd, gender, replyTo) =>
          log.info(s"==============recv msg")
          checkUserName(name).map {
            case Some(_) =>
              replyTo ! nameError
            case None =>
              addUser(UserInfo(0L, name, pwd, "", gender, false)).onComplete {
                case Success(_) =>
                  replyTo ! SuccessRsp()
                case Failure(_) =>
                  replyTo ! dbError
              }
          }.recover{ case e: Exception =>
            log.error(s"register:::$e")
            replyTo ! dbError
          }
          Behaviors.stopped

        case Login(name, pwd, replyTo) =>
          checkUserName(name).map {
            case Some(u) =>
              if (u.password == pwd)
                if (!u.forbidden) {
                  replyTo ! LoginRsp(UserDetailInfo("user"+ u.id.toString, u.username, u.headImg, u.gender))
                } else {
                  replyTo ! forbidError
                }
              else replyTo ! pwdError

            case None =>
              replyTo ! userError
          }

          Behaviors.stopped //todo 成功stop；失败定时stop
      }
    }
  }

}
