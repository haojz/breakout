//package com.neo.sk.bbsbot.front.style
//
//import scala.language.postfixOps
//import scalacss.DevDefaults._
//import scalacss.ProdDefaults.StyleA
///**
//  * User: Taoz
//  * Date: 4/7/2018
//  * Time: 1:31 PM
//  */
//object TestStyles extends StyleSheet.Inline {
//  import dsl._
//
//  val table = style(
//
//  )
//
//  val navBar = style(
//    padding(10 px),
//    backgroundColor(c"rgba(132,112,255,0.2)")
//  )
//
//
//  val navBarButton = style(
//    paddingLeft(15 px),
//    paddingRight(15 px),
//    paddingTop(5 px),
//    paddingBottom(5 px),
//    float.right,
//    backgroundColor(c"rgba(127,255,212, 0.8)"),
//    &.hover(
//      backgroundColor(c"#CDCD00")
//    )
//  )
//
//  val bigPicture = style(
//    height(200 px),
//    width(100.%%),
//    backgroundColor(c"rgba(139,134,78,0.6)")
//  )
//
//  val mainArea = style(
//    height(600 px),
//    width(100.%%),
//    backgroundColor(c"rgba(0,0,255,0.6)")
//  )
//
//
//  private val demoColor = rgb(255,239,213)
//
//
//  val demoTab = style(
//    cursor.pointer,
//    position.relative,
//    display.inlineBlock,
//    borderTopLeftRadius(6 px),
//    borderTopRightRadius(6 px),
//    paddingLeft(15 px),
//    paddingRight(15 px),
//    paddingTop(5 px),
//    marginTop(5 px),
//
//    backgroundColor(demoColor),
//
//    &.hover(
//      backgroundColor(c"#CDCD00")
//    )
//
//  )
//
//
//  val demoTabActive: StyleA = demoTab + style(
//    zIndex(2),
//    borderBottom.none,
//    paddingBottom(2 px)
//  )
//
//  //z-index:1; position:absolute; bottom:0%; height:2px; width:100%; background-color: #000000
//  val demoSplitLine = style(
//    position.absolute,
//    zIndex(1),
//    bottom(0.%%),
//    height(2 px),
//    width(100.%%),
//    backgroundColor(rgb(0,0,0))
//  )
//
//  val demoBox = style(
//
//    height(400 px),
//    padding(30 px),
//    backgroundColor(demoColor)
//  )
//
//
//}
