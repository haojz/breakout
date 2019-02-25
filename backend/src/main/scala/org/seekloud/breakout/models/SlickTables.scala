package org.seekloud.breakout.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tUserInfo.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tUserInfo
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param userId Database column user_id SqlType(varchar), Length(100,true)
    *  @param userName Database column user_name SqlType(varchar), Length(100,true)
    *  @param headImg Database column head_img SqlType(varchar), Length(500,true)
    *  @param gender Database column gender SqlType(int4)
    *  @param password Database column password SqlType(varchar), Length(100,true), Default()
    *  @param forbidden Database column forbidden SqlType(bool), Default(false) */
  final case class rUserInfo(id: Long, userId: String, userName: String, headImg: String, gender: Int, password: String = "", forbidden: Boolean = false)
  /** GetResult implicit for fetching rUserInfo objects using plain SQL queries */
  implicit def GetResultrUserInfo(implicit e0: GR[Long], e1: GR[String], e2: GR[Int], e3: GR[Boolean]): GR[rUserInfo] = GR{
    prs => import prs._
      rUserInfo.tupled((<<[Long], <<[String], <<[String], <<[String], <<[Int], <<[String], <<[Boolean]))
  }
  /** Table description of table user_info. Objects of this class serve as prototypes for rows in queries. */
  class tUserInfo(_tableTag: Tag) extends profile.api.Table[rUserInfo](_tableTag, "user_info") {
    def * = (id, userId, userName, headImg, gender, password, forbidden) <> (rUserInfo.tupled, rUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(userId), Rep.Some(userName), Rep.Some(headImg), Rep.Some(gender), Rep.Some(password), Rep.Some(forbidden)).shaped.<>({r=>import r._; _1.map(_=> rUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(varchar), Length(100,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(100,varying=true))
    /** Database column user_name SqlType(varchar), Length(100,true) */
    val userName: Rep[String] = column[String]("user_name", O.Length(100,varying=true))
    /** Database column head_img SqlType(varchar), Length(500,true) */
    val headImg: Rep[String] = column[String]("head_img", O.Length(500,varying=true))
    /** Database column gender SqlType(int4) */
    val gender: Rep[Int] = column[Int]("gender")
    /** Database column password SqlType(varchar), Length(100,true), Default() */
    val password: Rep[String] = column[String]("password", O.Length(100,varying=true), O.Default(""))
    /** Database column forbidden SqlType(bool), Default(false) */
    val forbidden: Rep[Boolean] = column[Boolean]("forbidden", O.Default(false))
  }
  /** Collection-like TableQuery object for table tUserInfo */
  lazy val tUserInfo = new TableQuery(tag => new tUserInfo(tag))
}
