package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class MyGdxGame extends CardboardCompatibleApplicationAdapter {
	
    private static final float TIME_DELTA = 0.3f;
	
	private ModelBatch modelBatch;
	private DecalBatch decalBatch;
	
	private Environment environment;
    private PerspectiveCamera monoCamera;
    
    private Model cubeModel;
    private Decal groundDecal;
	
	private Array<ModelInstance> instances = new Array<ModelInstance>();
	private ModelInstance cubeInstance;
	
	@Override
	public void create() {
		this.modelBatch = new ModelBatch();
		
		Vector3 cubePosition = new Vector3(0.0f, 5.0f, -5.0f);
		
		this.monoCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		this.monoCamera.position.set(0.0f, 5.0f, 0.0f);
		this.monoCamera.lookAt(cubePosition);
		this.monoCamera.near = 0.1f;
		this.monoCamera.far = 100.0f;
		this.monoCamera.update();
				
		this.environment = new Environment();
		this.environment.set(new ColorAttribute(ColorAttribute.Ambient, 0.8f, 0.8f, 0.8f, 1.0f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0.0f, -0.8f, -0.2f));
        
        ModelBuilder modelBuilder = new ModelBuilder();     
        this.cubeModel = modelBuilder.createBox(2.0f, 2.0f, 2.0f, new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal);        
        this.cubeInstance = new ModelInstance(cubeModel);
        this.cubeInstance.transform.setToTranslation(cubePosition);
        
        this.groundDecal = Decal.newDecal(200, 200, new TextureRegion(new Texture(Gdx.files.internal("grass.png"))));
        this.groundDecal.setColor(Color.LIGHT_GRAY);
        this.groundDecal.setPosition(0.0f, -10.0f, 0.0f);
        this.groundDecal.rotateX(90);
           
        this.instances.addAll(this.cubeInstance);
	}
	
	@Override
	public void newFrame(HeadTransform transform) {		
		performPerFrame();
	}

	@Override
	public void finishFrame(Viewport viewport) {
		// Do nothing
	}
	
	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		performPerFrame();
		render(this.monoCamera);
	}

	@Override
	public void drawEye(Eye eye) {
		if (eye.getType() == Eye.Type.MONOCULAR) {
			render();
		} else {
			EyePerspectiveCamera eyeCamera = EyePerspectiveCamera.createEyePerspectiveCamera(eye, this.monoCamera);
			render(eyeCamera);
		}
	}
	
	@Override
	public void dispose() {
		this.cubeModel.dispose();
		this.modelBatch.dispose();
		this.decalBatch.dispose();
	}
	
	private void performPerFrame() {
		cubeInstance.transform.rotate(0.5f, 0.5f, 1.0f, TIME_DELTA);	
	}
	
	private void render(Camera camera) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		this.decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
		this.decalBatch.add(this.groundDecal);
		this.decalBatch.flush();
		
		this.modelBatch.begin(camera);
		this.modelBatch.render(instances, environment);
		this.modelBatch.end();
	}
}
