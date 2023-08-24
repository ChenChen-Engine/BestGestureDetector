package chenchen.engine.gesture

/**
 * @author: chenchen
 * @since: 2023/3/13 9:36
 * 手势运动轨迹
 */
enum class MovementTrack {
    /*空*/
    None,

    /*从左滑到右*/
    LeftToRight,

    /*从右滑到左*/
    RightToLeft,

    /*从上滑到下*/
    TopToBottom,

    /*从下滑到上*/
    BottomToTop,
}