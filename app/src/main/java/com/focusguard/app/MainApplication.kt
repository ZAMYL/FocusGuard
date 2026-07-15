package com.focusguard.app

import android.app.Application

/**
 * 自定义 Application 入口
 *
 * 整个应用共用此实例，可通过 [MainApplication.timerState] 访问全局番茄钟状态。
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        /** 全局 Application 实例，供无障碍服务等组件获取 Context */
        lateinit var instance: MainApplication
            private set

        /** 对外暴露的 TimerState 快捷引用 */
        val timerState: TimerState
            get() = TimerState
    }
}
