package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import com.keshav.capturesposed.BuildConfig
import com.keshav.capturesposed.TAG
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

object FlagSecureHooker {
    private var module: XposedModule? = null
    private var classLoader: ClassLoader? = null

    @SuppressLint("PrivateApi", "BlockedPrivateApi")
    fun hook(param: SystemServerStartingParam, module: XposedModule) {
        this.module = module
        this.classLoader = param.classLoader

        val cl = param.classLoader

        // 1. Hook ScreenCapture / SurfaceControl native capture methods
        try {
            hookScreenCapture(cl, module)
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "hookScreenCapture failed: $t")
        }

        // 2. Hook WindowState.isSecureLocked
        try {
            hookWindowState(cl, module)
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "hookWindowState failed: $t")
        }

        // 3. Hook ScreenshotHardwareBuffer.containsSecureLayers
        try {
            hookScreenshotHardwareBuffer(cl, module)
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "hookScreenshotHardwareBuffer failed: $t")
        }

        // 4. Hook DisplayControl (VirtualDisplay security flag for screen recording)
        try {
            hookDisplayControl(cl, module)
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "hookDisplayControl failed: $t")
        }

        // 5. Hook VirtualDisplayAdapter
        try {
            hookVirtualDisplayAdapter(cl, module)
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "hookVirtualDisplayAdapter failed: $t")
        }

        // 6. Hook OEM specific implementations (HyperOS, OneUI, OPlus)
        hookOemSpecifics(cl, module)
    }

    private fun hookScreenCapture(classLoader: ClassLoader, module: XposedModule) {
        val screenCaptureClass: Class<*>
        val captureArgsClass: Class<*>

        if (Build.VERSION.SDK_INT >= 35) {
            screenCaptureClass = try {
                classLoader.loadClass("android.window.ScreenCaptureInternal")
            } catch (_: ClassNotFoundException) {
                classLoader.loadClass("android.window.ScreenCapture")
            }
            captureArgsClass = try {
                classLoader.loadClass("android.window.ScreenCaptureInternal\$CaptureArgs")
            } catch (_: ClassNotFoundException) {
                classLoader.loadClass("android.window.ScreenCapture\$CaptureArgs")
            }
        } else {
            screenCaptureClass = classLoader.loadClass("android.window.ScreenCapture")
            captureArgsClass = classLoader.loadClass("android.window.ScreenCapture\$CaptureArgs")
        }

        val captureSecureLayersField = try {
            captureArgsClass.getDeclaredField("mCaptureSecureLayers")
        } catch (_: NoSuchFieldException) {
            captureArgsClass.getDeclaredField("mSecureContentPolicy")
        }
        captureSecureLayersField.isAccessible = true

        val captureMethods = screenCaptureClass.declaredMethods.filter {
            it.name == "nativeCaptureDisplay" || it.name == "nativeCaptureLayers"
        }

        for (m in captureMethods) {
            m.isAccessible = true
            module.hook(m).intercept { chain ->
                val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs.getBoolean("flagSecureHookActive", true)

                if (isHookActive && chain.args.isNotEmpty() && chain.args[0] != null) {
                    val captureArgs = chain.args[0]
                    try {
                        if (captureSecureLayersField.type == Int::class.javaPrimitiveType) {
                            captureSecureLayersField.setInt(captureArgs, 1)
                        } else {
                            captureSecureLayersField.setBoolean(captureArgs, true)
                        }
                    } catch (t: Throwable) {
                        module.log(Log.WARN, TAG, "Failed to set captureSecureLayers: $t")
                    }
                }
                chain.proceed()
            }
        }
        module.log(Log.INFO, TAG, "ScreenCapture hooked successfully")
    }

    private fun hookWindowState(classLoader: ClassLoader, module: XposedModule) {
        val windowStateClass = classLoader.loadClass("com.android.server.wm.WindowState")
        val isSecureLockedMethod = windowStateClass.getDeclaredMethod("isSecureLocked")
        val systemServerCl = windowStateClass.classLoader

        module.hook(isSecureLockedMethod).intercept { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("flagSecureHookActive", true)

            if (!isHookActive) {
                return@intercept chain.proceed()
            }

            // StackWalker to allow initial surface creation without breaking DRM / secure decoders
            val walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            val match = walker.walk { frames ->
                frames.anyMatch { frame ->
                    frame.declaringClass != null &&
                            frame.declaringClass.classLoader == systemServerCl &&
                            (frame.methodName == "setInitialSurfaceControlProperties" ||
                                    frame.methodName == "createSurfaceLocked")
                }
            }

            if (match) {
                return@intercept chain.proceed()
            }

            return@intercept false
        }
        module.log(Log.INFO, TAG, "WindowState.isSecureLocked hooked successfully")
    }

    private fun hookScreenshotHardwareBuffer(classLoader: ClassLoader, module: XposedModule) {
        val hardwareBufferClass = try {
            if (Build.VERSION.SDK_INT >= 35) {
                try {
                    classLoader.loadClass("android.window.ScreenCaptureInternal\$ScreenshotHardwareBuffer")
                } catch (_: ClassNotFoundException) {
                    classLoader.loadClass("android.window.ScreenCapture\$ScreenshotHardwareBuffer")
                }
            } else {
                classLoader.loadClass("android.window.ScreenCapture\$ScreenshotHardwareBuffer")
            }
        } catch (_: ClassNotFoundException) {
            classLoader.loadClass("android.view.SurfaceControl\$ScreenshotHardwareBuffer")
        }

        val containsSecureLayersMethod = hardwareBufferClass.getDeclaredMethod("containsSecureLayers")
        containsSecureLayersMethod.isAccessible = true

        module.hook(containsSecureLayersMethod).intercept { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("flagSecureHookActive", true)

            if (isHookActive) {
                return@intercept false
            }
            chain.proceed()
        }
        module.log(Log.INFO, TAG, "ScreenshotHardwareBuffer hooked successfully")
    }

    private fun hookDisplayControl(classLoader: ClassLoader, module: XposedModule) {
        val displayControlClass = classLoader.loadClass("com.android.server.display.DisplayControl")
        val methodName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            "createVirtualDisplay"
        } else {
            "createDisplay"
        }

        val createDisplayMethod = displayControlClass.getDeclaredMethod(
            methodName, String::class.java, Boolean::class.javaPrimitiveType
        )
        createDisplayMethod.isAccessible = true

        module.hook(createDisplayMethod).intercept { chain ->
            val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val isHookActive = prefs.getBoolean("flagSecureHookActive", true)

            if (isHookActive && chain.args.size >= 2) {
                val args = chain.args.toTypedArray()
                args[1] = true // Force virtual display to be secure
                return@intercept chain.proceed(args)
            }
            chain.proceed()
        }
        module.log(Log.INFO, TAG, "DisplayControl.$methodName hooked successfully")
    }

    private fun hookVirtualDisplayAdapter(classLoader: ClassLoader, module: XposedModule) {
        val virtualDisplayAdapterClass = classLoader.loadClass("com.android.server.display.VirtualDisplayAdapter")
        val methods = virtualDisplayAdapterClass.declaredMethods.filter { it.name == "createVirtualDisplayLocked" }

        for (m in methods) {
            m.isAccessible = true
            module.hook(m).intercept { chain ->
                val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val isHookActive = prefs.getBoolean("flagSecureHookActive", true)

                if (isHookActive) {
                    val caller = chain.args.getOrNull(2) as? Int ?: 0
                    if (!(caller >= 10000 && chain.args.getOrNull(1) == null)) {
                        val args = chain.args.toTypedArray()
                        for (i in 3 until args.size) {
                            val arg = args[i]
                            if (arg is Int) {
                                // DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE = 0x00000004
                                args[i] = arg or 0x00000004
                                return@intercept chain.proceed(args)
                            }
                        }
                    }
                }
                chain.proceed()
            }
        }
        module.log(Log.INFO, TAG, "VirtualDisplayAdapter hooked successfully")
    }

    private fun hookOemSpecifics(classLoader: ClassLoader, module: XposedModule) {
        // HyperOS / MIUI
        try {
            val hyperOSClass = classLoader.loadClass("com.android.server.wm.WindowManagerServiceImpl")
            hyperOSClass.declaredMethods.filter { it.name == "notAllowCaptureDisplay" }.forEach { m ->
                m.isAccessible = true
                module.hook(m).intercept { chain ->
                    val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    if (prefs.getBoolean("flagSecureHookActive", true)) false else chain.proceed()
                }
            }
            module.log(Log.INFO, TAG, "HyperOS specific hook applied")
        } catch (_: Throwable) {}

        // Samsung OneUI
        try {
            val oneUiClass = classLoader.loadClass("com.android.server.wm.WmScreenshotController")
            oneUiClass.declaredMethods.filter { it.name == "canBeScreenshotTarget" }.forEach { m ->
                m.isAccessible = true
                module.hook(m).intercept { chain ->
                    val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    if (prefs.getBoolean("flagSecureHookActive", true)) true else chain.proceed()
                }
            }
            module.log(Log.INFO, TAG, "OneUI specific hook applied")
        } catch (_: Throwable) {}

        // OPlus
        try {
            val oplusClass = classLoader.loadClass("com.android.server.wm.OplusLongshotMainWindow")
            oplusClass.declaredMethods.filter { it.name == "hasSecure" }.forEach { m ->
                m.isAccessible = true
                module.hook(m).intercept { chain ->
                    val prefs = module.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    if (prefs.getBoolean("flagSecureHookActive", true)) false else chain.proceed()
                }
            }
            module.log(Log.INFO, TAG, "OPlus specific hook applied")
        } catch (_: Throwable) {}
    }
}
