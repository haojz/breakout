package org.seekloud.breakout

import mhtml._
import org.scalajs.dom
import org.seekloud.breakout.client.NetGameHolder
import org.seekloud.breakout.utils._

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.seekloud.breakout.pages._
import org.seekloud.breakout.style.{HomeStyle, LoginPageStyle, RegisterPageStyle}

import scalacss.ProdDefaults._

/**
  * User: Taoz
  * Date: 3/29/2018
  * Time: 1:57 PM
  */
@JSExportTopLevel("breakout.Main")
object Main extends PageSwitcher {
//  val host = "0.0.0.0"
//  var name1 = ""
//  var name2 = ""
   val host = "10.1.29.250"



  val guestName = List("安琪拉","白起", "妲己","狄仁杰","典韦","韩信","老夫子","刘邦",
    "刘禅","鲁班七号","墨子","孙膑","孙尚香","孙悟空","项羽","亚瑟","周瑜",
    "庄周","蔡文姬","甄姬","廉颇","程咬金","后羿","扁鹊","钟无艳","小乔","王昭君",
    "虞姬","李元芳","张飞","刘备","牛魔王","张良","兰陵王","露娜","貂蝉","达摩","曹操",
    "芈月","荆轲","高渐离","钟馗","花木兰","关羽","李白","吕布","嬴政",
    "武则天","赵云","姜子牙","哪吒","诸葛亮","黄忠","大乔",
    "庞统", "鬼谷子","女娲","Aurora","Ariel","Belle","Jasmine",
    "Mulan","Tiana","Merida","Anna","Elsa","Moana")

  val currentPage = currentHashVar.map { ls =>
    println(s"currentPage change to ${ls.mkString(",")}")
    ls match {
      case "Home" :: Nil => <div style="width:100%">{Home.render}</div>
      case "Admin" :: Nil => <div style="width:100%">{Admin.render}</div>
      case "Register" :: Nil => <div style="width:100%">{Register.render}</div>
      case "Login" :: Nil => <div style="width:100%">{Login.render}</div>
      case "Game" :: id :: Nil => new NetGameHolder(id).render()
      case "Header" :: Nil => <div>{org.seekloud.breakout.components.Header.render}</div>
      case _ => <div>{Home.render}</div>
    }
  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
    <div class="container" style="max-width:100%;width: 100%; margin:0;padding:0;overflow-x:hidden;">
      <div class="row">
        {currentPage}
      </div>
    </div>
    LoginPageStyle.addToDocument()
    RegisterPageStyle.addToDocument()
    HomeStyle.addToDocument()
    mount(dom.document.body, page)

  }



  def main(args: Array[String]): Unit = {

  }

  @JSExport
  def run(): Unit = {
    Main.show()
    println(s"hahahaha")
  }

}

