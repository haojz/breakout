package org.seekloud.breakout

/**
  * Created by haoshuhan on 2019/2/13.
  */
package object ptcl {
  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

  val dbError = ErrorRsp(10001, "dbError!")
  val nameError = ErrorRsp(10002, "用户名已存在！")
  val userError = ErrorRsp(10003, "用户不存在！")
  val pwdError = ErrorRsp(10004, "密码错误！")
  val forbidError = ErrorRsp(10005, "该账号已被禁用！")
}
