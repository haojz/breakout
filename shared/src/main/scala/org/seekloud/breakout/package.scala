package org.seekloud

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
package object breakout {

  case class Point(x: Float, y: Float) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)

    def format = Point(x.formatted("%.4f").toFloat, y.formatted("%.4f").toFloat)
  }

  case class SkDt(
                   bId: Byte, //breakout id
                   id: String,  //id
                   name: String,
                   paddleLeft: Int,
                   color: Byte, //0:红色； 1：蓝色
                   characterLife: Int = 0,
                   level: Int = 0,
                   off: Int = 0,
                   life: Byte = 9,
                   score: Int = 0
                 )

  case class Ball(
                 bId: Byte,
                 speed: Int,
                 theta: Double, //角度（0， 180）
                 point: Point
                 )

  object Boundary{
    val w = 130
    val h = 60
    val start1 = 0
    val end1 = 50
    val start2 = 80
    val end2 = 130
  }

  object Window {
    val w = 120 //50 + 20 + 50
    val h = 60
  }

  val numEveryRow = 10
  val rowNum = 5

  object Constant {
    val ballRadius = 1
  }




}
