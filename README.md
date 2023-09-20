# Welcome to the BestGestureDetector wiki!

这是一个手势基础库，实现了缩放、旋转、平移最基础的手势，可以通过单指、双指来实现(暂不支持双指以上的手势，懒得写)。

这个库最大的优点是可以在任意一个View的onTouchEvent或setOnTouchEventListener中处理手势。(以往写手势都需要在ViewGroup中的onTouchEvent对子View进行缩放、旋转、平移)。原理是使用MotionEvent.getRawX()和MotionEvent.getRawY()处理，细节可以看代码实现，比较简单。

这个库第二个优点是作为一个基础库可以衍生出其他的手势库。由于业务需要，实现了一个吸附的手势，已经内置在库中，若以后有更多的手势，可能会单独拆分成一个个衍生手势库。简而言之，基于手势库可以快速实现非常多常见又麻烦的手势。

## Lastest version
[![](https://jitpack.io/v/ChenChen-Engine/BestGestureDetector.svg)](https://jitpack.io/#ChenChen-Engine/BestGestureDetector)

## 依赖
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //...
        //...
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
dependencies {
    implementation 'com.github.ChenChen-Engine:BestGestureDetector:lastestVersion'
}
```

## [WiKi](https://github.com/ChenChen-Engine/BestGestureDetector/wiki/BestGestureDetector)
