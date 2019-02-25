package org.seekloud.breakout.style
import scala.language.postfixOps
import scalacss.DevDefaults._
/**
  * Created by haoshuhan on 2019/2/13.
  */
object RegisterPageStyle extends StyleSheet.Inline{
  import dsl._
  val bg = style(
    backgroundColor(c"#F5F5F5"),
    backgroundSize := "cover",
    height(100.%%),
    width(100.%%),
    position.absolute
  )

  val registerBox = style(
    width(35.%%),
    margin(120 px, auto)
  )

  val rgst = style(
    width(400 px),
    height(360 px),
    paddingLeft(50 px),
    paddingRight(50 px),
    paddingBottom(50 px),
    backgroundColor(c"#ffffff"),
    borderRadius(6 px),
    boxSizing.borderBox,
    float.right,
    marginRight(50 px),
    position.relative,
    marginTop(50 px),
    paddingTop(20 px)
  )

}
