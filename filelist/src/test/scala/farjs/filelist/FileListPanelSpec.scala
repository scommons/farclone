package farjs.filelist

import farjs.filelist.FileListPanel._
import farjs.filelist.api.{FileListDir, FileListItem}
import farjs.ui._
import farjs.ui.border._
import farjs.ui.theme.Theme
import org.scalatest.Assertion
import scommons.react._
import scommons.react.blessed._
import scommons.react.test._

class FileListPanelSpec extends TestSpec with TestRendererUtils {

  FileListPanel.withSizeComp = () => "WithSize".asInstanceOf[ReactClass]
  FileListPanel.doubleBorderComp = () => "DoubleBorder".asInstanceOf[ReactClass]
  FileListPanel.horizontalLineComp = () => "HorizontalLine".asInstanceOf[ReactClass]
  FileListPanel.fileListComp = () => "FileList".asInstanceOf[ReactClass]
  FileListPanel.textLineComp = () => "TextLine".asInstanceOf[ReactClass]

  it should "render empty component" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val props = FileListPanelProps(dispatch, actions, FileListState())

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, FileListState())
  }
  
  it should "render component with selected one file" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(index = 2, currDir = FileListDir("/", isRoot = true, items = List(
      FileListItem("dir 1", isDir = true, size = 1),
      FileListItem("dir 2", isDir = true, size = 2),
      FileListItem("file", size = 3)
    )), selectedNames = Set("dir 2"))
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "file", "3", showDate = true,
      selected = Some("2 in 1 file"), dirSize = "3 (1)")
  }
  
  it should "render component with selected more than one file" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(index = 2, currDir = FileListDir("/", isRoot = true, items = List(
      FileListItem("dir 1", isDir = true, size = 1),
      FileListItem("dir 2", isDir = true, size = 2),
      FileListItem("file", size = 3)
    )), selectedNames = Set("dir 2", "file"))
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "file", "3", showDate = true,
      selected = Some("5 in 2 files"), dirSize = "3 (1)")
  }
  
  it should "render active component with root dir and focused file" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(index = 1, currDir = FileListDir("/", isRoot = true, items = List(
      FileListItem("file 1", size = 1),
      FileListItem("file 2", size = 2, permissions = "drwxr-xr-x"),
      FileListItem("file 3", size = 3)
    )), isActive = true)
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "file 2", "2", permissions = "drwxr-xr-x", showDate = true, dirSize = "6 (3)")
  }
  
  it should "render component with root dir and focused file" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(index = 1, currDir = FileListDir("/", isRoot = true, items = List(
      FileListItem("file 1", size = 1),
      FileListItem("file 2", size = 2, permissions = "drwxr-xr-x"),
      FileListItem("file 3", size = 3)
    )))
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "file 2", "2", permissions = "drwxr-xr-x", showDate = true, dirSize = "6 (3)")
  }
  
  it should "render component with sub-dir and focused dir" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(index = 1, currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem.up,
      FileListItem("dir", isDir = true, size = 1, permissions = "dr--r--r--"),
      FileListItem("file", size = 2)
    )))
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "dir", "1", permissions = "dr--r--r--", showDate = true, dirSize = "2 (1)")
  }
  
  it should "render component with sub-dir and focused .." in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = mock[FileListActions]
    val state = FileListState(currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem.up,
      FileListItem("dir", isDir = true, size = 1),
      FileListItem("file", size = 2)
    )))
    val props = FileListPanelProps(dispatch, actions, state)

    //when
    val result = testRender(<(FileListPanel())(^.wrapped := props)())

    //then
    assertFileListPanel(result, props, state, "..", dirSize = "2 (1)")
  }
  
  private def assertFileListPanel(result: TestInstance,
                                  props: FileListPanelProps,
                                  state: FileListState,
                                  expectedFile: String = "",
                                  expectedFileSize: String = "",
                                  permissions: String = "",
                                  showDate: Boolean = false,
                                  selected: Option[String] = None,
                                  dirSize: String = "0 (0)"): Unit = {
    
    val (width, height) = (25, 15)
    val theme = Theme.current.fileList
    
    def assertComponents(border: TestInstance,
                         line: TestInstance,
                         list: TestInstance,
                         currFolder: TestInstance,
                         selection: Option[TestInstance],
                         currFile: TestInstance,
                         fileSize: TestInstance,
                         filePerm: TestInstance,
                         fileDate: TestInstance,
                         folderSize: TestInstance): Assertion = {

      assertTestComponent(border, doubleBorderComp) {
        case DoubleBorderProps(resSize, style, pos, title) =>
          resSize shouldBe width -> height
          style shouldBe theme.regularItem
          pos shouldBe 0 -> 0
          title shouldBe None
      }
      assertTestComponent(line, horizontalLineComp) {
        case HorizontalLineProps(pos, len, lineCh, style, startCh, endCh) =>
          pos shouldBe 0 -> (height - 4)
          len shouldBe width
          lineCh shouldBe SingleBorder.horizontalCh
          style shouldBe theme.regularItem
          startCh shouldBe Some(DoubleBorder.leftSingleCh)
          endCh shouldBe Some(DoubleBorder.rightSingleCh)
      }
      assertTestComponent(list, fileListComp) {
        case FileListProps(dispatch, actions, resState, resSize, columns) =>
          dispatch shouldBe props.dispatch
          actions shouldBe props.actions
          resState shouldBe state
          resSize shouldBe (width - 2) -> (height - 5)
          columns shouldBe 2
      }
      assertTestComponent(currFolder, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Center
          pos shouldBe 1 -> 0
          resWidth shouldBe (width - 2)
          text shouldBe state.currDir.path
          style shouldBe theme.regularItem
          focused shouldBe props.state.isActive
          padding shouldBe 1
      }
      
      selection.size shouldBe selected.size
      selection.foreach { selectedText =>
        assertTestComponent(selectedText, textLineComp) {
          case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
            align shouldBe TextLine.Center
            pos shouldBe 1 -> (height - 4)
            resWidth shouldBe (width - 2)
            text shouldBe selected.get
            style shouldBe theme.selectedItem
            focused shouldBe false
            padding shouldBe 1
        }
      }
      
      assertTestComponent(currFile, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Left
          pos shouldBe 1 -> (height - 3)
          resWidth shouldBe (width - 2 - 12)
          text shouldBe expectedFile
          style shouldBe theme.regularItem
          focused shouldBe false
          padding shouldBe 0
      }
      assertTestComponent(fileSize, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Right
          pos shouldBe (1 + width - 2 - 12) -> (height - 3)
          resWidth shouldBe 12
          text shouldBe expectedFileSize
          style shouldBe theme.regularItem
          focused shouldBe false
          padding shouldBe 0
      }
      
      assertTestComponent(filePerm, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Left
          pos shouldBe 1 -> (height - 2)
          resWidth shouldBe 10
          text shouldBe permissions
          style shouldBe theme.regularItem
          focused shouldBe false
          padding shouldBe 0
      }
      assertTestComponent(fileDate, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Right
          pos shouldBe (1 + width - 2 - 25) -> (height - 2)
          resWidth shouldBe 25
          if (showDate) text should not be empty
          else text should be (empty)
          style shouldBe theme.regularItem
          focused shouldBe false
          padding shouldBe 0
      }
      
      assertTestComponent(folderSize, textLineComp) {
        case TextLineProps(align, pos, resWidth, text, style, focused, padding) =>
          align shouldBe TextLine.Center
          pos shouldBe 1 -> (height - 1)
          resWidth shouldBe (width - 2)
          text shouldBe dirSize
          style shouldBe theme.regularItem
          focused shouldBe false
          padding shouldBe 1
      }
    }
    
    assertTestComponent(result, withSizeComp) { case WithSizeProps(render) =>
      val result = createTestRenderer(render(width, height)).root
      
      assertNativeComponent(result, <.box(^.rbStyle := theme.regularItem)(), {
        case List(border, line, list, currFolder, currFile, fileSize, filePerm, fileDate, folderSize) =>
          assertComponents(border, line, list, currFolder, None, currFile, fileSize, filePerm, fileDate, folderSize)
        case List(border, line, list, currFolder, selection, currFile, fileSize, filePerm, fileDate, folderSize) =>
          assertComponents(border, line, list, currFolder, Some(selection), currFile, fileSize, filePerm, fileDate, folderSize)
      })
    }
  }
}
