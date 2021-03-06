package farjs.filelist.copy

import farjs.filelist.copy.CopyProgressPopup._
import farjs.ui._
import farjs.ui.border._
import farjs.ui.popup.ModalContent._
import farjs.ui.popup.ModalProps
import farjs.ui.theme.Theme
import org.scalatest.Assertion
import scommons.react._
import scommons.react.blessed._
import scommons.react.test._

class CopyProgressPopupSpec extends TestSpec with TestRendererUtils {

  CopyProgressPopup.modalComp = () => "Modal".asInstanceOf[ReactClass]
  CopyProgressPopup.textLineComp = () => "TextLine".asInstanceOf[ReactClass]
  CopyProgressPopup.horizontalLineComp = () => "HorizontalLine".asInstanceOf[ReactClass]

  it should "render component" in {
    //given
    val props = getCopyProgressPopupProps()

    //when
    val result = testRender(<(CopyProgressPopup())(^.wrapped := props)())

    //then
    assertCopyProgressPopup(result, props)
  }

  it should "convert seconds to time when toTime" in {
    //when & then
    toTime(0) shouldBe "00:00:00"
    toTime(1) shouldBe "00:00:01"
    toTime(61) shouldBe "00:01:01"
    toTime(3601) shouldBe "01:00:01"
    toTime(3661) shouldBe "01:01:01"
    toTime(3662) shouldBe "01:01:02"
  }
  
  it should "convert bits per second to speed when toSpeed" in {
    //when & then
    toSpeed(0) shouldBe "0b"
    toSpeed(99000) shouldBe "99000b"
    toSpeed(100000) shouldBe "100Kb"
    toSpeed(99000000) shouldBe "99000Kb"
    toSpeed(100000000) shouldBe "100Mb"
    toSpeed(99000000000d) shouldBe "99000Mb"
    toSpeed(100000000000d) shouldBe "100Gb"
  }
  
  private def getCopyProgressPopupProps(item: String = "test item",
                                        to: String = "test to",
                                        itemPercent: Int = 1,
                                        total: Double = 2,
                                        totalPercent: Int = 3,
                                        timeSeconds: Int = 4,
                                        leftSeconds: Int = 5,
                                        bytesPerSecond: Double = 6,
                                        onCancel: () => Unit = () => ()): CopyProgressPopupProps = {
    CopyProgressPopupProps(
      item = item,
      to = to,
      itemPercent = itemPercent,
      total = total,
      totalPercent = totalPercent,
      timeSeconds = timeSeconds,
      leftSeconds = leftSeconds,
      bytesPerSecond = bytesPerSecond,
      onCancel = onCancel
    )
  }

  private def assertCopyProgressPopup(result: TestInstance, props: CopyProgressPopupProps): Unit = {
    val (width, height) = (50, 13)
    val contentWidth = width - (paddingHorizontal + 2) * 2
    val contentLeft = 2
    val theme = Theme.current.popup.regular

    def assertComponents(label: TestInstance,
                         item: TestInstance,
                         to: TestInstance,
                         itemPercent: TestInstance,
                         sep1: TestInstance,
                         total: TestInstance,
                         totalPercent: TestInstance,
                         sep2: TestInstance,
                         time: TestInstance,
                         speed: TestInstance,
                         button: TestInstance): Assertion = {

      assertNativeComponent(label,
        <.text(
          ^.rbLeft := contentLeft,
          ^.rbTop := 1,
          ^.rbStyle := theme,
          ^.content :=
            """Copying the file
              |
              |to
              |""".stripMargin
        )()
      )
      assertTestComponent(item, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, resStyle, focused, padding) =>
          align shouldBe TextLine.Left
          pos shouldBe contentLeft -> 2
          resWidth shouldBe contentWidth
          text shouldBe props.item
          resStyle shouldBe theme
          focused shouldBe false
          padding shouldBe 0
      }
      assertTestComponent(to, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, resStyle, focused, padding) =>
          align shouldBe TextLine.Left
          pos shouldBe contentLeft -> 4
          resWidth shouldBe contentWidth
          text shouldBe props.to
          resStyle shouldBe theme
          focused shouldBe false
          padding shouldBe 0
      }

      assertTestComponent(itemPercent, progressBarComp) {
        case ProgressBarProps(percent, pos, resLength, resStyle) =>
          percent shouldBe props.itemPercent
          pos shouldBe contentLeft -> 5
          resLength shouldBe contentWidth
          resStyle shouldBe theme
      }
      assertTestComponent(sep1, horizontalLineComp) {
        case HorizontalLineProps(pos, resLength, lineCh, resStyle, startCh, endCh) =>
          pos shouldBe contentLeft -> 6
          resLength shouldBe contentWidth
          lineCh shouldBe SingleBorder.horizontalCh
          resStyle shouldBe theme
          startCh shouldBe None
          endCh shouldBe None
      }
      assertTestComponent(total, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, resStyle, focused, padding) =>
          align shouldBe TextLine.Center
          pos shouldBe contentLeft -> 6
          resWidth shouldBe contentWidth
          text shouldBe f"Total: ${props.total}%,.0f"
          resStyle shouldBe theme
          focused shouldBe false
          padding shouldBe 1
      }
      assertTestComponent(totalPercent, progressBarComp) {
        case ProgressBarProps(percent, pos, resLength, resStyle) =>
          percent shouldBe props.totalPercent
          pos shouldBe contentLeft -> 7
          resLength shouldBe contentWidth
          resStyle shouldBe theme
      }

      assertTestComponent(sep2, horizontalLineComp) {
        case HorizontalLineProps(pos, resLength, lineCh, resStyle, startCh, endCh) =>
          pos shouldBe contentLeft -> 8
          resLength shouldBe contentWidth
          lineCh shouldBe SingleBorder.horizontalCh
          resStyle shouldBe theme
          startCh shouldBe None
          endCh shouldBe None
      }

      assertNativeComponent(time,
        <.text(
          ^.rbLeft := contentLeft,
          ^.rbTop := 9,
          ^.rbStyle := theme,
          ^.content := s"Time: ${toTime(props.timeSeconds)} Left: ${toTime(props.leftSeconds)}"
        )()
      )
      assertTestComponent(speed, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, resStyle, focused, padding) =>
          align shouldBe TextLine.Right
          pos shouldBe (contentLeft + 30) -> 9
          resWidth shouldBe (contentWidth - 30)
          text shouldBe s"${toSpeed(props.bytesPerSecond * 8)}/s"
          resStyle shouldBe theme
          focused shouldBe false
          padding shouldBe 0
      }

      assertNativeComponent(button,
        <.button(^.rbWidth := 0, ^.rbHeight := 0)()
      )
    }

    assertTestComponent(result, modalComp)({ case ModalProps(title, size, resStyle, onCancel) =>
      title shouldBe "Copy"
      size shouldBe width -> height
      resStyle shouldBe theme
      onCancel should be theSameInstanceAs props.onCancel
    }, inside(_) {
      case List(label, item, to, itemPercent, sep1, total, totalPercent, sep2, time, speed, button) =>
        assertComponents(label, item, to, itemPercent, sep1, total, totalPercent, sep2, time, speed, button)
    })
  }
}
