package com.mygdx.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.google.vrtoolkit.cardboard.Eye;

public class EyePerspectiveCamera extends Camera {

	
	public static EyePerspectiveCamera createEyePerspectiveCamera(Eye eye, Camera camera) {
		EyePerspectiveCamera rv = new EyePerspectiveCamera();
		
		Matrix4 eyeView = new Matrix4(eye.getEyeView());
		eyeView.translate(-camera.position.x, -camera.position.y, -camera.position.z);
		
		rv.view.set(eyeView);		
		rv.projection.set(eye.getPerspective(camera.near, camera.far));
      
		rv.combined.set(rv.projection);
		Matrix4.mul(rv.combined.val, rv.view.val);
		
		rv.invProjectionView.set(rv.combined);
		Matrix4.inv(rv.invProjectionView.val);
		rv.frustum.update(rv.invProjectionView);
						
		return rv;
	}

	@Override
	public void update(boolean updateFrustum) {
		// Do nothing
	}

	@Override
	public void update() {
		// Do nothing -- let Cardboard handle camera movement
	}
}
