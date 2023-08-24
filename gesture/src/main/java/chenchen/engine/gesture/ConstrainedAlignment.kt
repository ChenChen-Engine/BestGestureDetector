package chenchen.engine.gesture

/**
 * 被约束的对齐方式，选择用什么对齐方式去受约束
 * 比如受到了左边的约束[ConstraintAlignment.Left]，可以选择用左边[LeftToLeft]，
 * 中间[HorizontalCenterToLeft]或右边[RightToLeft]去接受约束
 * @author: chenchen
 * @since: 2023/3/13 9:36
 */
enum class ConstrainedAlignment {
    /*左边对齐左边*/
    LeftToLeft,

    /*左边对齐中心点*/
    LeftToHorizontalCenter,

    /*左边对齐右边*/
    LeftToRight,

    /*中心点对齐左边*/
    HorizontalCenterToLeft,

    /*中心点对齐中心点*/
    HorizontalCenterToHorizontalCenter,

    /*中心点对齐右边*/
    HorizontalCenterToRight,

    /*右边对齐左边*/
    RightToLeft,

    /*右边对齐中心点*/
    RightToHorizontalCenter,

    /*右边对齐右边*/
    RightToRight,

    /*顶部对齐顶部*/
    TopToTop,

    /*顶部对齐中心点*/
    TopToVerticalCenter,

    /*顶部对齐底部*/
    TopToBottom,

    /*中心点对齐顶部*/
    VerticalCenterToTop,

    /*中心点对齐中心点*/
    VerticalCenterToVerticalCenter,

    /*中心点对齐底部*/
    VerticalCenterToBottom,

    /*底部对齐顶部*/
    BottomToTop,

    /*底部对齐中心点*/
    BottomToVerticalCenter,

    /*底部对齐底部*/
    BottomToBottom;

    companion object {
        /**
         * 四边
         */
        fun frame() = arrayListOf(LeftToLeft, RightToRight, TopToTop, BottomToBottom)

        /**
         * 居中
         */
        fun center() = arrayListOf(HorizontalCenterToHorizontalCenter, VerticalCenterToVerticalCenter)

        /**
         * 四边+居中
         */
        fun frameAndCenter() = ArrayList<ConstrainedAlignment>().apply {
            addAll(frame())
            addAll(center())
        }

        /**
         * 垂直
         */
        fun vertical() = arrayListOf(
            TopToTop, TopToVerticalCenter, TopToBottom,
            VerticalCenterToTop, VerticalCenterToVerticalCenter, VerticalCenterToBottom,
            BottomToTop, BottomToVerticalCenter, BottomToBottom
        )

        /**
         * 水平
         */
        fun horizontal() = arrayListOf(
            LeftToLeft, LeftToHorizontalCenter, LeftToRight,
            HorizontalCenterToLeft, HorizontalCenterToHorizontalCenter, HorizontalCenterToRight,
            RightToLeft, RightToHorizontalCenter, RightToRight
        )

        /**
         * 全部
         */
        fun all() = arrayListOf(
            LeftToLeft, LeftToHorizontalCenter, LeftToRight,
            RightToLeft, RightToHorizontalCenter, RightToRight,
            TopToTop, TopToVerticalCenter, TopToBottom,
            BottomToBottom, BottomToVerticalCenter, BottomToTop,
            HorizontalCenterToLeft, HorizontalCenterToHorizontalCenter, HorizontalCenterToRight,
            VerticalCenterToTop, VerticalCenterToVerticalCenter, VerticalCenterToBottom,
        )
    }
}
