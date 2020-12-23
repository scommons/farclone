package farjs.ui

import scommons.react._
import scommons.react.blessed._

import scala.scalajs.js

case class LogPanelProps(content: String)

object LogPanel extends FunctionComponent[LogPanelProps] {
  
  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    
    <.log(
      ^.rbAutoFocus := false,
      ^.rbMouse := true,
      ^.rbStyle := styles.container,
      ^.rbScrollbar := true,
      ^.rbScrollable := true,
      ^.rbAlwaysScroll := true,
      ^.content := props.content
    )()
  }
  
  private[ui] lazy val styles = new Styles
  private[ui] class Styles extends js.Object {
    
    val container: BlessedStyle = new BlessedStyle {
      override val scrollbar = new BlessedScrollBarStyle {
        override val bg = "cyan"
      }
    }
  }
}
