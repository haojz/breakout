package org.seekloud.breakout.client

//import org.scalajs.dom.raw.CloseEvent
//import org.seekloud.carnie.model.ReplayInfo
import java.util.concurrent.atomic.AtomicInteger

import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw._
import org.seekloud.breakout.Protocol.{GameMessage, UserAction}
import org.seekloud.byteobject.ByteObject.{bytesDecode, _}
import org.seekloud.byteobject.{MiddleBufferInJs, decoder}
import org.seekloud.breakout.{Main, Protocol}

import scala.scalajs.js.typedarray.ArrayBuffer
import scalatags.JsDom.short.s

/**
  * Created by dry on 2018/9/3.
  **/
class WebSocketClient(
                        connectOpenSuccess: (Event) => Unit,
                        connectError: Event => Unit,
                        messageHandler: GameMessage => Unit,
                        close:(Event, Boolean) => Unit
                      ) {

  private var wsSetup = false
  private var serverState = true
  private var gameStreamOpt: Option[WebSocket] = None

  def setUp(id: String): Unit = {
    println(s"set up !!!!!!!!$id")
    if (!wsSetup) {
      val url = getWebSocketUri(id)
      val gameStream = new WebSocket(url)
      gameStreamOpt = Some(gameStream)

      gameStream.onopen = { event0: Event =>
        wsSetup = true
        connectOpenSuccess(event0)
      }

      gameStream.onerror = { event: Event =>
        wsSetup = false
        serverState = false
        gameStreamOpt = None
        connectError(event)
      }

      gameStream.onclose = { event: Event =>
        println(s"ws close========$event")
        wsSetup = false
        gameStreamOpt = None
        close(event, serverState)
        dom.window.location.href=s"http://${Main.host}:40110/breakout#/Home"
      }

      val messageIdGenerator = new AtomicInteger(0)
      val messageMap = scala.collection.mutable.Map[Int, (Option[Protocol.GameMessage], Boolean)]() //(消息编号，是否解码完成）

      gameStream.onmessage = { event: MessageEvent =>
        event.data match {
          case blobMsg: Blob =>
            val messageId = messageIdGenerator.getAndIncrement()
            messageMap += messageId -> (None, false)
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val middleDataInJs = new MiddleBufferInJs(fr.result.asInstanceOf[ArrayBuffer]) //put data into MiddleBuffer
            val encodedData: Either[decoder.DecoderFailure, Protocol.GameMessage] = bytesDecode[Protocol.GameMessage](middleDataInJs) // get encoded data.
              encodedData match {
                case Right(data) =>
                  data match {
                    case Protocol.CloseWs =>
                      gameStream.close()
                      wsSetup = false
                      gameStreamOpt = None
                      close(event, serverState)
                    case _ =>
                  }
//                  println(s"recv data:::$data")
                  if (!messageMap.exists(m => m._1 < messageId)) { //此前消息都已处理完毕
                    messageHandler(data)
                    messageMap -= messageId
                    var isContinue = true
                    while (isContinue && messageMap.nonEmpty) {
                      val msgMin = messageMap.keys.min
                      if (messageMap(msgMin)._2) {
                        messageHandler(messageMap(msgMin)._1.get)
                        messageMap -= msgMin
                      } else isContinue = false
                    }
                  } else { //存在未处理解码消息
                    messageMap.update(messageId, (Some(data), true))
                  }

                case Left(e) =>
                  println(s"got error: ${e.message}")
              }
            }
        }
      }
    }
  }

  val sendBuffer = new MiddleBufferInJs(409600) //sender buffer

  def sendMessage(msg: UserAction): Unit = {
    gameStreamOpt match {
      case Some(gameStream) =>
        gameStream.send(msg.fillMiddleBuffer(sendBuffer).result())

      case None => //
    }
  }

  def getWebSocketUri(id: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/breakout/game/joinGame?id=$id"
  }

  def getWsState:Boolean = wsSetup


}
