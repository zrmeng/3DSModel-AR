package com.example.dsmodel_ar;

import java.io.InputStream;
import java.lang.reflect.Field;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager.LayoutParams;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

public class MainActivity extends Activity {

	// Used to handle pause and resume...
		private static MainActivity master = null;

		private GLSurfaceView mGLView;
		private MyRenderer renderer = null;
		private FrameBuffer fb = null;
		private World world = null;

		private float touchTurn = 0;
		private float touchTurnUp = 0;

		private float xpos = -1;
		private float ypos = -1;

		private Object3D cube = null;
		private int fps = 0;

		private Light sun = null;

		protected void onCreate(Bundle savedInstanceState) {

			Logger.log("onCreate");

			if (master != null) {
				copy(master);
			}

			super.onCreate(savedInstanceState);
			mGLView = new GLSurfaceView(this);
			
			mGLView.setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
			mGLView.getHolder().setFormat( PixelFormat.TRANSLUCENT );
			
			renderer = new MyRenderer();
			mGLView.setRenderer(renderer);
			setContentView(mGLView);
			
			CameraView cameraView = new CameraView( this );
	        addContentView( cameraView, new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT ) );
		}

		@Override
		protected void onPause() {
			super.onPause();
			mGLView.onPause();
		}

		@Override
		protected void onResume() {
			super.onResume();
			mGLView.onResume();
		}

		@Override
		protected void onStop() {
			super.onStop();
		}

		private void copy(Object src) {
			try {
				Logger.log("Copying data from master Activity!");
				Field[] fs = src.getClass().getDeclaredFields();
				for (Field f : fs) {
					f.setAccessible(true);
					f.set(this, f.get(src));
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public boolean onTouchEvent(MotionEvent me) {

			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				xpos = me.getX();
				ypos = me.getY();
				return true;
			}

			if (me.getAction() == MotionEvent.ACTION_UP) {
				xpos = -1;
				ypos = -1;
				touchTurn = 0;
				touchTurnUp = 0;
				return true;
			}

			if (me.getAction() == MotionEvent.ACTION_MOVE) {
				float xd = me.getX() - xpos;
				float yd = me.getY() - ypos;

				xpos = me.getX();
				ypos = me.getY();

				touchTurn = xd / -100f;
				touchTurnUp = yd / -100f;
				return true;
			}

			try {
				Thread.sleep(15);
			} catch (Exception e) {
				// No need for this...
			}

			return super.onTouchEvent(me);
		}

		protected boolean isFullscreenOpaque() {
			return true;
		}

		class MyRenderer implements GLSurfaceView.Renderer {

			private long time = System.currentTimeMillis();

			public MyRenderer() {
			}

			public void onSurfaceChanged(GL10 gl, int w, int h) {
				if (fb != null) {
					fb.dispose();
				}
				fb = new FrameBuffer(gl, w, h);

				if (master == null) {

					world = new World();
					world.setAmbientLight(20, 20, 20);

					sun = new Light(world);
					sun.setIntensity(250, 250, 250);

					Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.monster)), 512, 512));
					TextureManager.getInstance().addTexture("texture", texture);

					cube = loadModel("res/raw/monster.3ds", (float) 0.5);
					cube.setTexture("texture");
					cube.build();

					world.addObject(cube);

					Camera cam = world.getCamera();
					cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
					cam.lookAt(cube.getTransformedCenter());

					SimpleVector sv = new SimpleVector();
					sv.set(cube.getTransformedCenter());
					sv.y -= 100;
					sv.z -= 100;
					sun.setPosition(sv);
					MemoryHelper.compact();

					if (master == null) {
						Logger.log("Saving master Activity!");
						master = MainActivity.this;
					}
				}
			}

			public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			}

			public void onDrawFrame(GL10 gl) {
				if (touchTurn != 0) {
					cube.rotateY(touchTurn);
					touchTurn = 0;
				}

				if (touchTurnUp != 0) {
					cube.rotateX(touchTurnUp);
					touchTurnUp = 0;
				}

				fb.clear();
				world.renderScene(fb);
				world.draw(fb);
				fb.display();

				if (System.currentTimeMillis() - time >= 1000) {
					Logger.log(fps + "fps");
					fps = 0;
					time = System.currentTimeMillis();
				}
				fps++;
			}
			
			private Object3D loadModel(String filename, float scale){
				InputStream stream = getResources().openRawResource(R.raw.monster);
		        Object3D[] model = Loader.load3DS(stream, scale);
		        Object3D o3d = new Object3D(0);
		        Object3D temp = null;
		        for (int i = 0; i < model.length; i++) {
		            temp = model[i];
		            temp.setCenter(SimpleVector.ORIGIN);
		            temp.rotateX((float)( -.5*Math.PI));
		            temp.rotateMesh();
		            temp.setRotationMatrix(new Matrix());
		            o3d = Object3D.mergeObjects(o3d, temp);
		            o3d.build();
		        }
		        return o3d;
		    }
		}

}
