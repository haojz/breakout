package org.seekloud.breakout.ptcl

//import org.seekloud.breakout.ptcl.RoomProtocol.Rooms

/**
  * Created by haoshuhan on 2019/2/13.
  */
object UserProtocol {
  case class RegisterReq(name: String,
                         pwd: String,
                         gender: Int = 0
                        )
  case class LoginReq(name: String, pwd: String)
  case class UserBaseInfo(id: String,
                          name: String)

  case class UserDetailInfo(id: String,
                            name: String,
                            headImg: String,
                            gender: Int)

  case class LoginRsp(userInfo: UserDetailInfo,
                      errCode: Int = 0,
                      msg: String = "") extends CommonRsp



}
