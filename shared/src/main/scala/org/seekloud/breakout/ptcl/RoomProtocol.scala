package org.seekloud.breakout.ptcl
/**
  * Created by haoshuhan on 2019/2/14.
  */
object RoomProtocol {
//  trait State
//  case object InGame extends State
//  case object Ready extends State
  case class RoomState(pwd: Option[String], users: Map[Byte, User])
  case class UserInfo(bId: Byte, id: String, name: String)
  case class User(seat: Byte, info: UserInfo, state: Byte) // state = 0: Ready; state =1: InGame; state =2: Left;

//  case class Room(room: (Int, Option[RoomState])) //????不能直接传输
  case class Room(roomId: Int, roomState: Option[RoomState]) //????不能直接传输

  case class RoomStateRsp(roomState: List[Room],
                          errCode: Int = 0,
                          msg: String = ""
                         ) extends CommonRsp


  case class RoomStateReq(userId: String)

}
