package com.naver.android.helloyako.imagecrop.util

import android.opengl.GLES20
import javax.microedition.khronos.egl.*

/**
 * Created by helloyako on 15. 6. 19..
 */
object GLUtils {
    //        GL10 mGL;

    // No error checking performed, minimum required code to elucidate logic

    // No error checking performed, minimum required code to elucidate logic
    // Expand on this logic to be more selective in choosing a configuration
    val maxTextureSize: Int // Choosing a config is a little more

    // complicated

    // mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig,
    // EGL_NO_CONTEXT, null);

        //        mGL = (GL10) mEGLContext.getGL();
        get() {
            val version = IntArray(2)
            val attribList = intArrayOf(
                EGL10.EGL_WIDTH, 100,
                EGL10.EGL_HEIGHT, 100,
                EGL10.EGL_NONE
            )
            val mEGL: EGL10
            val mEGLDisplay: EGLDisplay
            val mEGLConfigs: Array<EGLConfig?>
            val mEGLConfig: EGLConfig?
            val mEGLContext: EGLContext
            val mEGLSurface: EGLSurface
            //        GL10 mGL;

            // No error checking performed, minimum required code to elucidate logic
            mEGL = EGLContext.getEGL() as EGL10
            mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            mEGL.eglInitialize(mEGLDisplay, version)
            val attribList1 = intArrayOf(
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
            )

            // No error checking performed, minimum required code to elucidate logic
            // Expand on this logic to be more selective in choosing a configuration
            val numConfig = IntArray(1)
            mEGL.eglChooseConfig(mEGLDisplay, attribList1, null, 0, numConfig)
            val configSize = numConfig[0]
            mEGLConfigs = arrayOfNulls(configSize)
            mEGL.eglChooseConfig(mEGLDisplay, attribList1, mEGLConfigs, configSize, numConfig)
            mEGLConfig = mEGLConfigs[0] // Choosing a config is a little more

            // complicated

            // mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig,
            // EGL_NO_CONTEXT, null);
            val EGL_CONTEXT_CLIENT_VERSION = 0x3098
            val attrib_list = intArrayOf(
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
            )
            mEGLContext =
                mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
            mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList)
            mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)

//        mGL = (GL10) mEGLContext.getGL();
            val maxTextureSize = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
            mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface)
            mEGL.eglDestroyContext(mEGLDisplay, mEGLContext)
            mEGL.eglTerminate(mEGLDisplay)
            return maxTextureSize[0]
        }
}