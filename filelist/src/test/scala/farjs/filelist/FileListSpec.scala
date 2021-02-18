package farjs.filelist

import farjs.filelist.FileList._
import farjs.filelist.FileListActions._
import farjs.filelist.FileListSpec._
import farjs.filelist.api.{FileListDir, FileListItem}
import farjs.filelist.popups.FileListPopupsActions
import org.scalactic.source.Position
import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.path
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.ReactClass
import scommons.react.blessed.BlessedScreen
import scommons.react.redux.task.FutureTask
import scommons.react.test._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

class FileListSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  FileList.fileListViewComp = () => "FileListView".asInstanceOf[ReactClass]

  it should "dispatch popups actions when F1-F10 keys" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(
      currDir = FileListDir("/sub-dir", isRoot = false, items = List(
        FileListItem.up,
        FileListItem("file 1"),
        FileListItem("dir 1", isDir = true)
      )) 
    )
    val props = FileListProps(dispatch, actions, state, (5, 5), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := props)())

    def check(fullKey: String,
              action: Any,
              index: Int = 0,
              selectedNames: Set[String] = Set.empty,
              never: Boolean = false): Unit = {
      //given
      renderer.update(<(FileList())(^.wrapped := props.copy(
        state = props.state.copy(index = index, selectedNames = selectedNames)
      ))())
      
      //then
      if (never) dispatch.expects(action).never()
      else dispatch.expects(action)
      
      //when
      findComponentProps(renderer.root, fileListViewComp).onKeypress(null, fullKey)
    }

    dirAction.task.future.map { _ =>
      //when & then
      check("f1", FileListPopupsActions.FileListPopupHelpAction(show = true))
      check("f3", FileListPopupsActions.FileListPopupViewItemsAction(show = true), never = true)
      check("f3", FileListPopupsActions.FileListPopupViewItemsAction(show = true), index = 1, never = true)
      check("f3", FileListPopupsActions.FileListPopupViewItemsAction(show = true), index = 1, selectedNames = Set("file 1"))
      check("f3", FileListPopupsActions.FileListPopupViewItemsAction(show = true), index = 2)
      check("f7", FileListPopupsActions.FileListPopupMkFolderAction(show = true))
      check("f8", FileListPopupsActions.FileListPopupDeleteAction(show = true), never = true)
      check("f8", FileListPopupsActions.FileListPopupDeleteAction(show = true), index = 1)
      check("delete", FileListPopupsActions.FileListPopupDeleteAction(show = true), never = true)
      check("delete", FileListPopupsActions.FileListPopupDeleteAction(show = true), selectedNames = Set("file 1"))
      check("f10", FileListPopupsActions.FileListPopupExitAction(show = true))
      
      Succeeded
    }
  }
  
  it should "dispatch action only once when mount but not when update" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state1 = FileListState(
      currDir = FileListDir("/sub-dir", isRoot = false, items = List(FileListItem("item 1")))
    )
    val props1 = FileListProps(dispatch, actions, state1, (7, 2), columns = 2)
    val state2 = state1.copy(
      currDir = FileListDir("/changed", isRoot = false, items = List(FileListItem("item 2")))
    )
    val props2 = props1.copy(state = state2)
    val action = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(state1.currDir))
    )
    
    //then
    (actions.changeDir _).expects(dispatch, state1.isRight, None, FileListDir.curr).returning(action)
    dispatch.expects(action)
    
    //when
    val renderer = createTestRenderer(<(FileList())(^.wrapped := props1)())
    renderer.update(<(FileList())(^.wrapped := props2)()) //noop
    
    //cleanup
    renderer.unmount()

    action.task.future.map(_ => Succeeded)
  }

  it should "copy parent path into clipboard when onKeypress(C-c)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/sub-dir", isRoot = false, items = List(FileListItem("..")))
    ), (7, 2), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    val screenMock = mock[BlessedScreenMock]    
    
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)
    
    //then
    (screenMock.copyToClipboard _).expects("/sub-dir")
    
    val result = testRender(<(FileList())(^.wrapped := props)())
    val List(viewProps) = findProps(result, fileListViewComp)
    
    //when
    viewProps.onKeypress(screenMock.asInstanceOf[BlessedScreen], "C-c")
    
    dirAction.task.future.map(_ => Succeeded)
  }

  it should "copy item path into clipboard when onKeypress(C-c)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/sub-dir", isRoot = false, items = List(FileListItem("item 1")))
    ), (7, 2), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    val screenMock = mock[BlessedScreenMock]    
    
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)
    
    //then
    (screenMock.copyToClipboard _).expects(path.join("/sub-dir", "item 1"))
    
    val result = testRender(<(FileList())(^.wrapped := props)())
    val List(viewProps) = findProps(result, fileListViewComp)
    
    //when
    viewProps.onKeypress(screenMock.asInstanceOf[BlessedScreen], "C-c")
    
    dirAction.task.future.map(_ => Succeeded)
  }

  it should "dispatch action when onKeypress(M-pagedown)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/sub-dir", isRoot = false, items = List(FileListItem("item 1")))
    ), (7, 2), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    val openAction = FileListOpenInDefaultAppAction(
      FutureTask("Opening item", Future.successful((new js.Object, new js.Object)))
    )
    
    //then
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    (actions.openInDefaultApp _).expects("/sub-dir", "item 1").returning(openAction)
    dispatch.expects(dirAction)
    dispatch.expects(openAction)
    
    val result = testRender(<(FileList())(^.wrapped := props)())
    val List(viewProps) = findProps(result, fileListViewComp)
    
    //when
    viewProps.onKeypress(null, "M-pagedown")
    
    dirAction.task.future.flatMap(_ => openAction.task.future).map(_ => Succeeded)
  }

  it should "dispatch action when onKeypress(enter | C-pageup | C-pagedown)" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("dir 1", isDir = true),
        FileListItem("dir 2", isDir = true),
        FileListItem("dir 3", isDir = true),
        FileListItem("dir 4", isDir = true),
        FileListItem("dir 5", isDir = true),
        FileListItem("dir 6", isDir = true),
        FileListItem("file 7")
      )),
      isActive = true
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := props)())
    findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe 0

    def prepare(offset: Int, index: Int, currDir: String, items: Seq[FileListItem]): Future[Assertion] = Future {
      renderer.update(<(FileList())(^.wrapped := props.copy(state = props.state.copy(
        offset = offset,
        index = index,
        currDir = FileListDir(currDir, currDir == "/", items = items)
      )))())

      Succeeded
    }

    def check(keyFull: String,
              parent: String,
              pressItem: String,
              items: List[String],
              offset: Int,
              index: Int,
              changed: Boolean = true
             )(implicit pos: Position): Future[Assertion] = {

      val currDirPath =
        if (changed) {
          if (pressItem == FileListItem.up.name) {
            val index = parent.lastIndexOf('/')
            parent.take(if (index > 0) index else 1)
          }
          else if (parent == "/") s"$parent$pressItem"
          else s"$parent/$pressItem"
        }
        else parent
      
      val isRoot = currDirPath == "/"
      val currDir = FileListDir(currDirPath, isRoot, items =
        if (isRoot) props.state.currDir.items
        else FileListItem.up +: props.state.currDir.items
      )
      val state = props.state.copy(offset = 0, index = offset + index, currDir = currDir)
      val checkF =
        if (changed) {
          val action = FileListDirChangeAction(
            FutureTask("Changing dir", Future.successful(currDir))
          )

          //then
          (actions.changeDir _).expects(dispatch, state.isRight, Some(parent), pressItem).returning(action)
          dispatch.expects(action)

          action.task.future.map(_ => Succeeded)
        }
        else Future.successful(Succeeded)

      Future {
        //when
        findComponentProps(renderer.root, fileListViewComp).onKeypress(null, keyFull)
        renderer.update(<(FileList())(^.wrapped := props.copy(state = state))())

        //then
        val res = findComponentProps(renderer.root, fileListViewComp)
        val viewItems = items.map(name => FileListItem(
          name = name,
          isDir = name == FileListItem.up.name || name.startsWith("dir")
        ))
        (res.items.toList, res.focusedIndex) shouldBe ((viewItems, index))
      }.flatMap(_ => checkF)
    }

    val resF = Future.sequence(List(
      //when & then
      check("unknown", "/",    "123",         List("dir 1", "dir 2", "dir 3", "dir 4"), 0, 0, changed = false),

      check("C-pageup", "/",   "..",          List("dir 1", "dir 2", "dir 3", "dir 4"), 0, 0),
      check("C-pagedown", "/", "dir 1",       List("..", "dir 1", "dir 2", "dir 3"), 0, 0),
      check("C-pageup", "/dir 1", "..",       List("dir 1", "dir 2", "dir 3", "dir 4"), 0, 0),
      
      check("enter", "/",      "dir 1",       List("..", "dir 1", "dir 2", "dir 3"), 0, 0),
      check("enter", "/dir 1", "..",          List("dir 1", "dir 2", "dir 3", "dir 4"), 0, 0),
      
      prepare(3, 3, "/", props.state.currDir.items),
      check("enter", "/",      "file 7",      List("dir 5", "dir 6", "file 7"), 4, 2, changed = false),
      check("C-pagedown", "/", "file 7",      List("dir 5", "dir 6", "file 7"), 4, 2, changed = false),
      check("C-pageup", "/",   "..",          List("dir 1", "dir 2", "dir 3", "dir 4"), 0, 0),
      
      prepare(3, 2, "/", props.state.currDir.items),
      check("enter", "/",      "dir 6",       List("..", "dir 1", "dir 2", "dir 3"), 0, 0),
      
      prepare(3, 1, "/dir 6", FileListItem.up +: props.state.currDir.items),
      check("enter", "/dir 6",       "dir 4", List("..", "dir 1", "dir 2", "dir 3"), 0, 0),
      check("enter", "/dir 6/dir 4", "..",    List("dir 4", "dir 5", "dir 6", "file 7"), 4, 0),
      
      prepare(0, 0, "/dir 6", FileListItem.up +: props.state.currDir.items),
      check("enter", "/dir 6",       "..",    List("dir 5", "dir 6", "file 7"), 4, 1)
      
    ))
    dirAction.task.future.flatMap { _ =>
      resF.map(_ => Succeeded)
    }
  }

  it should "dispatch action when onActivate" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("item 1"),
        FileListItem("item 2")
      ))
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := props)())
    findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe -1

    def check(active: Boolean, changed: Boolean = true)(implicit pos: Position): Assertion = {
      val state = props.state.copy(isActive = active)
      if (changed) {
        //then
        dispatch.expects(FileListActivateAction(isRight = state.isRight))
      }
      
      //when
      findComponentProps(renderer.root, fileListViewComp).onActivate()
      renderer.update(<(FileList())(^.wrapped := props.copy(state = state))())

      //then
      val res = findComponentProps(renderer.root, fileListViewComp)
      res.focusedIndex shouldBe {
        if (active) 0
        else -1
      }
    }

    dirAction.task.future.map { _ =>
      //when & then
      check(active = true)
      check(active = true, changed = false)

      //when & then
      check(active = false)
      check(active = false, changed = false)
    }
  }

  it should "focus item when onWheel and active" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("item 1"),
        FileListItem("item 2"),
        FileListItem("item 3"),
        FileListItem("item 4"),
        FileListItem("item 5")
      )),
      isActive = true
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := props)())
    findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe 0

    def check(up: Boolean, offset: Int, index: Int, changed: Boolean = true)(implicit pos: Position): Assertion = {
      val state = props.state.copy(offset = offset, index = index)
      if (changed) {
        //then
        dispatch.expects(FileListParamsChangedAction(state.isRight, offset, index, Set.empty))
      }
      
      //when
      findComponentProps(renderer.root, fileListViewComp).onWheel(up)
      renderer.update(<(FileList())(^.wrapped := props.copy(state = state))())

      //then
      val res = findComponentProps(renderer.root, fileListViewComp)
      res.focusedIndex shouldBe index
    }

    dirAction.task.future.map { _ =>
      //when & then
      check(up = false, offset = 1, index = 0)
      check(up = false, offset = 1, index = 1)
      check(up = false, offset = 1, index = 2)
      check(up = false, offset = 1, index = 3)
      check(up = false, offset = 1, index = 3, changed = false)

      //when & then
      check(up = true, offset = 0, index = 3)
      check(up = true, offset = 0, index = 2)
      check(up = true, offset = 0, index = 1)
      check(up = true, offset = 0, index = 0)
      check(up = true, offset = 0, index = 0, changed = false)
    }
  }

  it should "not focus item when onWheel and not active" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("item 1"),
        FileListItem("item 2")
      ))
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val comp = testRender(<(FileList())(^.wrapped := props)())
    val viewProps = findComponentProps(comp, fileListViewComp)
    viewProps.focusedIndex shouldBe -1

    //then
    dispatch.expects(*).never()
    
    //when
    viewProps.onWheel(false)
    viewProps.onWheel(true)
    
    dirAction.task.future.map(_ => Succeeded)
  }

  it should "focus item when onClick" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("item 1"),
        FileListItem("item 2"),
        FileListItem("item 3")
      )),
      isActive = true
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := props)())
    findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe 0

    def check(clickIndex: Int, index: Int, changed: Boolean = true)(implicit pos: Position): Assertion = {
      val state = props.state.copy(offset = 0, index = index)
      if (changed) {
        //then
        dispatch.expects(FileListParamsChangedAction(state.isRight, 0, index, Set.empty))
      }

      //when
      findComponentProps(renderer.root, fileListViewComp).onClick(clickIndex)
      renderer.update(<(FileList())(^.wrapped := props.copy(state = state))())

      //then
      val res = findComponentProps(renderer.root, fileListViewComp)
      res.focusedIndex shouldBe index
    }

    dirAction.task.future.map { _ =>
      //when & then
      check(clickIndex = 0, index = 0, changed = false) // first item in col 1
      check(clickIndex = 1, index = 1) // second item in col 1
      check(clickIndex = 2, index = 2) // first item in col 2
      check(clickIndex = 3, index = 2, changed = false) // last item in col 2
    }
  }

  it should "focus and select item when onKeypress" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val items = List(
      FileListItem("item 1"),
      FileListItem("item 2"),
      FileListItem("item 3"),
      FileListItem("item 4"),
      FileListItem("item 5"),
      FileListItem("item 6"),
      FileListItem("item 7")
    )
    val rootProps = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = items),
      isActive = true
    ), (7, 3), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(rootProps.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, rootProps.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    val renderer = createTestRenderer(<(FileList())(^.wrapped := rootProps)())
    findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe 0
    
    def check(keyFull: String,
              items: List[String],
              offset: Int,
              index: Int,
              selected: Set[String],
              changed: Boolean = true,
              props: FileListProps = rootProps
             )(implicit pos: Position): Assertion = {

      val state = props.state.copy(offset = offset, index = index, selectedNames = selected)
      if (changed) {
        //then
        dispatch.expects(FileListParamsChangedAction(state.isRight, offset, index, selected))
      }
      
      //when
      findComponentProps(renderer.root, fileListViewComp).onKeypress(null, keyFull)
      renderer.update(<(FileList())(^.wrapped := props.copy(state = state))())

      //then
      val res = findComponentProps(renderer.root, fileListViewComp)
      val viewItems = items.map(name => FileListItem(name, isDir = name == FileListItem.up.name))
      (res.items, res.focusedIndex, res.selectedNames) shouldBe ((viewItems, index, selected))
    }

    dirAction.task.future.map { _ =>
      //when & then
      check("unknown", List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty, changed = false)
      
      //when & then
      check("S-down",  List("item 1", "item 2", "item 3", "item 4"), 0, 1, Set("item 1"))
      check("S-down",  List("item 1", "item 2", "item 3", "item 4"), 0, 2, Set("item 1", "item 2"))
      check("down",    List("item 1", "item 2", "item 3", "item 4"), 0, 3, Set("item 1", "item 2"))
      check("down",    List("item 2", "item 3", "item 4", "item 5"), 1, 3, Set("item 1", "item 2"))
      check("S-down",  List("item 3", "item 4", "item 5", "item 6"), 2, 3, Set("item 1", "item 2", "item 5"))
      check("S-down",  List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 5", "item 6", "item 7"))
      check("down",    List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 5", "item 6", "item 7"), changed = false)
  
      //when & then
      check("S-up",    List("item 4", "item 5", "item 6", "item 7"), 3, 2, Set("item 1", "item 2", "item 5", "item 6"))
      check("S-up",    List("item 4", "item 5", "item 6", "item 7"), 3, 1, Set("item 1", "item 2", "item 5"))
      check("S-up",    List("item 4", "item 5", "item 6", "item 7"), 3, 0, Set("item 1", "item 2"))
      check("up",      List("item 3", "item 4", "item 5", "item 6"), 2, 0, Set("item 1", "item 2"))
      check("up",      List("item 2", "item 3", "item 4", "item 5"), 1, 0, Set("item 1", "item 2"))
      check("S-up",    List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty)
      check("up",      List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty, changed = false)
  
      //when & then
      check("S-right", List("item 1", "item 2", "item 3", "item 4"), 0, 2, Set("item 1", "item 2"))
      check("right",   List("item 3", "item 4", "item 5", "item 6"), 2, 2, Set("item 1", "item 2"))
      check("S-right", List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 5", "item 6", "item 7"))
      check("right",   List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 5", "item 6", "item 7"), changed = false)
  
      //when & then
      check("S-left",  List("item 4", "item 5", "item 6", "item 7"), 3, 1, Set("item 1", "item 2", "item 5"))
      check("left",    List("item 2", "item 3", "item 4", "item 5"), 1, 1, Set("item 1", "item 2", "item 5"))
      check("S-left",  List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set("item 1", "item 2", "item 3", "item 5"))
      check("left",    List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set("item 1", "item 2", "item 3", "item 5"), changed = false)
  
      //when & then
      check("S-pagedown", List("item 1", "item 2", "item 3", "item 4"), 0, 3, Set("item 5"))
      check("S-pagedown", List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 4", "item 5", "item 6", "item 7"))
      check("pagedown",   List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 4", "item 5", "item 6", "item 7"), changed = false)
  
      //when & then
      check("S-pageup",List("item 4", "item 5", "item 6", "item 7"), 3, 0, Set("item 4"))
      check("S-pageup",List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty)
      check("pageup",  List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty, changed = false)
  
      //when & then
      check("end",     List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set.empty)
      check("end",     List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set.empty, changed = false)
  
      //when & then
      check("home",    List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty)
      check("home",    List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty, changed = false)
      
      //when & then
      check("S-end",   List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 3", "item 4", "item 5", "item 6", "item 7"))
      check("S-end",   List("item 4", "item 5", "item 6", "item 7"), 3, 3, Set("item 1", "item 2", "item 3", "item 4", "item 5", "item 6", "item 7"), changed = false)
  
      //when & then
      check("S-home",  List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty)
      check("S-home",  List("item 1", "item 2", "item 3", "item 4"), 0, 0, Set.empty, changed = false)
  
      //given
      val nonRootProps = rootProps.copy(state = rootProps.state.copy(
        currDir = rootProps.state.currDir.copy(items = FileListItem.up +: items)
      ))
      renderer.update(<(FileList())(^.wrapped := nonRootProps)())
      findComponentProps(renderer.root, fileListViewComp).focusedIndex shouldBe 0
  
      //when & then
      check("S-down",  List("..", "item 1", "item 2", "item 3"), 0, 1, Set.empty, props = nonRootProps)
      check("S-down",  List("..", "item 1", "item 2", "item 3"), 0, 2, Set("item 1"), props = nonRootProps)
      check("up",      List("..", "item 1", "item 2", "item 3"), 0, 1, Set("item 1"), props = nonRootProps)
      check("S-up",    List("..", "item 1", "item 2", "item 3"), 0, 0, Set.empty, props = nonRootProps)
    }
  }

  it should "render empty component" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(), (7, 2), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    //when
    val result = testRender(<(FileList())(^.wrapped := props)())

    //then
    dirAction.task.future.map { _ =>
      assertFileList(result, props,
        viewItems = Nil,
        focusedIndex = -1,
        selectedNames = Set.empty
      )
    }
  }
  
  it should "render non-empty component" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListProps(dispatch, actions, FileListState(
      currDir = FileListDir("/", isRoot = true, items = List(
        FileListItem("item 1"),
        FileListItem("item 2"),
        FileListItem("item 3")
      )),
      isActive = true
    ), (7, 2), columns = 2)
    val dirAction = FileListDirChangeAction(
      FutureTask("Changing dir", Future.successful(props.state.currDir))
    )
    (actions.changeDir _).expects(dispatch, props.state.isRight, None, FileListDir.curr).returning(dirAction)
    dispatch.expects(dirAction)

    //when
    val result = testRender(<(FileList())(^.wrapped := props)())

    //then
    dirAction.task.future.map { _ =>
      assertFileList(result, props,
        viewItems = List(FileListItem("item 1"), FileListItem("item 2")),
        focusedIndex = 0,
        selectedNames = Set.empty
      )
    }
  }
  
  private def assertFileList(result: TestInstance,
                             props: FileListProps,
                             viewItems: List[FileListItem],
                             focusedIndex: Int,
                             selectedNames: Set[Int]): Assertion = {
    
    assertTestComponent(result, fileListViewComp) {
      case FileListViewProps(resSize, columns, items, resFocusedIndex, resSelectedNames, _, _, _, _) =>
        resSize shouldBe props.size
        columns shouldBe props.columns
        items shouldBe viewItems
        resFocusedIndex shouldBe focusedIndex
        resSelectedNames shouldBe selectedNames
    }
  }
}

object FileListSpec {

  @JSExportAll
  trait BlessedScreenMock {

    def copyToClipboard(text: String): Boolean
  }
}