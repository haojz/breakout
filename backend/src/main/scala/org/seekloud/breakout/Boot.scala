package org.seekloud.breakout

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.seekloud.breakout.http.HttpService

import scala.language.postfixOps
//import com.neo.sk.breakout.core.RoomManager
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.breakout.core.RoomManager.Command
import org.seekloud.breakout.core.RoomManager

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:25 PM
  */
object Boot extends HttpService {

  import concurrent.duration._
  import org.seekloud.breakout.common.AppSettings._


  override implicit val system = ActorSystem("arges", config)
  // the executor should not be the default dispatcher.
  override implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer = ActorMaterializer()
  override implicit val scheduler = system.scheduler


  override implicit val timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)
  val roomManager: ActorRef[Command] =system.spawn(RoomManager.create(),"roomManager")





  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done.")
  }






}
