package scommons.farc.ui

import scommons.react._
import scommons.react.blessed._
import scommons.react.hooks._

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object LogPanel extends FunctionComponent[Unit] {
  
  protected def render(props: Props): ReactElement = {
    val (content, setContent) = useStateUpdater("")
    val oldLogRef = useRef[js.Any](null)
    
    useLayoutEffect({ () =>
      oldLogRef.current = g.console.log
      val log: js.Function1[String, Unit] = { msg: String =>
        setContent(c => c + s"$msg\n")
      }
      g.console.log = log

      val cleanup: js.Function0[Unit] = { () =>
        g.console.log = oldLogRef.current
      }
      cleanup
    }, Nil)
    
    <.log(
      ^.rbMouse := true,
      ^.rbStyle := styles.container,
      ^.rbScrollbar := true,
      ^.rbScrollable := true,
      ^.rbAlwaysScroll := true,
      ^.content := content
    )()
  }
  
  private[ui] lazy val styles = Styles
  
  private[ui] object Styles extends js.Object {
    val container: BlessedStyle = new BlessedStyle {
      override val scrollbar = new BlessedScrollBarStyle {
        override val bg = "cyan"
      }
    }
  }
}