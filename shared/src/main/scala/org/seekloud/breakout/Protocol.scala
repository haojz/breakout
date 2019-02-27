package org.seekloud.breakout

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:40 PM
  */

object Protocol {

  sealed trait GameMessage extends WsSourceProtocol.WsMsgSource

  sealed trait UserAction extends GameMessage

  case class GridDataSync(
                           frameCount: Long,
                           snakes: List[SkDt],
                           bricks: Map[Byte, (Byte, Byte)],
                           balls: List[Ball]
//                           bodyDetails: List[Bd],
//                           appleDetails: List[Ap]
                         ) extends GameMessage

  case class GridBallData(
                         snakes: List[SkDt],
                         balls: List[Ball]
                         )

  case class SomeOneLose(
                        frame: Int,
                        id: Byte,
                        score: List[(Byte, Int)]
                        ) extends GameMessage

  case object CloseWs extends GameMessage


//  case class FeedApples(
//                         aLs: List[Ap]
//                       ) extends GameMessage



  case class TextMsg(
                      msg: String
                    ) extends GameMessage

  case class SyncFrame(
                        frameCount: Int
                      ) extends GameMessage

  case class ScoreTest(
                  frameCount: Int,
                  score: Int
                  ) extends GameMessage

  case class Id(id: Byte) extends GameMessage

  case class RoomId(id: String) extends GameMessage

  case class InitBricks(frame: Int, bricks: List[(Byte, (Byte, Byte))]) extends GameMessage

  case class NewSnake(snake: SkDt) extends GameMessage

  case class NewBalls(frame: Int, balls: List[(Byte, Ball)]) extends GameMessage

  case class UserLeft(bId: Byte) extends GameMessage

  case class NewSnakeJoined(id: Long, name: String) extends GameMessage

  case class SnakeAction(breakoutId: Byte, keyCode: Byte, frame: Int) extends GameMessage

  case class TotalData(frame: Int, snakes: List[(Byte, SkDt)], balls: List[(Byte, Ball)], bricks: List[(Byte, (Byte, Byte))])
    extends GameMessage

  case class UserDead(frame: Int, users: List[String]) extends GameMessage

  case class SnakeLeft(id: Long, name: String) extends GameMessage

  case class NetDelayTest(createTime: Long) extends GameMessage

  case class KeyDown(keyCode: Byte, frameCount: Int) extends UserAction

  case class KeyUp(frameCount: Int) extends UserAction

  case class SendText(msg: String) extends UserAction

  case object PressSpace extends UserAction

  case class TextInfo(msg: String) extends UserAction

  case class Text(seat: Byte, msg: String) extends GameMessage

  case object NeedToSync extends UserAction

  case object OneMoreGame extends UserAction

  case class UserConfirm(bId: Byte) extends GameMessage

  case object ReJoin extends GameMessage


  val frameRate = 50

}

