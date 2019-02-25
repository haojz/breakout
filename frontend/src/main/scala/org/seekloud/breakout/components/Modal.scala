package org.seekloud.breakout.components

import mhtml._
import org.scalajs.dom.Event
import org.seekloud.breakout.utils._
import scala.xml.Elem

class Modal(message:String) extends Component{

  val showModal : Var[String] = Var(message)
  val modalBody = showModal.map(modal=>
    if (modal != "")
      <div class="modal fade show" onclick={() => showModal := ""} style="display:block;background:rgba(0,0,0,0.5);cursor:pointer;z-index:10;">
        <div class="modal-dialog" style="top:30%" role="document" onclick={(e: Event) => e.stopPropagation()}>
          <div class="modal-content">
            <div class="modal-header" style="justify-content: center">
              <h5 class="modal-title">{message}</h5>
            </div>
            <div class="modal-footer" style="justify-content:center">
              <button type="button" class="btn btn-secondary" onclick={()=>showModal := ""}>确定</button>
            </div>
          </div>
        </div>
      </div>
    else
      emptyHTML
  )

  override def render: Elem ={
    <div>
      {modalBody}
    </div>
  }
}
