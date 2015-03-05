package com.mygdx.game.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.LinearLayout;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGL20;
import com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.WindowedMean;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class AndroidCardboardGraphics implements Graphics, StereoRenderer {
	
	private static final String LOG_TAG = "AndroidCardboardGraphics";
	static volatile boolean enforceContinuousRendering = false;
	
	final CardboardView view;
	int width;
	int height;
	AndroidCardboardApplication app;
	GL20 gl20;
	GL30 gl30;
	EGLContext eglContext;
	String extensions;
	
	protected long lastFrameTime = System.nanoTime();
	protected float deltaTime = 0;
	protected long frameStart = System.nanoTime();
	protected long frameId = -1;
	protected int frames = 0;
	protected int fps;
	protected WindowedMean mean = new WindowedMean(5);

	volatile boolean created = false;
	volatile boolean running = false;
	volatile boolean pause = false;
	volatile boolean resume = false;
	volatile boolean destroy = false;

	private float ppiX = 0;
	private float ppiY = 0;
	private float ppcX = 0;
	private float ppcY = 0;
	private float density = 1;

	protected final AndroidApplicationConfiguration config;
	private BufferFormat bufferFormat = new BufferFormat(5, 6, 5, 0, 16, 0, 0, false);
	private boolean isContinuous = true;

	public AndroidCardboardGraphics(AndroidCardboardApplication application, AndroidApplicationConfiguration config) {
		this.app = application;
		this.config = config;
		this.view = new CardboardView(application.getContext());
		this.view.setRenderer(this);
		this.view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));		
	}

	@Override
	public void onDrawEye(Eye eye) {
		app.getApplicationListener().drawEye(eye);
	}


	@Override
	public void onFinishFrame(Viewport viewport) {
		app.getApplicationListener().finishFrame(viewport);
	}


	@Override
	public void onNewFrame(HeadTransform transform) {
		app.getApplicationListener().newFrame(transform);
	}


	@Override
	public void onRendererShutdown() {
		app.getApplicationListener().dispose();
	}


	@Override
	public void onSurfaceChanged(int width, int height) {
		this.width = width;
		this.height = height;
		updatePpi();

		if (created == false) {
			app.getApplicationListener().create();
			created = true;
			synchronized (this) {
				running = true;
			}
		}
		app.getApplicationListener().resize(width, height);
	}

	@Override
	public void onSurfaceCreated(EGLConfig config) {
		eglContext = ((EGL10)EGLContext.getEGL()).eglGetCurrentContext();
		setupGL();
		logConfig(config);
		updatePpi();

		Mesh.invalidateAllMeshes(app);
		Texture.invalidateAllTextures(app);
		Cubemap.invalidateAllCubemaps(app);
		ShaderProgram.invalidateAllShaderPrograms(app);
		FrameBuffer.invalidateAllFrameBuffers(app);

		logManagedCachesStatus();

		Display display = app.getWindowManager().getDefaultDisplay();
		this.width = display.getWidth();
		this.height = display.getHeight();
		this.mean = new WindowedMean(5);
		this.lastFrameTime = System.nanoTime();		
	}
		
	private void setupGL () {
		if (gl20 != null) return;

		gl20 = new AndroidGL20();

		Gdx.gl = gl20;
		Gdx.gl20 = gl20;
	}
	
	private void logConfig (EGLConfig config) {
		EGL10 egl = (EGL10)EGLContext.getEGL();
		EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		int r = getAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
		int g = getAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
		int b = getAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
		int a = getAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
		int d = getAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
		int s = getAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
		int samples = Math.max(getAttrib(egl, display, config, EGL10.EGL_SAMPLES, 0),
			getAttrib(egl, display, config, GdxEglConfigChooser.EGL_COVERAGE_SAMPLES_NV, 0));
		boolean coverageSample = getAttrib(egl, display, config, GdxEglConfigChooser.EGL_COVERAGE_SAMPLES_NV, 0) != 0;

		Gdx.app.log(LOG_TAG, "framebuffer: (" + r + ", " + g + ", " + b + ", " + a + ")");
		Gdx.app.log(LOG_TAG, "depthbuffer: (" + d + ")");
		Gdx.app.log(LOG_TAG, "stencilbuffer: (" + s + ")");
		Gdx.app.log(LOG_TAG, "samples: (" + samples + ")");
		Gdx.app.log(LOG_TAG, "coverage sampling: (" + coverageSample + ")");

		bufferFormat = new BufferFormat(r, g, b, a, d, s, samples, coverageSample);
	}
	
	protected void logManagedCachesStatus () {
		Gdx.app.log(LOG_TAG, Mesh.getManagedStatus());
		Gdx.app.log(LOG_TAG, Texture.getManagedStatus());
		Gdx.app.log(LOG_TAG, Cubemap.getManagedStatus());
		Gdx.app.log(LOG_TAG, ShaderProgram.getManagedStatus());
		Gdx.app.log(LOG_TAG, FrameBuffer.getManagedStatus());
	}
	
	int[] value = new int[1];

	private int getAttrib (EGL10 egl, EGLDisplay display, EGLConfig config, int attrib, int defValue) {
		if (egl.eglGetConfigAttrib(display, config, attrib, value)) {
			return value[0];
		}
		return defValue;
	}
	
	private void updatePpi () {
		DisplayMetrics metrics = new DisplayMetrics();
		app.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		ppiX = metrics.xdpi;
		ppiY = metrics.ydpi;
		ppcX = metrics.xdpi / 2.54f;
		ppcY = metrics.ydpi / 2.54f;
		density = metrics.density;
	}


	@Override
	public boolean isGL30Available() {
		return gl30 != null;
	}

	@Override
	public GL20 getGL20() {
		return this.gl20;
	}

	@Override
	public GL30 getGL30() {
		return this.gl30;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public long getFrameId() {
		return this.frameId;
	}

	@Override
	public float getDeltaTime() {
		return this.mean.getMean() == 0 ? this.deltaTime : this.mean.getMean();
	}

	@Override
	public float getRawDeltaTime() {
		return this.deltaTime;
	}

	@Override
	public int getFramesPerSecond() {
		return this.fps;
	}

	@Override
	public GraphicsType getType() {
		return GraphicsType.AndroidGL;
	}

	public CardboardView getView () {
		return this.view;
	}

	@Override
	public float getPpiX () {
		return this.ppiX;
	}

	@Override
	public float getPpiY () {
		return this.ppiY;
	}

	@Override
	public float getPpcX () {
		return this.ppcX;
	}

	@Override
	public float getPpcY () {
		return this.ppcY;
	}

	@Override
	public float getDensity () {
		return this.density;
	}
	
	@Override
	public boolean supportsDisplayModeChange() {
		return false;
	}

	@Override
	public DisplayMode[] getDisplayModes() {
		return new DisplayMode[] {getDesktopDisplayMode()};
	}
	
	private class AndroidDisplayMode extends DisplayMode {
		protected AndroidDisplayMode (int width, int height, int refreshRate, int bitsPerPixel) {
			super(width, height, refreshRate, bitsPerPixel);
		}
	}

	@Override
	public DisplayMode getDesktopDisplayMode() {
		DisplayMetrics metrics = new DisplayMetrics();
		app.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return new AndroidDisplayMode(metrics.widthPixels, metrics.heightPixels, 0, 0);
	}

	@Override
	public boolean setDisplayMode(DisplayMode displayMode) {
		return false;
	}

	@Override
	public boolean setDisplayMode(int width, int height, boolean fullscreen) {
		return false;
	}

	@Override
	public void setTitle(String title) {
		// Do nothing
	}

	@Override
	public BufferFormat getBufferFormat () {
		return bufferFormat;
	}

	@Override
	public void setVSync (boolean vsync) {
	}

	@Override
	public boolean supportsExtension(String extension) {
		if (extensions == null) extensions = Gdx.gl.glGetString(GL10.GL_EXTENSIONS);
		return extensions.contains(extension);
	}

	@Override
	public void setContinuousRendering(boolean isContinuous) {
		if (view != null) {
			// ignore setContinuousRendering(false) while pausing
			this.isContinuous = enforceContinuousRendering || isContinuous;
			int renderMode = this.isContinuous ? GLSurfaceView.RENDERMODE_CONTINUOUSLY : GLSurfaceView.RENDERMODE_WHEN_DIRTY;
			if (view instanceof GLSurfaceView) ((GLSurfaceView)view).setRenderMode(renderMode);
			mean.clear();
		}
	}

	@Override
	public boolean isContinuousRendering() {
		return this.isContinuous;
	}

	@Override
	public void requestRendering() {
		if (this.view != null) {
			if (this.view instanceof GLSurfaceView) ((GLSurfaceView)this.view).requestRender();
		}	
	}

	@Override
	public boolean isFullscreen() {
		return true;
	}
	

}
