package org.seekloud.breakout.models

import scala.concurrent.Future
import org.seekloud.breakout.Boot.executor
/**
  * Created by dry on 2018/10/24.
  **/

case class UserInfo(id: Long, username: String, password: String, headImg: String, gender: Int, forbidden: Boolean)

trait UserInfoTable {

  import org.seekloud.breakout.utils.DBUtil.driver.api._

  class UserInfoTable(tag: Tag) extends Table[UserInfo](tag, "USER_INFO") {
    val id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
//    val appId = column[String]("APP_ID", O.PrimaryKey)
    val username = column[String]("USERNAME")
    val password = column[String]("PASSWORD")
    val headImg = column[String]("HEAD_IMG")
    val gender = column[Int]("GENDER")
    val forbidden = column[Boolean]("FORBIDDEN")

//    val state = column[Int]("STATE")
//    val accessToken = column[String]("ACCESS_TOKEN")
//    val expiresAt = column[Long]("EXPIRES_AT")
//    val refreshToken = column[String]("REFRESH_TOKEN")
//    val funcInfos = column[Long]("FUNC_INFOS")

//    def * = (appId, state, accessToken, expiresAt, refreshToken, funcInfos) <> (MpAuthInfo.tupled, MpAuthInfo.unapply)
    def * = (id, username, password, headImg, gender, forbidden) <> (UserInfo.tupled, UserInfo.unapply)
  }

  protected val userInfoTableQuery = TableQuery[UserInfoTable]
}

object UserInfoRepo extends UserInfoTable {

  import org.seekloud.breakout.utils.DBUtil.driver.api._
  import org.seekloud.breakout.utils.DBUtil.db



  def checkUserName(name: String) = {
    db.run(userInfoTableQuery.filter(_.username === name).result.headOption)
  }

  def addUser(userInfo: UserInfo) = {
    db.run(userInfoTableQuery.insertOrUpdate(userInfo))
  }

  def create(): Future[Unit] = {
    db.run(userInfoTableQuery.schema.create)
  }

  def getAllUsers() = {
    db.run(userInfoTableQuery.result)
  }

  def forbidUser(id: String, operate: Int) = {
    val forbid = operate match {
      case 1 => true
      case 0 => false
    }
    db.run(userInfoTableQuery.filter(_.id === id.drop(4).toLong).map(_.forbidden).update(forbid))
  }
//
//  def getAllAppId: Future[List[String]] = {
//    db.run (mpAuthInfoTableQuery.map(_.appId).to[List].result)
//  }
//
//  def getAllMpInfos: Future[List[MpAuthInfo]] = {
//    db.run (mpAuthInfoTableQuery.to[List].result)
//  }
//
//  def saveMpInfos(mpInfo:List[MpAuthInfo]) = {
//    val action = for {
//      _ <- mpAuthInfoTableQuery.delete
//      r2 <- mpAuthInfoTableQuery ++= mpInfo
//    } yield {
//      r2
//    }
//    db.run(action.transactionally)
//  }
//
//  def updateMpInfo(mpInfo: MpAuthInfo): Future[Int] = {
//    db.run(mpAuthInfoTableQuery.insertOrUpdate(mpInfo))
//  }
//
//  def getAuthInfo(mpAppId: String) = {
//    db.run(mpAuthInfoTableQuery.filter(m => m.appId === mpAppId && m.state === 1).result.headOption)
//  }
//
//  def updateMpInfo(mpAppId: String, accessToken: String, refreshToken: String, expiresAt: Long) = {
//    db.run(mpAuthInfoTableQuery.filter(m => m.appId === mpAppId).map(m => (m.accessToken,
//      m.refreshToken, m.expiresAt)).update((accessToken, refreshToken, expiresAt)))
//  }

}