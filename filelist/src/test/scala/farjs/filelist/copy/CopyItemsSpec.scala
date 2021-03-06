package farjs.filelist.copy

import farjs.filelist.FileListActions.{FileListDirUpdateAction, FileListParamsChangedAction, FileListTaskAction}
import farjs.filelist._
import farjs.filelist.api.{FileListDir, FileListItem}
import farjs.filelist.copy.CopyItems._
import farjs.filelist.popups.FileListPopupsActions.FileListPopupCopyItemsAction
import farjs.filelist.popups.{FileListPopupsProps, FileListPopupsState}
import farjs.ui.popup.MessageBoxProps
import farjs.ui.theme.Theme
import org.scalatest.Succeeded
import scommons.nodejs.test.AsyncTestSpec
import scommons.react._
import scommons.react.redux.task.FutureTask
import scommons.react.test._

import scala.concurrent.{Future, Promise}

class CopyItemsSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  CopyItems.copyItemsStats = () => "CopyItemsStats".asInstanceOf[ReactClass]
  CopyItems.copyItemsPopup = () => "CopyItemsPopup".asInstanceOf[ReactClass]
  CopyItems.copyProcessComp = () => "CopyProcess".asInstanceOf[ReactClass]
  CopyItems.messageBoxComp = () => "MessageBox".asInstanceOf[ReactClass]
  
  it should "show CopyItemsStats when showCopyItemsPopup=true" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val currDir = FileListDir("/folder", isRoot = false, List(
      FileListItem("dir 1", isDir = true)
    ))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    //when
    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())

    //then
    assertTestComponent(renderer.root.children(0), copyItemsStats) {
      case CopyItemsStatsProps(resDispatch, resActions, state, _, _) =>
        resDispatch shouldBe dispatch
        resActions shouldBe actions
        state shouldBe props.data.activeList
    }
  }

  it should "hide CopyItemsStats when onCancel" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val currDir = FileListDir("/folder", isRoot = false, List(
      FileListItem("dir 1", isDir = true)
    ))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))
    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)

    //then
    dispatch.expects(FileListPopupCopyItemsAction(show = false))
    
    //when
    statsPopup.onCancel()

    //then
    TestRenderer.act { () =>
      renderer.update(<(CopyItems())(^.wrapped := props.copy(
        data = FileListsState(popups = FileListPopupsState())
      ))())
    }

    renderer.root.children.toList should be (empty)
  }

  it should "hide CopyItemsPopup when onCancel" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val currDir = FileListDir("/folder", isRoot = false, List(
      FileListItem("dir 1", isDir = true)
    ))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))
    
    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    statsPopup.onDone(123)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)

    //then
    dispatch.expects(FileListPopupCopyItemsAction(show = false))

    //when
    copyPopup.onCancel()
    
    //then
    TestRenderer.act { () =>
      renderer.update(<(CopyItems())(^.wrapped := props.copy(
        data = FileListsState(popups = FileListPopupsState())
      ))())
    }

    renderer.root.children.toList should be (empty)
  }

  it should "dispatch FileListTaskAction if failure when onCopy" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val item = FileListItem("dir 1", isDir = true)
    val currDir = FileListDir("/folder", isRoot = false, List(item))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    val total = 123456789
    statsPopup.onDone(total)
    
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)
    val p = Promise[FileListDir]()
    val to = "test to path"
    (actions.readDir _).expects(Some(currDir.path), to).returning(p.future)
    copyPopup.onCopy(to)

    //then
    var resultF: Future[_] = null
    dispatch.expects(*).onCall { action: Any =>
      inside(action) { case action: FileListTaskAction =>
        action.task.message shouldBe "Resolving target dir"
        resultF = action.task.future
      }
    }

    //when
    p.failure(new Exception("test error"))

    //then
    eventually {
      resultF should not be null
    }.flatMap(_ => resultF.failed).map { _ =>
      Succeeded
    }
  }

  it should "render error popup if same path when onCopy" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val item = FileListItem("dir 1", isDir = true)
    val currDir = FileListDir("/folder", isRoot = false, List(item))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    val total = 123456789
    statsPopup.onDone(total)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)
    val toDir = FileListDir("/folder", isRoot = false, Nil)
    val to = "test to path"

    //then
    (actions.readDir _).expects(Some(currDir.path), to).returning(Future.successful(toDir))
    dispatch.expects(FileListPopupCopyItemsAction(show = false))

    //when
    copyPopup.onCopy(to)

    //then
    TestRenderer.act { () =>
      renderer.update(<(CopyItems())(^.wrapped := props.copy(
        data = FileListsState(
          left = props.data.left,
          popups = FileListPopupsState()
        )
      ))())
    }

    eventually {
      assertTestComponent(renderer.root.children.head, messageBoxComp) {
        case MessageBoxProps(title, message, resActions, style) =>
          title shouldBe "Error"
          message shouldBe s"Cannot copy the item\n${item.name}\nonto itself"
          inside(resActions) { case List(ok) =>
            ok.label shouldBe "OK"
          }
          style shouldBe Theme.current.popup.error
      }
    }
  }

  it should "hide error popup when OK action" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val item = FileListItem("dir 1", isDir = true)
    val currDir = FileListDir("/folder", isRoot = false, List(item))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    val total = 123456789
    statsPopup.onDone(total)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)
    val toDir = FileListDir("/folder", isRoot = false, Nil)
    val to = "test to path"
    
    (actions.readDir _).expects(Some(currDir.path), to).returning(Future.successful(toDir))
    dispatch.expects(FileListPopupCopyItemsAction(show = false))
    copyPopup.onCopy(to)

    TestRenderer.act { () =>
      renderer.update(<(CopyItems())(^.wrapped := props.copy(
        data = FileListsState(
          left = props.data.left,
          popups = FileListPopupsState()
        )
      ))())
    }

    eventually(findProps(renderer.root, messageBoxComp) should not be empty).map { _ =>
      val errorProps = findComponentProps(renderer.root, messageBoxComp)
      
      //when
      errorProps.actions.head.onAction()
      
      //then
      renderer.root.children.toList should be (empty)
    }
  }

  it should "render CopyProcess when onCopy" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val item = FileListItem("dir 1", isDir = true)
    val currDir = FileListDir("/folder", isRoot = false, List(
      item,
      FileListItem("file 1")
    ))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = currDir, isActive = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    val total = 123456789
    statsPopup.onDone(total)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)
    val toDir = FileListDir("/to/path/dir 1", isRoot = false, Nil)
    val to = "test to path"

    //then
    (actions.readDir _).expects(Some(currDir.path), to).returning(Future.successful(toDir))
    dispatch.expects(FileListPopupCopyItemsAction(show = false))

    //when
    copyPopup.onCopy(to)

    //then
    TestRenderer.act { () =>
      renderer.update(<(CopyItems())(^.wrapped := props.copy(
        data = FileListsState(
          left = props.data.left,
          popups = FileListPopupsState()
        )
      ))())
    }

    eventually {
      assertTestComponent(renderer.root.children.head, copyProcessComp) {
        case CopyProcessProps(resDispatch, resActions, fromPath, items, resToPath, resTotal, _, _) =>
          resDispatch shouldBe dispatch
          resActions shouldBe actions
          fromPath shouldBe currDir.path
          items shouldBe List(item)
          resToPath shouldBe toDir.path
          resTotal shouldBe total
      }
    }
  }

  it should "dispatch FileListParamsChangedAction if selected when onDone" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val dir = FileListItem("dir 1", isDir = true)
    val leftDir = FileListDir("/left/dir", isRoot = false, List(
      FileListItem.up,
      dir,
      FileListItem("file 1")
    ))
    val rightDir = FileListDir("/right/dir", isRoot = false, List(FileListItem("dir 2", isDir = true)))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(index = 1, currDir = leftDir, isActive = true, selectedNames = Set(dir.name, "file 1")),
      right = FileListState(currDir = rightDir, isRight = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    statsPopup.onDone(123)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)

    val toDir = FileListDir("/to/path/dir 1", isRoot = false, Nil)
    val to = "test to path"
    (actions.readDir _).expects(Some(leftDir.path), to).returning(Future.successful(toDir))
    dispatch.expects(FileListPopupCopyItemsAction(show = false))
    copyPopup.onCopy(to)
    
    eventually {
      findProps(renderer.root, copyProcessComp) should not be empty
    }.flatMap { _ =>
      val progressPopup = findComponentProps(renderer.root, copyProcessComp)
      progressPopup.onTopItem(dir)

      val updatedDir = FileListDir("/updated/dir", isRoot = false, List(
        FileListItem("file 1")
      ))
      val leftAction = FileListDirUpdateAction(FutureTask("Updating", Future.successful(updatedDir)))
      val rightAction = FileListDirUpdateAction(FutureTask("Updating", Future.successful(updatedDir)))

      //then
      dispatch.expects(FileListParamsChangedAction(
        isRight = false,
        offset = 0,
        index = 1,
        selectedNames = Set("file 1")
      ))
      (actions.updateDir _).expects(dispatch, false, leftDir.path).returning(leftAction)
      (actions.updateDir _).expects(dispatch, true, rightDir.path).returning(rightAction)
      dispatch.expects(leftAction)
      dispatch.expects(rightAction)

      //when
      progressPopup.onDone()

      //then
      findProps(renderer.root, copyProcessComp) should be(empty)

      for {
        _ <- leftAction.task.future
        _ <- rightAction.task.future
      } yield Succeeded
    }
  }

  it should "not dispatch FileListParamsChangedAction if not selected when onDone" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val dir = FileListItem("dir 1", isDir = true)
    val leftDir = FileListDir("/left/dir", isRoot = false, List(
      FileListItem.up,
      dir,
      FileListItem("file 1")
    ))
    val rightDir = FileListDir("/right/dir", isRoot = false, List(FileListItem("dir 2", isDir = true)))
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(index = 1, currDir = leftDir, isActive = true, selectedNames = Set("file 1")),
      right = FileListState(currDir = rightDir, isRight = true),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)
    statsPopup.onDone(123)
    val copyPopup = findComponentProps(renderer.root, copyItemsPopup)

    val toDir = FileListDir("/to/path/dir 1", isRoot = false, Nil)
    val to = "test to path"
    (actions.readDir _).expects(Some(leftDir.path), to).returning(Future.successful(toDir))
    dispatch.expects(FileListPopupCopyItemsAction(show = false))
    copyPopup.onCopy("test to path")

    eventually {
      findProps(renderer.root, copyProcessComp) should not be empty
    }.flatMap { _ =>
      val progressPopup = findComponentProps(renderer.root, copyProcessComp)
      progressPopup.onTopItem(dir)

      val updatedDir = FileListDir("/updated/dir", isRoot = false, List(
        FileListItem("file 1")
      ))
      val leftAction = FileListDirUpdateAction(FutureTask("Updating", Future.successful(updatedDir)))
      val rightAction = FileListDirUpdateAction(FutureTask("Updating", Future.successful(updatedDir)))

      //then
      dispatch.expects(FileListParamsChangedAction(
        isRight = false,
        offset = 0,
        index = 1,
        selectedNames = Set("file 1")
      )).never()
      (actions.updateDir _).expects(dispatch, false, leftDir.path).returning(leftAction)
      (actions.updateDir _).expects(dispatch, true, rightDir.path).returning(rightAction)
      dispatch.expects(leftAction)
      dispatch.expects(rightAction)

      //when
      progressPopup.onDone()

      //then
      findProps(renderer.root, copyProcessComp) should be(empty)

      for {
        _ <- leftAction.task.future
        _ <- rightAction.task.future
      } yield Succeeded
    }
  }

  it should "render empty component when showCopyItemsPopup=false" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListPopupsProps(dispatch, actions, FileListsState())

    //when
    val result = createTestRenderer(<(CopyItems())(^.wrapped := props)()).root

    //then
    result.children.toList should be (empty)
  }
  
  it should "render CopyItemsPopup(items=single)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val item = FileListItem("dir 1", isDir = true)
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = FileListDir("/folder", isRoot = false, List(item)), isActive = true),
      right = FileListState(currDir = FileListDir("/test-path", isRoot = false, Nil)),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))
    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)

    //when
    statsPopup.onDone(123)

    //then
    assertTestComponent(renderer.root.children(0), copyItemsPopup) {
      case CopyItemsPopupProps(path, items, _, _) =>
        path shouldBe "/test-path"
        items shouldBe Seq(item)
    }
  }

  it should "render CopyItemsPopup(items=multiple)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val items = List(
      FileListItem("file 1"),
      FileListItem("dir 1", isDir = true)
    )
    val props = FileListPopupsProps(dispatch, actions, FileListsState(
      left = FileListState(currDir = FileListDir("/folder", isRoot = false, Nil)),
      right = FileListState(
        currDir = FileListDir("/test-path", isRoot = false, items),
        isActive = true,
        selectedNames = Set("file 1", "dir 1")
      ),
      popups = FileListPopupsState(showCopyItemsPopup = true)
    ))

    val renderer = createTestRenderer(<(CopyItems())(^.wrapped := props)())
    val statsPopup = findComponentProps(renderer.root, copyItemsStats)

    //when
    statsPopup.onDone(123)

    //then
    assertTestComponent(renderer.root.children(0), copyItemsPopup) {
      case CopyItemsPopupProps(path, resItems, _, _) =>
        path shouldBe "/folder"
        resItems shouldBe items
    }
  }
}
