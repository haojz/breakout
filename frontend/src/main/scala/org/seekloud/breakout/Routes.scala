package org.seekloud.breakout

/**
  * Created by haoshuhan on 2019/2/13.
  */
object Routes {
  val base = "/breakout"

  val login =  base + s"/user/login"
  val logout = base + s"/user/logout"
  val register = base + s"/user/register"
  val getRoomState = base + s"/user/getRoomState"

  object Game {
    val gameBase = base + "/game"
    def joinRoom(userType: Byte, id: String, name: String, room: String, seat: Byte) =
      gameBase + s"/joinRoom?type=$userType&id=$id&name=$name&roomId=$room&seat=$seat"
  }

  val getAllUsers = base + s"/user/getAllUsers"
  def forbidden(id: String, forbid: Int) = base + s"/user/forbidden?id=$id&forbid=$forbid"
}
