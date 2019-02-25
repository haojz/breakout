//package org.seekloud.breakout.models.dao
//import org.seekloud.breakout.utils.DBUtil.db
//import slick.jdbc.PostgresProfile.api._
//import org.seekloud.breakout.models.SlickTables._
//import org.seekloud.breakout.Boot.executor
///**
//  * Created by haoshuhan on 2019/2/13.
//  */
//object UserDao {
//  def checkUserName(name: String) = {
//    db.run(tUserInfo.filter(_.userName === name).result.headOption)
//  }
//
//  def addUser(userInfo: rUserInfo) = {
//    db.run(tUserInfo += userInfo)
//  }
//
//}
