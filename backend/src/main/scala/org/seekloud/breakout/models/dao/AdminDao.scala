//package org.seekloud.breakout.models.dao
//import org.seekloud.breakout.utils.DBUtil.db
//import slick.jdbc.PostgresProfile.api._
//import org.seekloud.breakout.models.SlickTables._
//import org.seekloud.breakout.Boot.executor
///**
//  * Created by haoshuhan on 2019/2/21.
//  */
//object AdminDao {
//  def getAllUsers() = {
//    db.run(tUserInfo.result)
//  }
//
//  def forbidUser(id: String, operate: Int) = {
//    val forbid = operate match {
//      case 1 => true
//      case 0 => false
//    }
//    db.run(tUserInfo.filter(_.id === id.drop(4).toLong).map(_.forbidden).update(forbid))
//  }
//
//}
