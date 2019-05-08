package scommons.farc.ui.border

import scommons.react._
import scommons.react.blessed._

case class DoubleBorderProps(size: (Int, Int),
                             style: BlessedStyle)

object DoubleBorder extends FunctionComponent[DoubleBorderProps] {
  
  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    val (width, height) = props.size

    <.>()(
      <(HorizontalLine())(^.key := "0", ^.wrapped := HorizontalLineProps(
        pos = (0, 0),
        length = width,
        lineCh = horizontalCh,
        style = props.style,
        startCh = Some(topLeftCh),
        endCh = Some(topRightCh)
      ))(),
      
      <(VerticalLine())(^.key := "1", ^.wrapped := VerticalLineProps(
        pos = (0, 1),
        length = height - 2,
        lineCh = verticalCh,
        style = props.style
      ))(),
      
      <(VerticalLine())(^.key := "2", ^.wrapped := VerticalLineProps(
        pos = (width - 1, 1),
        length = height - 2,
        lineCh = verticalCh,
        style = props.style
      ))(),

      <(HorizontalLine())(^.key := "3", ^.wrapped := HorizontalLineProps(
        pos = (0, height - 1),
        length = width,
        lineCh = horizontalCh,
        style = props.style,
        startCh = Some(bottomLeftCh),
        endCh = Some(bottomRightCh)
      ))()
    )
  }

  // lines
  val horizontalCh = "\u2550"
  val verticalCh = "\u2551"

  // corners
  val topLeftCh = "\u2554"
  val topRightCh = "\u2557"
  val bottomLeftCh = "\u255a"
  val bottomRightCh = "\u255d"
  
  // connectors
  val topCh = "\u2566"
  val bottomCh = "\u2569"
  val leftCh = "\u2560"
  val rightCh = "\u2563"
  
  // single connectors
  val topSingleCh = "\u2564"
  val bottomSingleCh = "\u2567"
  val leftSingleCh = "\u255f"
  val rightSingleCh = "\u2562"

  // crosses
  val crossCh = "\u256c"
  val crossSingleVertCh = "\u256a"
  val crossSingleHorizCh = "\u256b"
}
