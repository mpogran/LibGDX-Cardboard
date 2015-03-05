package com.mygdx.game;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.google.vrtoolkit.cardboard.Eye;

public class EyePerspectiveCamera extends PerspectiveCamera {

	private EyePerspectiveCamera(float fieldOfView, float viewportWidth, float viewportHeight) {
		super(fieldOfView, viewportWidth, viewportHeight);
	}
	
	public static EyePerspectiveCamera createEyePerspectiveCamera(Eye eye, PerspectiveCamera camera) {
		EyePerspectiveCamera rv = new EyePerspectiveCamera(camera.fieldOfView, camera.viewportWidth, camera.viewportHeight);
		
		rv.view.set(eye.getEyeView());		
		rv.projection.set(eye.getPerspective(camera.near, camera.far));
      
		rv.combined.set(rv.projection);
		Matrix4.mul(rv.combined.val, rv.view.val);
		
		rv.invProjectionView.set(rv.combined);
		Matrix4.inv(rv.invProjectionView.val);
		rv.frustum.update(rv.invProjectionView);
		
		return rv;
	}
	
	@Override
	public void update() {
		// Do nothing -- let Cardboard handle camera movement
	}
}
