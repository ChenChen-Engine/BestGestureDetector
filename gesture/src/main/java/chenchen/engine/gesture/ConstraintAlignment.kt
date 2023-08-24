package chenchen.engine.gesture

/**
 * 约束对齐方式，选择用什么对齐方式约束对象
 * 比如使用左边[Left]去约束对象，那么被约束的对象只能用左边[ConstrainedAlignment.LeftToLeft]、
 * 中间[ConstrainedAlignment.HorizontalCenterToLeft]、右边[ConstrainedAlignment.RightToLeft]去接受约束
 * @author: chenchen
 * @since: 2023/4/20 14:43
 */
enum class ConstraintAlignment {
    /*左边对齐*/
    Left,

    /*顶部对齐*/
    Top,

    /*右边对齐*/
    Right,

    /*底部对齐*/
    Bottom,

    /*垂直中心对齐，这条是水平线*/
    VerticalCenter,

    /*水平中心对齐，这条是垂直线*/
    HorizontalCenter;

    companion object {
        /**
         * 四边
         */
        fun frame() = arrayListOf(Left, Top, Right, Bottom)

        /**
         * 居中
         */
        fun center() = arrayListOf(VerticalCenter, HorizontalCenter)

        /**
         * 垂直
         */
        fun vertical() = arrayListOf(Top, VerticalCenter, Bottom)

        /**
         * 水平
         */
        fun horizontal() = arrayListOf(Left, HorizontalCenter, Right)

        /**
         * 全部
         */
        fun all() = ArrayList<ConstraintAlignment>().apply {
            addAll(frame())
            addAll(center())
        }
    }
}
