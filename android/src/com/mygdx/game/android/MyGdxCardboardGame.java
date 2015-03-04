package com.mygdx.game.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

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
	private static final float Z_NEAR = 0.1f;
	private static final float Z_FAR = 100.0f;

	private static final float CAMERA_Z = 0.01f;
	private static final float TIME_DELTA = 0.3f;

	private static final float YAW_LIMIT = 0.12f;
	private static final float PITCH_LIMIT = 0.12f;

	private static final int COORDS_PER_VERTEX = 3;

	// We keep the light always position just above the user.
	private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] { 0.0f, 2.0f, 0.0f, 1.0f };

	private final float[] lightPosInEyeSpace = new float[4];

	private FloatBuffer floorVertices;
	private FloatBuffer floorColors;
	private FloatBuffer floorNormals;

	private FloatBuffer cubeVertices;
	private FloatBuffer cubeColors;
	private FloatBuffer cubeFoundColors;
	private FloatBuffer cubeNormals;

	private int cubeProgram;
	private int floorProgram;

	private int cubePositionParam;
	private int cubeNormalParam;
	private int cubeColorParam;
	private int cubeModelParam;
	private int cubeModelViewParam;
	private int cubeModelViewProjectionParam;
	private int cubeLightPosParam;

	private int floorPositionParam;
	private int floorNormalParam;
	private int floorColorParam;
	private int floorModelParam;
	private int floorModelViewParam;
	private int floorModelViewProjectionParam;
	private int floorLightPosParam;

	private float[] modelCube;
	private float[] camera;
	private float[] view;
	private float[] headView;
	private float[] modelViewProjection;
	private float[] modelView;
	private float[] modelFloor;

	private int score = 0;
	private float objectDistance = 12f;
	private float floorDepth = 20f;
	
	private AndroidCardboardApplication app;
	  
	public MyGdxCardboardGame(AndroidCardboardApplication app) {
	    modelCube = new float[16];
	    camera = new float[16];
	    view = new float[16];
	    modelViewProjection = new float[16];
	    modelView = new float[16];
	    modelFloor = new float[16];
	    headView = new float[16];
	    
	    this.app = app;
	}
	
	
	@Override
	public void newFrame(HeadTransform headTransform) {
	    // Build the Model part of the ModelView matrix.
	    Matrix.rotateM(modelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

	    // Build the camera matrix and apply it to the ModelView.
	    Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

	    headTransform.getHeadView(headView, 0);
	}

	@Override
	public void finishFrame(Viewport viewport) {
	}

	@Override
	public void drawEye(Eye eye) {
	    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

	    // Apply the eye transformation to the camera.
	    Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

	    // Set the position of the light
	    Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

	    // Build the ModelView and ModelViewProjection matrices
	    // for calculating cube position and light.
	    float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
	    Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
	    Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
	    drawCube();

	    // Set modelView for the floor, so we draw floor in the correct location
	    Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
	    Matrix.multiplyMM(modelViewProjection, 0, perspective, 0,
	      modelView, 0);
	    drawFloor();

	}
	
	  public void drawCube() {
		    GLES20.glUseProgram(cubeProgram);

		    GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

		    // Set the Model in the shader, used to calculate lighting
		    GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelCube, 0);

		    // Set the ModelView in the shader, used to calculate lighting
		    GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

		    // Set the position of the cube
		    GLES20.glVertexAttribPointer(cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
		        false, 0, cubeVertices);

		    // Set the ModelViewProjection matrix in the shader.
		    GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

		    // Set the normal positions of the cube, again for shading
		    GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, cubeNormals);
		    GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0,cubeColors);

		    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
		  }

		  /**
		   * Draw the floor.
		   *
		   * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
		   * position of the light, so if we rewrite our code to draw the floor first, the lighting might
		   * look strange.
		   */
		  public void drawFloor() {
		    GLES20.glUseProgram(floorProgram);

		    // Set ModelView, MVP, position, normals, and color.
		    GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
		    GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
		    GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
		    GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false,
		        modelViewProjection, 0);
		    GLES20.glVertexAttribPointer(floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
		        false, 0, floorVertices);
		    GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
		        floorNormals);
		    GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);

		    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

		  }
	
	@Override
	public void create() {
		GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

	    ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COORDS.length * 4);
	    bbVertices.order(ByteOrder.nativeOrder());
	    cubeVertices = bbVertices.asFloatBuffer();
	    cubeVertices.put(WorldLayoutData.CUBE_COORDS);
	    cubeVertices.position(0);

	    ByteBuffer bbColors = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COLORS.length * 4);
	    bbColors.order(ByteOrder.nativeOrder());
	    cubeColors = bbColors.asFloatBuffer();
	    cubeColors.put(WorldLayoutData.CUBE_COLORS);
	    cubeColors.position(0);

	    ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(
	        WorldLayoutData.CUBE_FOUND_COLORS.length * 4);
	    bbFoundColors.order(ByteOrder.nativeOrder());
	    cubeFoundColors = bbFoundColors.asFloatBuffer();
	    cubeFoundColors.put(WorldLayoutData.CUBE_FOUND_COLORS);
	    cubeFoundColors.position(0);

	    ByteBuffer bbNormals = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_NORMALS.length * 4);
	    bbNormals.order(ByteOrder.nativeOrder());
	    cubeNormals = bbNormals.asFloatBuffer();
	    cubeNormals.put(WorldLayoutData.CUBE_NORMALS);
	    cubeNormals.position(0);

	    // make a floor
	    ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COORDS.length * 4);
	    bbFloorVertices.order(ByteOrder.nativeOrder());
	    floorVertices = bbFloorVertices.asFloatBuffer();
	    floorVertices.put(WorldLayoutData.FLOOR_COORDS);
	    floorVertices.position(0);

	    ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
	    bbFloorNormals.order(ByteOrder.nativeOrder());
	    floorNormals = bbFloorNormals.asFloatBuffer();
	    floorNormals.put(WorldLayoutData.FLOOR_NORMALS);
	    floorNormals.position(0);

	    ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS.length * 4);
	    bbFloorColors.order(ByteOrder.nativeOrder());
	    floorColors = bbFloorColors.asFloatBuffer();
	    floorColors.put(WorldLayoutData.FLOOR_COLORS);
	    floorColors.position(0);

	    int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
	    int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
	    int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

	    cubeProgram = GLES20.glCreateProgram();
	    GLES20.glAttachShader(cubeProgram, vertexShader);
	    GLES20.glAttachShader(cubeProgram, passthroughShader);
	    GLES20.glLinkProgram(cubeProgram);
	    GLES20.glUseProgram(cubeProgram);


	    cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
	    cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
	    cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

	    cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
	    cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
	    cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
	    cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

	    GLES20.glEnableVertexAttribArray(cubePositionParam);
	    GLES20.glEnableVertexAttribArray(cubeNormalParam);
	    GLES20.glEnableVertexAttribArray(cubeColorParam);


	    floorProgram = GLES20.glCreateProgram();
	    GLES20.glAttachShader(floorProgram, vertexShader);
	    GLES20.glAttachShader(floorProgram, gridShader);
	    GLES20.glLinkProgram(floorProgram);
	    GLES20.glUseProgram(floorProgram);


	    floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
	    floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
	    floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
	    floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

	    floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
	    floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
	    floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

	    GLES20.glEnableVertexAttribArray(floorPositionParam);
	    GLES20.glEnableVertexAttribArray(floorNormalParam);
	    GLES20.glEnableVertexAttribArray(floorColorParam);


	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

	    // Object first appears directly in front of user.
	    Matrix.setIdentityM(modelCube, 0);
	    Matrix.translateM(modelCube, 0, 0, 0, -objectDistance);

	    Matrix.setIdentityM(modelFloor, 0);
	    Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.
	}
	
	  private int loadGLShader(int type, int resId) {
		    String code = readRawTextFile(resId);
		    int shader = GLES20.glCreateShader(type);
		    GLES20.glShaderSource(shader, code);
		    GLES20.glCompileShader(shader);

		    // Get the compilation status.
		    final int[] compileStatus = new int[1];
		    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

		    // If the compilation failed, delete the shader.
		    if (compileStatus[0] == 0) {
		      GLES20.glDeleteShader(shader);
		      shader = 0;
		    }

		    if (shader == 0) {
		      throw new RuntimeException("Error creating shader.");
		    }

		    return shader;
		  }
	  
	  private String readRawTextFile(int resId) {
		    InputStream inputStream = this.app.getContext().getResources().openRawResource(resId);
		    try {
		      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		      StringBuilder sb = new StringBuilder();
		      String line;
		      while ((line = reader.readLine()) != null) {
		        sb.append(line).append("\n");
		      }
		      reader.close();
		      return sb.toString();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		    return null;
		  }
	
	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void render() {	

	}
	
	@Override
	public void dispose() {
	}
}
