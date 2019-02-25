package org.seekloud

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
package object breakout {

  sealed trait Spot
//  case class Body(id: Long, life: Int) extends Spot
//  case class Header(id: Long, life: Int) extends Spot
//  case class Apple(score: Int, life: Int) extends Spot
  case class Paddle(bId: Byte) extends Spot
  case class Brick(id: Byte, character: Byte) extends Spot

  case class Test(id: String) extends Spot


  case class Score(id: Long, n: String, k: Int, l: Int, t: Option[Long] = None)
  case class Br(id: Long, life: Int, x: Int, y: Int)
//  case class Ap(score: Int, life: Int, x: Int, y: Int)



  case class Point(x: Float, y: Float) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)

    def format = Point(x.formatted("%.4f").toFloat, y.formatted("%.4f").toFloat)
  }


  class Snake(x: Int, y: Int, len: Int = 5, d: Point = Point(1, 0)) {
    var length = len
    var direction = d
    var header = Point(x, y)
  }

  case class SkDt(
                   bId: Byte, //breakout id
                   id: String,  //id
                   name: String,
                   paddleLeft: Int,
                   color: Byte, //0:红色； 1：蓝色
//                   direction: Point = Point(1, 0),
                   characterLife: Int = 0,
                   level: Int = 0,
                   off: Int = 0,
                   life: Byte = 15,
                   length: Int = 4,
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




}
