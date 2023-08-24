package chenchen.engine.gesture

import java.lang.IllegalArgumentException

/**
 * 手势类型
 * @author: chenchen
 * @since: 2023/3/9 10:07
 */
enum class Gesture {
    DOWN,
    Move,
    Rotate,
    ScaleX,
    ScaleY,
    Scale,
    Click,
    LongClick,
    DoubleClick,
    Press,
    LongPress,
}

/**
 * 检查移动类型的手势，不能有多个相同的类型
 */
fun List<Gesture>.checkMoveGesture() {
    val moveCount = this.count { it.isMoveGesture() }
    if (moveCount > 1) {
        throw IllegalArgumentException("移动类的手势不能有多个相同类型")
    }
}

/**
 * 获取移动类的手势
 */
fun List<Gesture>.getMoveGesture(): Gesture? {
    return firstOrNull { it.isMoveGesture() }
}

fun Gesture?.isMoveGesture(): Boolean {
    return when (this) {
        Gesture.Move, Gesture.Rotate, Gesture.ScaleX, Gesture.ScaleY, Gesture.Scale -> true
        else -> false
    }
}