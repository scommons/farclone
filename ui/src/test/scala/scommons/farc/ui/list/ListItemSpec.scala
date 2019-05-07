package scommons.farc.ui.list

import scommons.react._
import scommons.react.blessed._
import scommons.react.test.TestSpec
import scommons.react.test.raw.ShallowInstance
import scommons.react.test.util.ShallowRendererUtils

class ListItemSpec extends TestSpec with ShallowRendererUtils {

  it should "render not focused short item" in {
    //given
    val props = getListItemProps.copy(
      width = 10,
      text = "short item",
      focused = false
    )
    val comp = <(ListItem())(^.wrapped := props)()

    //when
    val result = shallowRender(comp)

    //then
    assertListItem(result, props, longItem = false)
  }
  
  it should "render not focused too long item" in {
    //given
    val props = getListItemProps.copy(
      width = 3,
      text = "too long item",
      focused = false
    )
    val comp = <(ListItem())(^.wrapped := props)()

    //when
    val result = shallowRender(comp)

    //then
    assertListItem(result, props, longItem = true)
  }
  
  it should "render focused short item" in {
    //given
    val props = getListItemProps.copy(
      width = 10,
      text = "short item",
      focused = true
    )
    val comp = <(ListItem())(^.wrapped := props)()

    //when
    val result = shallowRender(comp)

    //then
    assertListItem(result, props, longItem = false)
  }
  
  it should "render focused long item" in {
    //given
    val props = getListItemProps.copy(
      width = 3,
      text = "too long item",
      focused = true
    )
    val comp = <(ListItem())(^.wrapped := props)()

    //when
    val result = shallowRender(comp)

    //then
    assertListItem(result, props, longItem = true)
  }
  
  private def getListItemProps: ListItemProps = ListItemProps(
    width = 5,
    top = 2,
    style = new BlessedStyle {
      override val fg = "white"
      override val bg = "blue"
      override val focus = new BlessedStyle {}
    },
    text = "test item",
    focused = true
  )

  private def assertListItem(result: ShallowInstance, props: ListItemProps, longItem: Boolean): Unit = {
    assertNativeComponent(result,
      <.>()(
        <.text(
          ^.rbWidth := props.width,
          ^.rbHeight := 1,
          ^.rbTop := props.top,
          ^.rbStyle := {
            if (props.focused) props.style.focus.asInstanceOf[BlessedStyle]
            else props.style
          },
          ^.content := props.text
        )(),
        
        if (longItem) Some(
          <.text(
            ^.rbHeight := 1,
            ^.rbLeft := props.width,
            ^.rbTop := props.top,
            ^.rbStyle := new BlessedStyle {
              override val fg = "red"
              override val bg = "blue"
            },
            ^.content := "}"
          )()
        )
        else None
      )
    )
  }
}