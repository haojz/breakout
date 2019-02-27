package org.seekloud.breakout.core
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.breakout.common.AppSettings
//import org.seekloud.breakout.core.RoomActor.UserDead
import org.seekloud.breakout.{Protocol, WsSourceProtocol}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import org.seekloud.breakout.ptcl.RoomProtocol._
import org.seekloud.breakout.ptcl.UserProtocol._
import org.seekloud.breakout.ptcl.{CommonRsp, ErrorRsp, SuccessRsp}
/**
  * Created by haoshuhan on 2019/2/4.
  */
object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val roomMap = mutable.HashMap[Int, (Option[String], mutable.Map[String, (Byte, String)])]() //roomId->(pwd, Map(id, (breakoutId, name)))

  private val roomState = mutable.HashMap[Int, Option[RoomState]]()

  private val breakoutIdMap = mutable.HashMap[String, Byte]()

  trait Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  case class JoinRoom(userType: Byte, id: String, name: String, room: Int, seat: Byte, replyTo: ActorRef[CommonRsp]) extends Command

  case class JoinGame(id: String, subscriber: ActorRef[WsSourceProtocol.WsMsgSource]) extends Command

  case class GetRoomState(replyTo: ActorRef[RoomStateRsp]) extends Command

  case class Left(id: String) extends Command with RoomActor.Command

  case class UserActionOnServer(id: String, action: Protocol.UserAction) extends Command

  case class CloseRoom(roomId: Int) extends Command

  case class Logout(id: String) extends Command

  private case object UnKnowAction extends Command

  trait UserAction extends Command

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      implicit val breakoutIdGenerator: AtomicInteger = new AtomicInteger(1)
      roomState ++= (1001 to 1020).map {i =>
        (i, None.asInstanceOf[Option[RoomState]])
      }.toMap

      Behaviors.withTimers[Command] { implicit timer =>
        val roomIdGenerator = new AtomicInteger(1000)
        idle(roomIdGenerator)
      }
    }
  }

  def idle(roomIdGenerator: AtomicInteger)(implicit breakoutIdGenerator: AtomicInteger,
    stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case m@JoinRoom(userType, id, name, room, seat, replyTo) =>
          log.info(s"got $m")
          if (roomState.exists(r => r._2.nonEmpty && r._2.get.users.exists(u => u._2.info.id == id && u._2.state != 0))) {
            replyTo ! ErrorRsp(11003, "您在其他房间的对战未结束，请稍等！")
          } else {
            val breakoutId = if (roomState.exists(r => r._2.nonEmpty && r._2.get.users.exists(_._2.info.id == id))) { //已加入，换座位
              val bId = breakoutIdMap.getOrElse(id, 0.toByte)
              val oldRoom = roomState.filter(r => r._2.nonEmpty && r._2.get.users.exists(_._1 == bId)).head
              val userMap = oldRoom._2.get.users
              val newUserMap = userMap - bId
              if (newUserMap.nonEmpty) {
                roomState += oldRoom._1 -> Some(RoomState(oldRoom._2.get.pwd, newUserMap))
              } else roomState += oldRoom._1 -> None
              bId
            } else {
              val bId = (breakoutIdGenerator.getAndIncrement() % Byte.MaxValue).toByte
              breakoutIdMap += id -> bId
              bId
            }
            roomState.get(room) match {
              case Some(None) =>
                roomState += room -> Some(RoomState(None, Map(breakoutId -> User(seat, UserInfo(breakoutId, id, name), 0))))
                replyTo ! SuccessRsp()
              case Some(Some(roomInfo)) =>
                if (!roomInfo.users.exists(_._2.seat == seat)) {
                  val user = roomInfo.users ++ Map(breakoutId -> User(seat, UserInfo(breakoutId, id, name), 0))
                  roomState += room -> Some(RoomState(None, user))
                  replyTo ! SuccessRsp()
                } else {
                  log.error(s"该座位已有用户!")
                  replyTo ! ErrorRsp(11001, "该座位已有用户!")
                }
              case None =>
                log.error(s"该房间不存在")
                replyTo ! ErrorRsp(11002, "该房间不存在！")

            }
          }
          Behaviors.same

        case m@JoinGame(id, subscriber) =>
          log.info(s"got $m")
          val bId = breakoutIdMap.getOrElse(id, 0.toByte)
          val roomOpt = roomState.find(r => r._2.nonEmpty && r._2.get.users.contains(bId))
          if (roomOpt.nonEmpty) {
            val roomId = roomOpt.get._1
            val users = roomState(roomId).get.users
            if (users.filter(_._1 == bId).head._2.state == 0) {
              val userUpdate = users.filter(_._1 == bId).head._2.copy(state = 1)
              roomState.update(roomId, Some(RoomState(roomState(roomId).get.pwd, users + (bId -> userUpdate))))
              val userInfo = userUpdate.info
              val seat = users.filter(_._1 == bId).head._2.seat
              getRoomActor(ctx, roomId) ! RoomActor.JoinRoom(userInfo.bId, userInfo.id, userInfo.name, seat, subscriber) //todo 两用户加入才开始
              val old = roomMap.getOrElse(roomId, (None, mutable.Map.empty[String,(Byte, String)]))
              roomMap += roomId -> (None, old._2  + (userInfo.id -> (userInfo.bId, userInfo.name)))

              if (roomMap(roomId)._2.toList.length == 2) { //两人都准备完毕
                //              getRoomActor(ctx, )
                println(s"roomMap::$roomMap")
              }
            }
          }

          Behaviors.same

        case Logout(id) =>
          if (roomState.exists(r => r._2.nonEmpty && r._2.get.users.exists(_._2.info.id == id))) {
            val bId = breakoutIdMap.getOrElse(id, 0.toByte)
            val oldRoom = roomState.filter(r => r._2.nonEmpty && r._2.get.users.exists(_._1 == bId)).head
            val userMap = oldRoom._2.get.users
            val newUserMap = userMap - bId
            if (newUserMap.nonEmpty) {
              roomState += oldRoom._1 -> Some(RoomState(oldRoom._2.get.pwd, newUserMap))
            } else roomState += oldRoom._1 -> None

          }

          Behaviors.same

        case m@UserActionOnServer(id, action) =>
          if (roomMap.exists(r => r._2._2.keys.toList.contains(id))) {
            val roomId = roomMap.filter(r => r._2._2.exists(u => u._1 == id)).head._1
            getRoomActor(ctx, roomId) ! RoomActor.UserActionOnServer(id, action)
          }
          Behaviors.same

        case GetRoomState(replyTo) =>
          replyTo ! RoomStateRsp(roomState.filterNot(_._2.isEmpty).toList.map(r =>Room(r._1, r._2)))
          Behaviors.same

        case Left(id) =>
          roomMap.find(_._2._2.contains(id)) match {
            case Some(r) =>
              getRoomActor(ctx, r._1) ! Left(id)
              try {
                roomMap.update(r._1, r._2.copy(_2 = r._2._2 - id))
                if (roomMap.get(r._1).isEmpty || (roomMap.get(r._1).nonEmpty && roomMap(r._1)._2.toList.length == 0)) {
                  roomMap -= r._1
                  roomState +=  r._1 -> None
                }
                val state = roomState(r._1)
                val users = state.get.users
                val u = users.filter(_._2.info.id == id).head
                roomState.update(r._1, Some(state.get.copy(users = users + (u._1 -> u._2.copy(state = 2)))))

              } catch {
                case e: Exception =>
                  log.error(s"$e")
              }


            case None => log.error(s"not contains $id")
          }
          Behaviors.same

        case CloseRoom(roomId) =>
          roomState += roomId -> None
          roomMap -= roomId
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Int) = {
    val childName = s"room_$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.create(roomId), childName)
      //      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor

    }.upcast[RoomActor.Command]
  }

  private def sink(actor: ActorRef[Command], id: String) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = Left(id),
    onFailureMessage = FailMsgFront.apply
  )

  def joinGame(actor: ActorRef[RoomManager.Command],userId: String): Flow[Protocol.UserAction, WsSourceProtocol.WsMsgSource, Any] = {
    val in = Flow[Protocol.UserAction]
      .map {
        case action@Protocol.KeyDown(_,_) => UserActionOnServer(userId, action)
        case action@Protocol.KeyUp(_) => UserActionOnServer(userId, action)
        case action@Protocol.PressSpace => UserActionOnServer(userId, action)
        case action@Protocol.NeedToSync => UserActionOnServer(userId, action)
        case action@Protocol.OneMoreGame => UserActionOnServer(userId, action)
        case action@Protocol.SendText(_) => UserActionOnServer(userId, action)
        case other =>
          log.error(s"unknown action:::$other")
          UnKnowAction
      }
      .to(sink(actor, userId))

    val out =
      ActorSource.actorRef[WsSourceProtocol.WsMsgSource](
        completionMatcher = {
          case WsSourceProtocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case WsSourceProtocol.FailMsgServer(e) ⇒ e
        },
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! JoinGame(userId, outActor))

    Flow.fromSinkAndSource(in, out)
  }



}
