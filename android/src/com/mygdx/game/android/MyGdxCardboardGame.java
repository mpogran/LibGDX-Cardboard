package com.mygdx.game.android;

import android.opengl.Matrix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class MyGdxCardboardGame extends CardboardApplicationAdapter {
	
	private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;
	
	private PerspectiveCamera camera;
	private DirectionalLight light;
	private Environment environment;
	private ModelBatch modelBatch;
	private Model model;
	private ModelInstance instance;
	
	private Matrix4 headView = new Matrix4();

	@Override
	public void newFrame(HeadTransform transform) {
		//Matrix.rotateM(instance.transform.val, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
		instance.transform.rotate(0.5f, 0.5f, 1.0f, TIME_DELTA);
		
		//Matrix.setLookAtM(camera.view.val, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		camera.lookAt(0.0f, 0.0f, 0.0f);
		
		transform.getHeadView(headView.val, 0);
	}

	@Override
	public void finishFrame(Viewport viewport) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawEye(Eye eye) {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
//		
//        //Matrix.multiplyMM(view.val, 0, eye.getEyeView(), 0, camera.view.val, 0);
//		camera.transform( new Matrix4(eye.getEyeView()));
//		
//		camera.update();
//
//        //Matrix.multiplyMV(lightPosInEyeSpace.val, 0, view.val, 0,new float[]{light.direction.x, light.direction.y, light.direction.z, 1.0f}, 0);
//		light.direction.mul(camera.view	);
//
//        //float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
//        float[] perspective = eye.getPerspective(camera.near, camera.far);
//        
        //Matrix.multiplyMM(mModelView, 0, mView, 0, mModelCube, 0);
        //Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
        //drawCube();
//        instance.transform.mul(camera.view);
        this.modelBatch.begin(camera);
        this.modelBatch.render(instance, environment);
        this.modelBatch.end();

//        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelFloor, 0);
//        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
//        drawFloor();
	}
	
	@Override
	public void create() {
		this.modelBatch = new ModelBatch();
		
		this.light = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
		
		this.environment = new Environment();
		this.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		this.environment.add(this.light);
		
		this.camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		this.camera.position.set(10f, 10f, 10f);
		this.camera.lookAt(0,0,0);
		this.camera.near = 1f;
        this.camera.far = 300f;
        this.camera.update();
        
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(5f, 5f, 5f, new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal);
        instance = new ModelInstance(model);
	}

	@Override
	public void render() {

//        
//        this.modelBatch.begin(this.camera);
//        this.modelBatch.render(instance, environment);
//        this.modelBatch.end();
	}
	
	@Override
	public void dispose() {
		this.model.dispose();
	}
}
