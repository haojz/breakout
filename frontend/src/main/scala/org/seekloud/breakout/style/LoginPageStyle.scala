package org.seekloud.breakout.style

import scala.language.postfixOps
import scalacss.DevDefaults._
/**
  * Created by haoshuhan on 2018/4/25.
  */
object LoginPageStyle extends StyleSheet.Inline {
  import dsl._
  val bg = style(
    backgroundColor(c"#F5F5F5"),
    backgroundSize := "cover",
    height(100.%%),
    width(100.%%),
    position.absolute
  )

  val loginBox = style(
    width(1100 px),
    margin(120 px, auto)
  )

  val loginImage = style(
    float.left,
    width(432 px),
    height(440 px),
    marginLeft(50 px)
  )

}
