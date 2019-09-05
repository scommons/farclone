package scommons.farc.ui.popup

import org.scalatest.Assertion
import scommons.farc.ui._
import scommons.farc.ui.border._
import scommons.react.blessed._
import scommons.react.test.TestSpec
import scommons.react.test.raw.ShallowInstance
import scommons.react.test.util.ShallowRendererUtils

class OkPopupSpec extends TestSpec with ShallowRendererUtils {

  it should "render component" in {
    //given
    val props = OkPopupProps("test message")

    //when
    val result = shallowRender(<(OkPopup())(^.wrapped := props)())

    //then
    assertOkPopup(result, props)
  }
  
  private def assertOkPopup(result: ShallowInstance, props: OkPopupProps): Unit = {
    val (width, height) = (50, 6)
    val expectedStyle = Popup.Styles.normal
    
    def assertComponents(border: ShallowInstance,
                         title: ShallowInstance,
                         message: ShallowInstance,
                         btn: ShallowInstance): Assertion = {

      assertComponent(border, DoubleBorder) { case DoubleBorderProps(resSize, style, pos) =>
        resSize shouldBe (width - 6) -> (height - 2)
        style shouldBe expectedStyle
        pos shouldBe 3 -> 1
      }
      assertComponent(title, TextLine) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Center
          pos shouldBe 3 -> 1
          resWidth shouldBe (width - 6)
          text shouldBe "Title"
          style shouldBe expectedStyle
          focused shouldBe false
          padding shouldBe 1
      }
      assertComponent(message, TextLine) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Center
          pos shouldBe 3 -> 2
          resWidth shouldBe (width - 6)
          text shouldBe props.message
          style shouldBe expectedStyle
          focused shouldBe false
          padding shouldBe 1
      }
      assertNativeComponent(btn,
        <.button(
          ^.rbMouse := true,
          ^.rbWidth := 4,
          ^.rbHeight := 1,
          ^.rbTop := height - 3,
          ^.rbLeft := "center",
          ^.rbStyle := expectedStyle,
          ^.content := " OK "
        )()
      )
    }
    
    assertComponent(result, Popup)({ case PopupProps(onClose, closable, focusable, _) =>
      onClose shouldBe props.onClose
      closable shouldBe true
      focusable shouldBe true
    }, { case List(box) =>
      assertNativeComponent(box,
        <.box(
          ^.rbClickable := true,
          ^.rbAutoFocus := false,
          ^.rbWidth := width,
          ^.rbHeight := height,
          ^.rbTop := "center",
          ^.rbLeft := "center",
          ^.rbShadow := true,
          ^.rbStyle := expectedStyle
        )(), {
          case List(border, title, message, btn) => assertComponents(border, title, message, btn)
        }
      )
    })
  }
}