package org.seekloud.breakout.utils

import java.io._

import org.slf4j.LoggerFactory
/**
  * @author Jingyi
  * @version 创建时间：2018/12/18
  */
object FileUtil {

  private val log = LoggerFactory.getLogger(this.getClass)

  def readFileByLines(path: String): List[String] =  {
    log.debug(s"read $path")
    var res:List[String] = List()
//    val url: URL = new URL(path)
//    val urlconn: URLConnection = url.openConnection()
//    urlconn.connect()
//    val isReader: InputStreamReader = new InputStreamReader(urlconn.getInputStream,"UTF-8")
//    val reader: BufferedReader = new BufferedReader(isReader)
    val file: File = new File(path)
    var reader: BufferedReader = null
    try {
      var tempString: String = null
      reader = new BufferedReader(new FileReader(file))
      tempString = reader.readLine()
      while (tempString != null) {
        res = res :+ tempString
        tempString = reader.readLine()
      }
      reader.close()
      res
    } catch{
      case e:IOException =>
        e.printStackTrace()
        res
    } finally {
      if (reader != null) {
        try {
          reader.close()
        } catch{
          case e1:IOException =>
            e1.printStackTrace()
        }
      }
    }
  }
}
