package org.seekloud.breakout.http

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Materializer, Supervision}
import akka.util.{ByteString, Timeout}
import org.seekloud.breakout.core.RoomManager
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import org.seekloud.breakout.Protocol._
import org.seekloud.breakout.Boot.roomManager
import akka.actor.typed.scaladsl.AskPattern._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.seekloud.breakout.ptcl._
import org.seekloud.breakout.utils.CirceSupport
/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 4:13 PM
  */
trait GameService extends CirceSupport with ServiceUtils{


  import io.circe.generic.auto._
  import io.circe.syntax._

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

//  lazy val playGround = PlayGround.create(system)

  val idGenerator = new AtomicInteger(1000000)

  private[this] val log = LoggerFactory.getLogger("com.neo.sk.hiStream.http.SnakeService")


//  val netSnakeRoute = {
//
//    path("join") {
//      parameter(
//        'id.as[String],
//        'name.as[String],
//        'mode.as[Int],
//        'img.as[Int]
//      ) { (id, name, mode, img) =>
//        handleWebSocketMessages(webSocketChatFlow(id, name, mode, img))
//      }
//    }
//  }
//
//
//  def webSocketChatFlow(id: String, name: String, mode: Int, img: Int): Flow[Message, Message, Any] =
//    Flow[Message]
//      .collect {
//        case TextMessage.Strict(msg) =>
//          log.debug(s"msg from webSocket: $msg")
//          msg
//        // unpack incoming WS text messages...
//        // This will lose (ignore) messages not received in one chunk (which is
//        // unlikely because chat messages are small) but absolutely possible
//        // FIXME: We need to handle TextMessage.Streamed as well.
//      }
////      .via(playGround.joinGame(idGenerator.getAndIncrement().toLong, sender)) // ... and route them through the chatFlow ...
//      .via(RoomManager.joinGame(roomManager, id, name, mode, img)) // ... and route them through the chatFlow ...
//      .map { msg => TextMessage.Strict(msg.asJson.noSpaces) // ... pack outgoing messages into WS JSON messages ...
//      //.map { msg => TextMessage.Strict(write(msg)) // ... pack outgoing messages into WS JSON messages ...
//    }.withAttributes(ActorAttributes.supervisionStrategy(decider))    // ... then log any processing errors on stdin
//
//
//  val decider: Supervision.Decider = {
//    e: Throwable =>
//      e.printStackTrace()
//      println(s"WS stream failed with $e")
//      Supervision.Resume
//  }

  val gameRoute = {
    (pathPrefix("game") & get) {
      pathEndOrSingleSlash {
        getFromResource("html/netSnake.html")
      } ~
        path("joinRoom") {
          parameter(
            'type.as[Byte],
            'id.as[String],
            'name.as[String],
            'roomId.as[Int],
            'seat.as[Byte]
          ) { (userType, id, name, roomId, seat) =>
//            println(s"name::::::::$name")
            val rstF: Future[CommonRsp] = roomManager ? (RoomManager.JoinRoom(userType, id, name, roomId, seat, _))
            dealFutureResult{
              rstF.map {
                case rst: SuccessRsp => complete(rst)
                case rst: ErrorRsp => complete(rst)
                case _ => complete(ErrorRsp(100001, "internal error!"))
              }
            }


//            handleWebSocketMessages(webSocketSnakeFlow(userType, id, name, roomId, seat))
          }
        } ~
      path("joinGame") {
        parameter(
          'id.as[String]
        ) {id =>
          handleWebSocketMessages(webSocketSnakeFlow(id))
        }
      }
    }
  }


  def webSocketSnakeFlow(id: String): Flow[Message, Message, Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._
    import org.seekloud.byteobject.MiddleBufferInJvm
    import io.circe.generic.auto._
    import io.circe.parser._
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          TextInfo(msg)
        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
        case BinaryMessage.Strict(bMsg) =>
          //decode process.
          val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
          val msg =
            bytesDecode[UserAction](buffer) match {
              case Right(v) => v
              case Left(e) =>
                println(s"decode error: ${e.message}")
                TextInfo("decode error")
            }
          msg
        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(RoomManager.joinGame(roomManager, id)) // ... and route them through the chatFlow ...
      .map {
        case msg: GameMessage =>
          val sendBuffer = new MiddleBufferInJvm(409600)
          val a = msg.fillMiddleBuffer(sendBuffer).result()
          BinaryMessage.Strict(ByteString(a))

        case x =>
          println("unknown")
          TextMessage.apply("")
       // ... pack outgoing messages into WS JSON messages ...
      //.map { msg => TextMessage.Strict(write(msg)) // ... pack outgoing messages into WS JSON messages ...
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
  }


  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }



}
