package org.seekloud.breakout.style

import scala.language.postfixOps
import scalacss.DevDefaults._
/**
  * Created by haoshuhan on 2019/2/15.
  */
object HomeStyle extends StyleSheet.Inline {
  import dsl._
  val breakoutImgStyle = style(
    position.relative,
    left(5.%%),
    width(20.%%)
  )

  val questionImgStyle = style(
    position.relative,
    left(5.%%),
    width(10.%%),
    marginBottom(6.%%)
  )

  val manageUl = style(
    fontSize(14 px),
    position.absolute,
    backgroundColor(c"#FFFFFF"),
    boxShadow:="0px 2px 4px 0px grey",
    borderRadius(4 px),
    width(40 px),
    height.initial,
    top(30 px),
    right(-10 px),
    padding(0 px)
  )
}
