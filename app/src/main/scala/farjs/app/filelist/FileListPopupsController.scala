package farjs.app.filelist

import farjs.app.FarjsStateDef
import farjs.filelist.FileListActions
import farjs.filelist.popups.{FileListPopups, FileListPopupsProps}
import io.github.shogowada.scalajs.reactjs.React.Props
import io.github.shogowada.scalajs.reactjs.redux.Redux.Dispatch
import scommons.react.UiComponent
import scommons.react.redux.BaseStateController

class FileListPopupsController(actions: FileListActions)
  extends BaseStateController[FarjsStateDef, FileListPopupsProps] {

  lazy val uiComponent: UiComponent[FileListPopupsProps] = FileListPopups

  def mapStateToProps(dispatch: Dispatch, state: FarjsStateDef, props: Props[Unit]): FileListPopupsProps = {
    FileListPopupsProps(dispatch, actions, state.fileListsState)
  }
}
