package farjs.filelist.fs

import farjs.filelist.FileListActions
import farjs.filelist.FileListActions.{FileListDirChangeAction, FileListTaskAction}
import farjs.filelist.api.{FileListDir, FileListItem}
import farjs.filelist.fs.FSDrivePopup._
import farjs.filelist.stack.{PanelStack, PanelStackProps}
import farjs.ui._
import farjs.ui.popup.{ModalContentProps, PopupProps}
import farjs.ui.theme.Theme
import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.Process.Platform
import scommons.nodejs.test.AsyncTestSpec
import scommons.react._
import scommons.react.blessed._
import scommons.react.redux.task.FutureTask
import scommons.react.test._

import scala.concurrent.Future

class FSDrivePopupSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  FSDrivePopup.popupComp = () => "Popup".asInstanceOf[ReactClass]
  FSDrivePopup.modalContentComp = () => "ModalContent".asInstanceOf[ReactClass]
  FSDrivePopup.buttonComp = () => "Button".asInstanceOf[ReactClass]

  it should "dispatch FileListDirChangeAction and call onClose when onPress item" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val onClose = mockFunction[Unit]
    val fsService = mock[FSService]
    FSDrivePopup.platform = Platform.win32
    FSDrivePopup.fsService = fsService
    val props = FSDrivePopupProps(dispatch, actions, isRight = false, onClose, showOnLeft = true)

    var disksF: Future[_] = null
    dispatch.expects(*).onCall { action =>
      disksF = action.asInstanceOf[FileListTaskAction].task.future
    }
    (fsService.readDisks _).expects().returning(Future.successful(List(
      FSDisk("C:", size = 156595318784.0, free = 81697124352.0, "SYSTEM"),
      FSDisk("D:", size = 842915639296.0, free = 352966430720.0, "DATA"),
      FSDisk("E:", size = 0.0, free = 0.0, "")
    )))

    //when & then
    val renderer = createTestRenderer(withContext(<(FSDrivePopup())(^.wrapped := props)()))
    renderer.root.children.isEmpty shouldBe true
    
    eventually {
      disksF should not be null
    }.flatMap(_ => disksF).map { _ =>
      inside(findProps(renderer.root, buttonComp)) {
        case List(item1, _, _) =>
          //given
          val action = FileListDirChangeAction(FutureTask("Changing Dir",
            Future.successful(FileListDir("/", isRoot = true, items = List.empty[FileListItem]))
          ))

          //then
          (actions.changeDir _).expects(dispatch, props.isRight, None, "C:").returning(action)
          dispatch.expects(action)
          onClose.expects()
          
          //when
          item1.onPress()
          
          Succeeded
      }
    }
  }

  it should "render component on Windows" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val fsService = mock[FSService]
    FSDrivePopup.platform = Platform.win32
    FSDrivePopup.fsService = fsService
    val props = FSDrivePopupProps(dispatch, actions, isRight = false, onClose = () => (), showOnLeft = true)

    var disksF: Future[_] = null
    dispatch.expects(*).onCall { action =>
      disksF = action.asInstanceOf[FileListTaskAction].task.future
    }
    (fsService.readDisks _).expects().returning(Future.successful(List(
      FSDisk("C:", size = 156595318784.0, free = 81697124352.0, "SYSTEM"),
      FSDisk("D:", size = 842915639296.0, free = 352966430720.0, "DATA"),
      FSDisk("E:", size = 0.0, free = 0.0, "")
    )))

    //when
    val renderer = createTestRenderer(withContext(<(FSDrivePopup())(^.wrapped := props)()))

    //then
    renderer.root.children.isEmpty shouldBe true
    
    eventually {
      disksF should not be null
    }.flatMap(_ => disksF).map { _ =>
      //then
      assertFSDrivePopup(renderer.root.children.head, props, List(
        "  C: │SYSTEM         │149341 M│ 77912 M ",
        "  D: │DATA           │803867 M│336615 M ",
        "  E: │               │        │         "
      ))
    }
  }

  it should "render component on Mac OS/Linux" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val fsService = mock[FSService]
    FSDrivePopup.platform = Platform.darwin
    FSDrivePopup.fsService = fsService
    val props = FSDrivePopupProps(dispatch, actions, isRight = false, onClose = () => (), showOnLeft = true)

    var disksF: Future[_] = null
    dispatch.expects(*).onCall { action =>
      disksF = action.asInstanceOf[FileListTaskAction].task.future
    }
    (fsService.readDisks _).expects().returning(Future.successful(List(
      FSDisk("/", size = 156595318784.0, free = 81697124352.0, "/"),
      FSDisk("/Volumes/TestDrive", size = 842915639296.0, free = 352966430720.0, "TestDrive")
    )))

    //when
    val renderer = createTestRenderer(withContext(<(FSDrivePopup())(^.wrapped := props)()))

    //then
    renderer.root.children.isEmpty shouldBe true
    
    eventually {
      disksF should not be null
    }.flatMap(_ => disksF).map { _ =>
      //then
      assertFSDrivePopup(renderer.root.children.head, props, List(
        " /              │149341 M│ 77912 M ",
        " TestDrive      │803867 M│336615 M "
      ))
    }
  }

  it should "left pos when getLeftPos" in {
    //when & then
    getLeftPos(10, showOnLeft = true, 5) shouldBe "0%+2"
    getLeftPos(5, showOnLeft = true, 5) shouldBe "0%+0"
    getLeftPos(5, showOnLeft = true, 10) shouldBe "0%+0"
    getLeftPos(5, showOnLeft = false, 5) shouldBe "50%+0"
    getLeftPos(10, showOnLeft = false, 5) shouldBe "50%+2"
    getLeftPos(5, showOnLeft = false, 10) shouldBe "50%-5"
    getLeftPos(5, showOnLeft = false, 11) shouldBe "0%+0"
  }

  it should "convert bytes to compact form when toCompact" in {
    //when & then
    toCompact(0) shouldBe ""
    toCompact(1000d * 1024d) shouldBe "1024000"
    toCompact(1000d * 1024d + 1) shouldBe "1000 K"
    toCompact(1000d * 1024d * 1024d) shouldBe "1024000 K"
    toCompact(1000d * 1024d * 1024d + 1) shouldBe "1000 M"
    toCompact(1000d * 1024d * 1024d * 1024d) shouldBe "1024000 M"
    toCompact(1000d * 1024d * 1024d * 1024d + 1) shouldBe "1000 G"
  }

  private def withContext(element: ReactElement, panelInput: BlessedElement = null): ReactElement = {
    <(PanelStack.Context.Provider)(^.contextValue := PanelStackProps(isRight = false, panelInput))(
      element
    )
  }

  private def assertFSDrivePopup(result: TestInstance,
                                 props: FSDrivePopupProps,
                                 expectedItems: List[String]): Assertion = {
    
    val textWidth = expectedItems.maxBy(_.length).length
    val width = textWidth + 3 * 2
    val height = 2 * 2 + expectedItems.size
    val theme = Theme.current.popup.menu

    assertTestComponent(result, popupComp)({
      case PopupProps(onClose, closable, focusable, _) =>
        onClose should be theSameInstanceAs props.onClose
        closable shouldBe true
        focusable shouldBe true
    }, inside(_) { case List(content) =>
      var resSize = 0 -> 0
      assertTestComponent(content, modalContentComp)({
        case ModalContentProps(title, size, style, padding, left) =>
          title shouldBe "Drive"
          resSize = size
          style shouldBe theme
          padding shouldBe FSDrivePopup.padding
          left shouldBe "0%+0"
      }, inside(_) { case lines =>
        lines.size shouldBe expectedItems.size
        lines.zipWithIndex.zip(expectedItems).foreach { case ((line, index), expected) =>
          assertTestComponent(line, buttonComp) {
            case ButtonProps(pos, label, resStyle, _) =>
              pos shouldBe 1 -> (1 + index)
              label shouldBe expected
              resStyle shouldBe theme
          }
        }

        resSize shouldBe width -> height
      })
    })
  }
}
