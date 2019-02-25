package org.seekloud.breakout.ptcl

/**
  * Created by haoshuhan on 2019/2/21.
  */
object AdminProtocol {
  case class User(userId: String, userName: String, forbidden: Boolean)
  case class AllUsersRsp(users: List[User],
                         errCode: Int = 0,
                         msg: String = "ok") extends CommonRsp

}
