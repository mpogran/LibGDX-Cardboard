package com.mygdx.game.android;

import com.badlogic.gdx.ApplicationAdapter;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public abstract class CardboardApplicationAdapter extends ApplicationAdapter {

	public abstract void newFrame(HeadTransform transform);
	
	public abstract void finishFrame(Viewport viewport);
	
	public abstract void drawEye(Eye eye);
}
