package com.mygdx.game.android;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationBase;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AndroidClipboard;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.backends.android.AndroidInput;
import com.badlogic.gdx.backends.android.AndroidInputFactory;
import com.badlogic.gdx.backends.android.AndroidNet;
import com.badlogic.gdx.backends.android.AndroidPreferences;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

public class AndroidCardboardApplication extends CardboardActivity implements AndroidApplicationBase {
	static {
		GdxNativesLoader.load();
	}
	
	protected AndroidCardboardGraphics graphics;
	protected AndroidInput input;
	protected AndroidAudio audio;
	protected AndroidFiles files;
	protected AndroidNet net;
	protected CardboardApplicationAdapter listener;
	public Handler handler;
	protected final Array<Runnable> runnables = new Array<Runnable>();
	protected final Array<Runnable> executedRunnables = new Array<Runnable>();
	protected final Array<LifecycleListener> lifecycleListeners = new Array<LifecycleListener>();
	protected int logLevel = LOG_INFO;
	
	public void initialize (CardboardApplicationAdapter listener, AndroidApplicationConfiguration config) {
		if (this.getVersion() < MINIMUM_SDK) {
			throw new GdxRuntimeException("LibGDX requires Android API Level " + MINIMUM_SDK + " or later.");
		}        
		this.graphics = new AndroidCardboardGraphics(this, config);
		this.input = AndroidInputFactory.newAndroidInput(this, this, graphics.getView(), config);
		this.audio = new AndroidAudio(this, config);
		this.getFilesDir(); // workaround for Android bug #10515463
		this.files = new AndroidFiles(this.getAssets(), this.getFilesDir().getAbsolutePath());
		this.net = new AndroidNet(this);
		
		this.listener = listener;
		this.handler = new Handler();
		
		Gdx.app = this;
		Gdx.input = this.getInput();
		Gdx.audio = this.getAudio();
		Gdx.files = this.getFiles();
		Gdx.graphics = this.getGraphics();
		Gdx.net = this.getNet();
		
		try {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception ex) {
			log("AndroidApplication", "Content already displayed, cannot request FEATURE_NO_TITLE", ex);
		}
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		setContentView(graphics.getView(), createLayoutParams());
	}
	
	protected FrameLayout.LayoutParams createLayoutParams () {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}

	@Override
	public CardboardApplicationAdapter getApplicationListener() {
		return this.listener;
	}

	@Override
	public Graphics getGraphics() {
		return this.graphics;
	}

	@Override
	public Audio getAudio() {
		return this.audio;
	}

	@Override
	public Files getFiles() {
		return this.files;
	}

	@Override
	public Net getNet() {
		return this.net;
	}

	@Override
	public void debug (String tag, String message) {
		if (logLevel >= LOG_DEBUG) {
			Log.d(tag, message);
		}
	}

	@Override
	public void debug (String tag, String message, Throwable exception) {
		if (logLevel >= LOG_DEBUG) {
			Log.d(tag, message, exception);
		}
	}

	@Override
	public void log (String tag, String message) {
		if (logLevel >= LOG_INFO) Log.i(tag, message);
	}

	@Override
	public void log (String tag, String message, Throwable exception) {
		if (logLevel >= LOG_INFO) Log.i(tag, message, exception);
	}

	@Override
	public void error (String tag, String message) {
		if (logLevel >= LOG_ERROR) Log.e(tag, message);
	}

	@Override
	public void error (String tag, String message, Throwable exception) {
		if (logLevel >= LOG_ERROR) Log.e(tag, message, exception);
	}

	@Override
	public void setLogLevel (int logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public int getLogLevel () {
		return logLevel;
	}

	@Override
	public ApplicationType getType() {
		return ApplicationType.Android;
	}

	@Override
	public int getVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}

	@Override
	public long getJavaHeap () {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	@Override
	public long getNativeHeap () {
		return Debug.getNativeHeapAllocatedSize();
	}

	@Override
	public Preferences getPreferences (String name) {
		return new AndroidPreferences(getSharedPreferences(name, Context.MODE_PRIVATE));
	}

	AndroidClipboard clipboard;

	@Override
	public Clipboard getClipboard () {
		if (this.clipboard == null) {
			this.clipboard = new AndroidClipboard(this);
		}
		return this.clipboard;
	}


	@Override
	public void postRunnable (Runnable runnable) {
		synchronized (this.runnables) {
			this.runnables.add(runnable);
			Gdx.graphics.requestRendering();
		}
	}

	@Override
	public void exit () {
		this.handler.post(new Runnable() {
			@Override
			public void run () {
				AndroidCardboardApplication.this.finish();
			}
		});
	}

	@Override
	public void addLifecycleListener (LifecycleListener listener) {
		synchronized (this.lifecycleListeners) {
			this.lifecycleListeners.add(listener);
		}
	}

	@Override
	public void removeLifecycleListener (LifecycleListener listener) {
		synchronized (this.lifecycleListeners) {
			this.lifecycleListeners.removeValue(listener, true);
		}
	}

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public Array<Runnable> getRunnables() {
		return this.runnables;
	}

	@Override
	public Array<Runnable> getExecutedRunnables() {
		return this.executedRunnables;
	}

	@Override
	public AndroidInput getInput() {
		return this.input;
	}

	@Override
	public Array<LifecycleListener> getLifecycleListeners() {
		return this.lifecycleListeners;
	}

	@Override
	public Window getApplicationWindow() {
		return this.getWindow();
	}

	@Override
	public void useImmersiveMode(boolean b) {
		// Do nothing
	}

	@Override
	public Handler getHandler() {
		return this.handler;
	}

}
