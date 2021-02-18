package farjs.filelist

import farjs.filelist.api.FileListItem
import farjs.ui._
import farjs.ui.border._
import farjs.ui.theme.Theme
import io.github.shogowada.scalajs.reactjs.redux.Redux.Dispatch
import scommons.react._
import scommons.react.blessed._

import scala.scalajs.js

case class FileListPanelProps(dispatch: Dispatch,
                              actions: FileListActions,
                              state: FileListState)

object FileListPanel extends FunctionComponent[FileListPanelProps] {

  private[filelist] var withSizeComp: UiComponent[WithSizeProps] = WithSize
  private[filelist] var doubleBorderComp: UiComponent[DoubleBorderProps] = DoubleBorder
  private[filelist] var horizontalLineComp: UiComponent[HorizontalLineProps] = HorizontalLine
  private[filelist] var fileListComp: UiComponent[FileListProps] = FileList
  private[filelist] var textLineComp: UiComponent[TextLineProps] = TextLine

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    val theme = Theme.current.fileList
    
    val currItem = props.state.currentItem
    val selectedItems = props.state.selectedItems

    <(withSizeComp())(^.wrapped := WithSizeProps({ (width, height) =>
      <.box(^.rbStyle := theme.regularItem)(
        <(doubleBorderComp())(^.wrapped := DoubleBorderProps((width, height), theme.regularItem))(),
        <(horizontalLineComp())(^.wrapped := HorizontalLineProps(
          pos = (0, height - 4),
          length = width,
          lineCh = SingleBorder.horizontalCh,
          style = theme.regularItem,
          startCh = Some(DoubleBorder.leftSingleCh),
          endCh = Some(DoubleBorder.rightSingleCh)
        ))(),
        <(fileListComp())(^.wrapped := FileListProps(
          dispatch = props.dispatch,
          actions = props.actions,
          state = props.state,
          size = (width - 2, height - 5),
          columns = 2
        ))(),
        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Center,
          pos = (1, 0),
          width = width - 2,
          text = props.state.currDir.path,
          style = theme.regularItem,
          focused = props.state.isActive
        ))(),

        if (selectedItems.nonEmpty) Some(
          <(textLineComp())(^.wrapped := TextLineProps(
            align = TextLine.Center,
            pos = (1, height - 4),
            width = width - 2,
            text = {
              val selectedSize = selectedItems.foldLeft(0.0)((res, f) => res + f.size)
              val count = selectedItems.size
              val files = if (count == 1) "file" else "files"
              f"$selectedSize%,.0f in $count%d $files"
            },
            style = theme.selectedItem
          ))()
        )
        else None,

        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Left,
          pos = (1, height - 3),
          width = width - 2 - 12,
          text = currItem.map(_.name).getOrElse(""),
          style = theme.regularItem,
          padding = 0
        ))(),
        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Right,
          pos = (1 + width - 2 - 12, height - 3),
          width = 12,
          text = currItem.filter(i => i.size > 0.0 || !i.isDir).map { i =>
            f"${i.size}%,.0f"
          }.getOrElse(""),
          style = theme.regularItem,
          padding = 0
        ))(),

        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Left,
          pos = (1, height - 2),
          width = 10,
          text = currItem.map(_.permissions).getOrElse(""),
          style = theme.regularItem,
          padding = 0
        ))(),
        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Right,
          pos = (1 + width - 2 - 25, height - 2),
          width = 25,
          text = currItem.filter(i => i.name != FileListItem.up.name).map { i =>
            val date = new js.Date(i.mtimeMs)
            s"${date.toLocaleDateString()} ${date.toLocaleTimeString()}"
          }.getOrElse(""),
          style = theme.regularItem,
          padding = 0
        ))(),

        <(textLineComp())(^.wrapped := TextLineProps(
          align = TextLine.Center,
          pos = (1, height - 1),
          width = width - 2,
          text = {
            val files = props.state.currDir.items.filter(!_.isDir)
            val filesSize = files.foldLeft(0.0)((res, f) => res + f.size)
            f"$filesSize%,.0f (${files.size}%d)"
          },
          style = theme.regularItem
        ))()
      )
    }))()
  }
}
