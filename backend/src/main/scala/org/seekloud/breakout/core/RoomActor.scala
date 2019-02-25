package org.seekloud.breakout.core
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.seekloud.breakout.Protocol._
import org.seekloud.breakout.client.GridOnServer
import scala.language.postfixOps
import concurrent.duration._
import org.seekloud.breakout.common.AppSettings
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import org.slf4j.LoggerFactory
//import org.seekloud.breakout.core.RoomManager
import org.seekloud.breakout.Protocol._
import org.seekloud.breakout._
import org.seekloud.breakout.core.RoomManager.Left
import org.seekloud.breakout.Boot.roomManager
/**
  * Created by haoshuhan on 2019/2/4.
  */
object RoomActor {
  trait Command

  case class JoinRoom(breakoutId: Byte, id: String, name: String, seat: Byte, subscriber: ActorRef[WsSourceProtocol.WsMsgSource]) extends Command

  case object ReJoinRoom extends Command

  case class UserInfo(breakoutId: Byte, name: String, joinFrame: Long, seat: Byte)

  case class UserActionOnServer(id: String, action: Protocol.UserAction) extends Command

//  case class UserDead(roomId: Int, users: List[String]) extends Command with RoomManager.Command

  private final case object SyncKey

  private case object Sync extends Command

  private final case object CloseRoomKey

  private case object Close extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

//  private var init = false
//  private var bricksInit = false

  def create(roomId: Int): Behavior[Command] = {
    log.debug(s"Room Actor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers[Command] {
        implicit timer =>
          val border = Point(200, 100)
          val grid = new GridOnServer(border)

          idle(roomId, false, grid, deadList = None, tickCount = 0l)
      }
    }
  }

  def idle(roomId: Int,
           init: Boolean,
           grid: GridOnServer,
           userMap: mutable.HashMap[String, UserInfo] = mutable.HashMap[String, UserInfo](),
           deadList: Option[Byte],
           subscribersMap: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]] = mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]](),
           tickCount: Long,
           oneMoreGameConfirm: List[String] = Nil
          )(implicit timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case m@JoinRoom(breakoutId, id, name, seat, subscriber) =>
          log.info(s"got JoinRoom $m")
          subscribersMap.put(id, subscriber)
          dispatchTo(subscribersMap, id, Protocol.Id(breakoutId))
          userMap.put(id, UserInfo(breakoutId, name, tickCount, seat))
          val paddle = seat match {
            case 0 => 20
            case 1 => 100
          }
          val snake = SkDt(breakoutId, id, name, paddle, seat)
          grid.snakes += breakoutId -> snake
          if (userMap.toList.length == 2) {
//            println(s"---------------bricks init")
            grid.initBricks()
//            bricksInit = true
            timer.startPeriodicTimer(SyncKey, Sync, frameRate millis)
            //            val bricks = grid.initBricks().toList.sortBy(_._1)
//            dispatch(subscribersMap, Protocol.InitBricks(bricks))
          }

//          println(s"snakes::::::::::${grid.snakes}")
          dispatch(subscribersMap, Protocol.NewSnake(snake))
//          grid.addSnake(id)
//          println(s"userMap:::::$userMap")
          Behaviors.same

        case ReJoinRoom =>
          timer.cancel(CloseRoomKey)
          dispatch(subscribersMap, Protocol.ReJoin)
          for ((u, i) <- userMap) {
            val paddle = i.seat match {
              case 0 => 20
              case 1 => 100
            }
            val snake = SkDt(i.breakoutId, u, i.name, paddle, i.seat)
            grid.snakes += i.breakoutId -> snake
            dispatch(subscribersMap, Protocol.NewSnake(snake))
          }
          grid.initBricks()
          timer.startPeriodicTimer(SyncKey, Sync, frameRate millis)
          Behaviors.same

        case UserActionOnServer(id, action) =>
          action match {
            case PressSpace =>
              log.info(s"pressSpace::$id")
              if (userMap.contains(id)) {
                //              if (deadList.contains(id)) {
                val bId = userMap(id).breakoutId
                if (grid.snakes.get(bId).nonEmpty && grid.snakes(bId).life > 0) {
                  grid.addSnake(id)
                  idle(roomId, init, grid, userMap, deadList, subscribersMap, tickCount + 1)
                } else {
                  log.error(s"snakes life = 0")
                  Behaviors.same
                }
              } else {
                log.error(s"userMap not contain $id")
                Behaviors.same
              }

//              } else {
//                log.error(s"deadList not contain $id")
//                Behaviors.same
//              }

            case KeyDown(key, frameCount) =>
              val realFrame = Math.max(grid.frameCount, frameCount)
              grid.addActionWithFrame(id, key, realFrame)
              if (userMap.get(id).nonEmpty) {
                val bId = userMap(id).breakoutId
                dispatch(subscribersMap, Protocol.SnakeAction(bId, key, realFrame.toInt))
              }
              Behaviors.same

            case KeyUp(frameCount) =>
//              println(s"userMap:::::$userMap")
              val realFrame = Math.max(grid.frameCount, frameCount)
              grid.addActionWithFrame(id, 0, realFrame)
              if (userMap.get(id).nonEmpty) {
                val bId = userMap(id).breakoutId
                dispatch(subscribersMap, Protocol.SnakeAction(bId, 0.toByte, realFrame.toInt))
              }
              Behaviors.same

            case m: SendText =>
              log.info(s"recv msg:$m")
              if (grid.snakes.exists(_._2.id == id)) {
                val seat = grid.snakes.filter(_._2.id == id).head._2.color
                dispatch(subscribersMap, Protocol.Text(seat, m.msg))
              } else {
                log.error(s"找不到该用户！！")
              }
              Behaviors.same

            case NeedToSync =>
              dispatchTo(subscribersMap, id, grid.getGridData)
              Behaviors.same

            case OneMoreGame =>
              if (oneMoreGameConfirm.isEmpty) {
                if (userMap.get(id).nonEmpty) dispatch(subscribersMap, Protocol.UserConfirm(userMap(id).breakoutId))
                idle(roomId, init, grid, userMap, deadList, subscribersMap, tickCount, List(id))
              } else { //一方已确认
                dispatch(subscribersMap, Protocol.UserConfirm(userMap(id).breakoutId))
                val border = Point(200, 100)
                val newGrid = new GridOnServer(border)
                ctx.self ! ReJoinRoom
                idle(roomId, false, newGrid, userMap, deadList = None, subscribersMap, tickCount = 0l)
              }

            case _ =>
              Behaviors.same
          }

//        case m@UserDead(_, users) =>
//          log.info(s"recv userdead:$users")
//          dispatch(subscribersMap, Protocol.UserDead(grid.frameCount.toInt, users))
//          idle(roomId, grid, userMap, users:::deadList, subscribersMap, tickCount)

        case Left(id) =>
          if (userMap.get(id).nonEmpty){
            val bId = userMap(id).breakoutId
            dispatch(subscribersMap.filterNot(_._1 == id), Protocol.UserLeft(bId))
            timer.cancel(SyncKey)
            timer.startSingleTimer(CloseRoomKey, Close, 5000 millis)
          } else log.error(s"when user $id left, can not find bId ")
          Behaviors.same

        case Close =>
          log.error(s"room:$roomId close;")
//          Behaviors.same
          dispatch(subscribersMap, Protocol.CloseWs)
          roomManager ! RoomManager.CloseRoom(roomId)
          Behaviors.stopped

        case Sync =>
//          println(s"userMap:::::$userMap")
          val deadUsers = grid.updateInService(roomId)
          val newData = grid.getGridData
          val initUpdate = if (grid.snakes.toList.length == 2 && !init) {
//            println(s"!!!!!!!!!!!!!!!!!!!!!!!!!!dispatch totaldata")
            dispatch(subscribersMap, newData)
            true
          } else init
          if (grid.newInfo.nonEmpty) {
            dispatch(subscribersMap, Protocol.NewBalls(grid.frameCount.toInt, grid.newInfo))
            println(s"grid.newInfo:::${grid.newInfo}")
            grid.newInfo.map(_._2.bId).foreach(b => grid.snakes += b -> grid.snakes(b).copy(life = (grid.snakes(b).life -1).toByte))
          }
          val filterDead = deadUsers.filter(u => u.life == 0 && !grid.balls.exists(_._2.bId == u.bId))
          val newDeadList = if (filterDead.nonEmpty) {
            if (deadList.nonEmpty) {
              val dead1Score = grid.snakes(deadList.get).score
              val dead2Score = filterDead.head.score
              if (dead1Score == dead2Score) {
                dispatch(subscribersMap, Protocol.SomeOneLose(grid.frameCount.toInt, -1, grid.snakes.map(s => (s._2.color, s._2.score)).toList))
              } else if (dead1Score < dead2Score){
                dispatch(subscribersMap, Protocol.SomeOneLose(grid.frameCount.toInt, deadList.get, grid.snakes.map(s => (s._2.color, s._2.score)).toList))
              } else {
                dispatch(subscribersMap, Protocol.SomeOneLose(grid.frameCount.toInt, filterDead.head.bId, grid.snakes.map(s => (s._2.color, s._2.score)).toList))
              }
              timer.cancel(SyncKey)
              timer.startSingleTimer(CloseRoomKey, Close, 15000 millis)
              deadList
            } else {
              val dead = filterDead.head
              val deadScore = dead.score
              val alive = grid.snakes.find(_._1 != dead.bId)
              if (alive.nonEmpty && deadScore <= alive.get._2.score) {
                dispatch(subscribersMap, Protocol.SomeOneLose(grid.frameCount.toInt, dead.bId, grid.snakes.map(s => (s._2.color, s._2.score)).toList))
                timer.cancel(SyncKey)
                timer.startSingleTimer(CloseRoomKey, Close, 15000 millis)
                Some(dead.bId)
              } else {  //有玩家死亡，但是分数比另一玩家高，等待另一玩家死亡或者另一玩家分数超过
                Some(dead.bId)
              }
            }
          } else deadList

          if(deadList.nonEmpty) {
            val dead = grid.snakes(deadList.get)
            val alive = grid.snakes.find(_._1 != dead.bId)
            if (alive.nonEmpty && dead.score < alive.get._2.score) { //另一玩家分数超过死亡玩家
              dispatch(subscribersMap, Protocol.SomeOneLose(grid.frameCount.toInt, dead.bId, grid.snakes.map(s => (s._2.color, s._2.score)).toList))
              timer.cancel(SyncKey)
              timer.startSingleTimer(CloseRoomKey, Close, 15000 millis)
            }
          }
          (0 to 1).foreach{seat =>
            if (!grid.bricks.exists(_._2._1 == seat.toByte)) {
              val bricks = grid.initBrick(seat).toList
              dispatch(subscribersMap, Protocol.InitBricks(grid.frameCount.toInt, bricks))
              val snakeOp = grid.snakes.find(_._2.color == seat.toByte)
              if (snakeOp.nonEmpty) {
                val s = snakeOp.get
                grid.snakes += s._1 -> s._2.copy(level = s._2.level + 1, off = 0)
              }

            }

          }

          for ((u, i) <- userMap) {
            if ((tickCount - i.joinFrame) % 60 == 30) dispatchTo(subscribersMap, u, Protocol.SyncFrame(grid.frameCount.toInt))
//            try {
//              if ((tickCount - i.joinFrame) % 100 == 0) dispatchTo(subscribersMap, u, Protocol.ScoreTest(grid.frameCount.toInt, grid.snakes(i.breakoutId).score))
//            } catch {
//              case e : Exception =>
//                log.error(s"exception!!::$e")
//            }
          }

          grid.newInfo = Nil

          idle(roomId, initUpdate, grid, userMap, newDeadList, subscribersMap, tickCount + 1)

      }
    }
  }

  def dispatchTo(subscribers: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]], id: String, gameOutPut: Protocol.GameMessage): Unit = {
    subscribers.get(id).foreach {
      _ ! gameOutPut
    }
  }

  def dispatch(subscribers: mutable.HashMap[String, ActorRef[WsSourceProtocol.WsMsgSource]], gameOutPut: Protocol.GameMessage): Unit = {
    subscribers.values.foreach {
      _ ! gameOutPut
    }
  }
}
