/****************************************************************************
Copyright (c) 2006, Radon Labs GmbH
Copyright (c) 2011-2013,WebJet Business Division,CYOU

http://www.genesis-3d.com.cn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
****************************************************************************/
package org.genesis.lib;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;


import org.genesis.lib.GenesisRenderer;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

public class GenesisGLSurfaceView extends GLSurfaceView {
	private final static int HANDLER_OPEN_IME_KEYBOARD = 2;
	private final static int HANDLER_CLOSE_IME_KEYBOARD = 3;
	private static final String TAG = GenesisGLSurfaceView.class.getSimpleName();

	private static Handler sHandler;
	private GenesisActivity mActivity;
	private static GenesisGLSurfaceView mGenesisGLSurfaceView;
	private static GenesisTextInputWraper sGenesisTextInputWraper;
	
	private String gamedir;
	private String scenename;
	private String packagename;
	private String apklocation;
	private float resourceSize;
	private int storageType;
	private float density;
	private boolean usePrecompilerShader;
	
	private GenesisRenderer mGenesisRenderer;
	private GenesisEditText mGenesisEditText;

	private ContextFactory mContextFactory;
	
	private AssetManager mMgr;
	//get set methods=====================================================================================
	public GenesisEditText getGenesisEditText() {
		return this.mGenesisEditText;
	}
	public void setGenesisEditText(final GenesisEditText pGenesisEditText) {
		this.mGenesisEditText = pGenesisEditText;
		if (null != this.mGenesisEditText && null != GenesisGLSurfaceView.sGenesisTextInputWraper) {
			this.mGenesisEditText.setOnEditorActionListener(GenesisGLSurfaceView.sGenesisTextInputWraper);
			this.mGenesisEditText.setGenesisGLSurfaceView(this);
			this.requestFocus();
		}
	}
	private void GetStorageType(final Context context)
	{
		String sgamedir = gamedir;
		sgamedir = "sdcard/"+sgamedir;
		int n = sgamedir.indexOf("/");
		String path = sgamedir.substring(n+1);
		path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+path;
		File desDir = new File(path);

		if(desDir.exists())
		{
			 storageType = 2;
			 return;
		}
		
	    sgamedir = gamedir;
		sgamedir = "data/data/"+packagename+File.separator+gamedir;
		n = sgamedir.indexOf("/");
		path = sgamedir.substring(n+1);
		path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+path;
		desDir = new File(path);
	
		if(desDir.exists())
		{
			 storageType = 1;
			 return;
		}
		
		File sddata = Environment.getExternalStorageDirectory();
	    float sdcardSize = GetAvailableSize(sddata);
	    
	    File data = Environment.getDataDirectory();
	    float dataSize =  GetAvailableSize(data);
	    
	    String apkpath = context.getPackageResourcePath();
	    File apk = new File(apkpath);
	    resourceSize = apk.length()/(1024*1024)*2;
	    storageType = 2;
	    if(sdcardSize<resourceSize)
	    {
	    	storageType = 1;
	    	return;
	    }
	    if(dataSize<resourceSize)
	    {
	    	storageType = 0;
	    	return;
	    }
	}
	
	private float GetAvailableSize(File data)
	{
		 StatFs sf = new StatFs(data.getPath());
		 int availableBlocks = sf.getAvailableBlocks();
		
		 int size = sf.getBlockSize();
		
		 int availableSize = availableBlocks * size;
		
		 return availableSize/(1024*1024);
	}
	
	
	private  void copyAllResFiles(final Context context,String Data,String Dest)
	{
		Log.d( "copy", "copy start");
		AssetManager assmgr = context.getAssets();

		String[] files;
		try {
			files = assmgr.list(Data);

			for(int i = 0;i<files.length;i++)
			{
				files[i] = Data+File.separator+files[i];
				
				
				int index = files[i].lastIndexOf(".");
				if(index>1)
				{
					copyAssetFile(context,files[i],Dest);
				}
				else
				{
					copyAllResFiles(context,files[i],Dest);
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d( "copy", "copy end");
	}
	
	
	
	private  void copyAssetFile(final Context context,String assetName, String target)
	{
		try
		{		
			int removeFrist = assetName.indexOf("/");
			String myassetName = assetName.substring(removeFrist);
			String desFileName = target+myassetName;
			int desFileDirStart = desFileName.lastIndexOf("/");
			String desFileDir = desFileName.substring(0,desFileDirStart);
			File desDir = new File(desFileDir);
			if(!desDir.exists())
			{
				if(!desDir.mkdirs())
				{
					Log.d( "mkdirsfailed", desFileDir);
				}
			}
	
			InputStream in = context.getAssets().open(assetName);
			File desFile = new File(desFileName);
			if(!desFile.exists())
			{
				desFile.createNewFile();	
			}
			
			FileOutputStream stream = new FileOutputStream(desFile);
			DataOutputStream fos    = new DataOutputStream(stream);
			
			byte[] buff = new byte[4096];
			int n = in.read(buff);
			
			while(n>0)
			{
				fos.write(buff,0,n);
				n = in.read(buff);
			}
			fos.close();
			in.close();		
			
		}
		catch(Exception e)
		{	
			e.printStackTrace();
			
			Log.d( "copy", e.getMessage());
			Log.d( "copy", e.toString());
		}
	
	}
	
	private String RemoveQuat(String str)
	{
		if(str.startsWith("\""))
		{
			int length = str.length();
			String out = str.substring(1, length-1);
			return out;
		}
		return str;
	}
	private void ReadConfig(final Context context)
	{		
		try {	
			
			gamedir = "sdcard/Sharp/";
			scenename = "asset:scene/Sharp.scene";
			   InputStream is = context.getAssets().open("Config.ini");
			   int count = is.available();
			   byte buffer[] = new byte[count];
			   is.read(buffer);
			   String strConfig = new String(buffer);
			   String[] strsCFG =  strConfig.split("\r\n");

			   if(  strsCFG[0].compareTo("[webjet]") != 0)
			   {
				   Log.d( "read config", "config file error!");
			   }
			   else
			   {
				   for(int i = 1 ;i<strsCFG.length ;i++)
				   {
					   String[] valuePair = strsCFG[i].split("=");
					   if(valuePair[0].compareTo("gamedir")==0)
					   {
						   gamedir = valuePair[1];
						   gamedir = RemoveQuat(gamedir);
					   }
					   else if(valuePair[0].compareTo("scenename")==0)
					   {
						   scenename = valuePair[1];
						   scenename = RemoveQuat(scenename);
					   }
					   else if(valuePair[0].compareTo("packagename")==0)
					   {
						   packagename = valuePair[1];
						   packagename = RemoveQuat(packagename);
					   }    
					   else if(valuePair[0].compareTo("usePrecompilerShader")==0)
					   {
						   String tmpStr;
						   tmpStr = valuePair[1];
						   tmpStr = RemoveQuat(tmpStr);
						   if(tmpStr.compareTo("true")==0)
						   {
							  usePrecompilerShader = true;
						   }
						   else
						   {
							   usePrecompilerShader = false;							   
						   }
						   
					   }  
					   
				   }
			   }
			   
		   } catch (IOException e) {
			   // TODO Auto-generated catch block
			   Log.d( "read config", "config exception!");
			   e.printStackTrace();
		   }
	}


	@Override
	public void onResume() {
		super.onResume();

		this.queueEvent(new Runnable() {
			@Override
			public void run() {
				GenesisGLSurfaceView.this.mGenesisRenderer.handleOnResume();
			}
		});
	}

	@Override
	public void onPause() {
		this.queueEvent(new Runnable() {
			@Override
			public void run() {
				GenesisGLSurfaceView.this.mGenesisRenderer.handleOnPause();
			}
		});

		super.onPause();
	}
	
	public void onStop()
	{
		this.queueEvent(new Runnable() {
	
			@Override
			public void run() {
				GenesisGLSurfaceView.this.mGenesisRenderer.handleOnStop();
			}
		});

	}
		
	public void onDestroy()
	{
		this.queueEvent(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				GenesisGLSurfaceView.this.mGenesisRenderer.handleOnDestroy();
			}
		});
	}
	public void setGenesisRenderer(final GenesisRenderer renderer) {
		this.mGenesisRenderer = renderer;
		this.setRenderer(this.mGenesisRenderer);
		mGenesisRenderer.SetGenesisGLSurfaceView(this);
	}
	
	public GenesisGLSurfaceView(Context context) {
		super(context);
		
		mMgr = context.getAssets();
		this.setEGLContextClientVersion(2);
		
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		
		mContextFactory = new ContextFactory();
		setEGLContextFactory(mContextFactory);
		
		setEGLConfigChooser( 
                new ConfigChooser(8, 8, 8, 8, 16, 8) );
		
		int OsVersion = android.os.Build.VERSION.SDK_INT;
		
		if (OsVersion >= 11)
		{
			//this.setPreserveEGLContextOnPause(true);
		}

		this.initView();
		apklocation = context.getPackageResourcePath();
		this.usePrecompilerShader = true;
		ReadConfig(context);
		storageType=0;
		
		if(storageType==2)
		{
			gamedir = "sdcard/"+gamedir;
		}
		else if(storageType==1)
		{
			gamedir = "data/data/"+packagename+File.separator+gamedir;
		}
		else if(storageType==0)
		{
			gamedir = apklocation+"/assets/Data";
			
			String path =  "data/data/"+packagename;
			String spath = path+"Data/Script";
			File cdesDir = new File(spath);
			File desDir = new File(path);
			if(!cdesDir.exists())
			{
				copyAllResFiles(context,"Data/Script",desDir.getAbsolutePath());
			}
			
			return;
		}
		if(storageType==2)
		{
			int n = gamedir.indexOf("/");
			String path = gamedir.substring(n+1);
			path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+path;
			File desDir = new File(path);

			if(!desDir.exists())
			{
				copyAllResFiles(context,"Data",desDir.getAbsolutePath());
			}
			gamedir = gamedir;
		}
		else if(storageType==1)
		{
			int n = gamedir.indexOf("/");
			String path = gamedir.substring(n+1);
			path = Environment.getDataDirectory().getAbsolutePath()+File.separator+path;
			File desDir = new File(path);

			if(!desDir.exists())
			{
				copyAllResFiles(context,"Data",desDir.getAbsolutePath());
			}
			gamedir = gamedir;
		}
		
		
		
	}
	protected void initView() {
		this.setFocusableInTouchMode(true);

		GenesisGLSurfaceView.mGenesisGLSurfaceView = this;
		GenesisGLSurfaceView.sGenesisTextInputWraper = new GenesisTextInputWraper(this);
	
		GenesisGLSurfaceView.sHandler = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				switch (msg.what) {
					case HANDLER_OPEN_IME_KEYBOARD:
						{
							
							
							if (null != GenesisGLSurfaceView.this.mGenesisEditText && GenesisGLSurfaceView.this.mGenesisEditText.requestFocus()) {
								GenesisGLSurfaceView.this.mGenesisEditText.removeTextChangedListener(GenesisGLSurfaceView.sGenesisTextInputWraper);
								GenesisGLSurfaceView.this.mGenesisEditText.setText("");
								final String text = (String) msg.obj;
								GenesisGLSurfaceView.this.mGenesisEditText.append(text);
								GenesisGLSurfaceView.sGenesisTextInputWraper.setOriginText(text);
								GenesisGLSurfaceView.this.mGenesisEditText.addTextChangedListener(GenesisGLSurfaceView.sGenesisTextInputWraper);
								final InputMethodManager imm = (InputMethodManager) GenesisGLSurfaceView.mGenesisGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
								imm.showSoftInput(GenesisGLSurfaceView.this.mGenesisEditText, 0);
								Log.d("GLSurfaceView", "showSoftInput");
							}
						}
						break;

					case HANDLER_CLOSE_IME_KEYBOARD:
						{
							if (null != GenesisGLSurfaceView.this.mGenesisEditText) {
								GenesisGLSurfaceView.this.mGenesisEditText.removeTextChangedListener(GenesisGLSurfaceView.sGenesisTextInputWraper);
								final InputMethodManager imm = (InputMethodManager) GenesisGLSurfaceView.mGenesisGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
								imm.hideSoftInputFromWindow(GenesisGLSurfaceView.this.mGenesisEditText.getWindowToken(), 0);
								GenesisGLSurfaceView.this.requestFocus();
								Log.d("GLSurfaceView", "HideSoftInput");
							}
						}
						break;
				}
			}
		};
	}
	

	@Override
	public boolean onTouchEvent(final MotionEvent pMotionEvent) {
		// these data are used in ACTION_MOVE and ACTION_CANCEL
		final int pointerNumber = pMotionEvent.getPointerCount();
		final int[] ids = new int[pointerNumber];
		final float[] xs = new float[pointerNumber];
		final float[] ys = new float[pointerNumber];

		for (int i = 0; i < pointerNumber; i++) {
			ids[i] = pMotionEvent.getPointerId(i);
			xs[i] = pMotionEvent.getX(i);
			ys[i] = pMotionEvent.getY(i);
		}

		switch (pMotionEvent.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_DOWN:
				final int indexPointerDown = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				final int idPointerDown = pMotionEvent.getPointerId(indexPointerDown);
				final float xPointerDown = pMotionEvent.getX(indexPointerDown);
				final float yPointerDown = pMotionEvent.getY(indexPointerDown);
						
				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionDown(idPointerDown, xPointerDown, yPointerDown);
					}
				});
				break;

			case MotionEvent.ACTION_DOWN:
				// there are only one finger on the screen
				final int idDown = pMotionEvent.getPointerId(0);
				final float xDown = xs[0];
				final float yDown = ys[0];

				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionDown(idDown, xDown, yDown);
					}
				});
				break;

			case MotionEvent.ACTION_MOVE:
				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionMove(ids, xs, ys);
					}
				});
				break;

			case MotionEvent.ACTION_POINTER_UP:
				final int indexPointUp = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				final int idPointerUp = pMotionEvent.getPointerId(indexPointUp);
				final float xPointerUp = pMotionEvent.getX(indexPointUp);
				final float yPointerUp = pMotionEvent.getY(indexPointUp);

				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionUp(idPointerUp, xPointerUp, yPointerUp);
					}
				});
				break;

			case MotionEvent.ACTION_UP:
				// there are only one finger on the screen
				final int idUp = pMotionEvent.getPointerId(0);
				final float xUp = xs[0];
				final float yUp = ys[0];

				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionUp(idUp, xUp, yUp);
					}
				});
				break;

			case MotionEvent.ACTION_CANCEL:
				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						GenesisGLSurfaceView.this.mGenesisRenderer.handleActionCancel(ids, xs, ys);
					}
				});
				break;
		}

		return true;
	}

	/*
	 * This function is called before GenesisRenderer.nativeInit(), so the
	 * width and height is correct.
	 */
	@Override
	protected void onSizeChanged(final int pNewSurfaceWidth, final int pNewSurfaceHeight, final int pOldSurfaceWidth, final int pOldSurfaceHeight) {
		if(!this.isInEditMode()) {
			
			float screenWidth = (int)(pNewSurfaceWidth * density + 0.5f);      
			float screenHeight = (int)(pNewSurfaceHeight * density + 0.5f);     
			
			this.mGenesisRenderer.setScreenWidthAndHeight(pNewSurfaceWidth, pNewSurfaceHeight,screenWidth,screenHeight,mMgr);
		               
			this.mGenesisRenderer.setConfig(gamedir,scenename,usePrecompilerShader);
			this.mGenesisRenderer.setStorage(storageType, packagename,apklocation );
		}
	}

	@Override
	public boolean onKeyDown(final int pKeyCode, final KeyEvent pKeyEvent) {
		switch (pKeyCode) {
			case KeyEvent.KEYCODE_BACK:
				this.queueEvent(new Runnable() {
					@Override
					public void run() {
						GenesisGLSurfaceView.this.mGenesisRenderer.handleKeyDown(pKeyCode);
					}
				});
				return true;
			default:
				return super.onKeyDown(pKeyCode, pKeyEvent);
		}
	}
	
	   private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
	        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
	            Log.w(TAG, "creating OpenGL ES 2.0 context");
	            checkEglError("Before eglCreateContext", egl);
	            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
	            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	            checkEglError("After eglCreateContext", egl);
	            return context;
	        }

	        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
	            egl.eglDestroyContext(display, context);
	        }
	    }

	    private static void checkEglError(String prompt, EGL10 egl) {
	        int error;
	        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
	            Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
	        }
	    }

	    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

	        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
	            mRedSize = r;
	            mGreenSize = g;
	            mBlueSize = b;
	            mAlphaSize = a;
	            mDepthSize = depth;
	            mStencilSize = stencil;
	        }

	        /* This EGL config specification is used to specify 2.0 rendering.
	         * We use a minimum size of 4 bits for red/green/blue, but will
	         * perform actual matching in chooseConfig() below.
	         */
	        private static int EGL_OPENGL_ES2_BIT = 4;
	        private static int[] s_configAttribs2 =
	        {
	            EGL10.EGL_RED_SIZE, 4,
	            EGL10.EGL_GREEN_SIZE, 4,
	            EGL10.EGL_BLUE_SIZE, 4,
	            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
	            EGL10.EGL_NONE
	        };

	        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

	            /* Get the number of minimally matching EGL configurations
	             */
	            int[] num_config = new int[1];
	            egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

	            int numConfigs = num_config[0];

	            if (numConfigs <= 0) {
	                throw new IllegalArgumentException("No configs match configSpec");
	            }

	            /* Allocate then read the array of minimally matching EGL configs
	             */
	            EGLConfig[] configs = new EGLConfig[numConfigs];
	            egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);

	            if (false) {
	                 printConfigs(egl, display, configs);
	            }
	            /* Now return the "best" one
	             */
	            return chooseConfig(egl, display, configs);
	        }

	        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
	                EGLConfig[] configs) {
	            for(EGLConfig config : configs) {
	                int d = findConfigAttrib(egl, display, config,
	                        EGL10.EGL_DEPTH_SIZE, 0);
	                int s = findConfigAttrib(egl, display, config,
	                        EGL10.EGL_STENCIL_SIZE, 0);

	                // We need at least mDepthSize and mStencilSize bits
	                if (d < mDepthSize || s < mStencilSize)
	                    continue;

	                // We want an *exact* match for red/green/blue/alpha
	                int r = findConfigAttrib(egl, display, config,
	                        EGL10.EGL_RED_SIZE, 0);
	                int g = findConfigAttrib(egl, display, config,
	                            EGL10.EGL_GREEN_SIZE, 0);
	                int b = findConfigAttrib(egl, display, config,
	                            EGL10.EGL_BLUE_SIZE, 0);
	                int a = findConfigAttrib(egl, display, config,
	                        EGL10.EGL_ALPHA_SIZE, 0);

	                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
	                    return config;
	            }
	            return null;
	        }

	        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
	                EGLConfig config, int attribute, int defaultValue) {

	            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
	                return mValue[0];
	            }
	            return defaultValue;
	        }

	        private void printConfigs(EGL10 egl, EGLDisplay display,
	            EGLConfig[] configs) {
	            int numConfigs = configs.length;
	            Log.w(TAG, String.format("%d configurations", numConfigs));
	            for (int i = 0; i < numConfigs; i++) {
	                Log.w(TAG, String.format("Configuration %d:\n", i));
	                printConfig(egl, display, configs[i]);
	            }
	        }

	        private void printConfig(EGL10 egl, EGLDisplay display,
	                EGLConfig config) {
	            int[] attributes = {
	                    EGL10.EGL_BUFFER_SIZE,
	                    EGL10.EGL_ALPHA_SIZE,
	                    EGL10.EGL_BLUE_SIZE,
	                    EGL10.EGL_GREEN_SIZE,
	                    EGL10.EGL_RED_SIZE,
	                    EGL10.EGL_DEPTH_SIZE,
	                    EGL10.EGL_STENCIL_SIZE,
	                    EGL10.EGL_CONFIG_CAVEAT,
	                    EGL10.EGL_CONFIG_ID,
	                    EGL10.EGL_LEVEL,
	                    EGL10.EGL_MAX_PBUFFER_HEIGHT,
	                    EGL10.EGL_MAX_PBUFFER_PIXELS,
	                    EGL10.EGL_MAX_PBUFFER_WIDTH,
	                    EGL10.EGL_NATIVE_RENDERABLE,
	                    EGL10.EGL_NATIVE_VISUAL_ID,
	                    EGL10.EGL_NATIVE_VISUAL_TYPE,
	                    0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
	                    EGL10.EGL_SAMPLES,
	                    EGL10.EGL_SAMPLE_BUFFERS,
	                    EGL10.EGL_SURFACE_TYPE,
	                    EGL10.EGL_TRANSPARENT_TYPE,
	                    EGL10.EGL_TRANSPARENT_RED_VALUE,
	                    EGL10.EGL_TRANSPARENT_GREEN_VALUE,
	                    EGL10.EGL_TRANSPARENT_BLUE_VALUE,
	                    0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
	                    0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
	                    0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
	                    0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
	                    EGL10.EGL_LUMINANCE_SIZE,
	                    EGL10.EGL_ALPHA_MASK_SIZE,
	                    EGL10.EGL_COLOR_BUFFER_TYPE,
	                    EGL10.EGL_RENDERABLE_TYPE,
	                    0x3042 // EGL10.EGL_CONFORMANT
	            };
	            String[] names = {
	                    "EGL_BUFFER_SIZE",
	                    "EGL_ALPHA_SIZE",
	                    "EGL_BLUE_SIZE",
	                    "EGL_GREEN_SIZE",
	                    "EGL_RED_SIZE",
	                    "EGL_DEPTH_SIZE",
	                    "EGL_STENCIL_SIZE",
	                    "EGL_CONFIG_CAVEAT",
	                    "EGL_CONFIG_ID",
	                    "EGL_LEVEL",
	                    "EGL_MAX_PBUFFER_HEIGHT",
	                    "EGL_MAX_PBUFFER_PIXELS",
	                    "EGL_MAX_PBUFFER_WIDTH",
	                    "EGL_NATIVE_RENDERABLE",
	                    "EGL_NATIVE_VISUAL_ID",
	                    "EGL_NATIVE_VISUAL_TYPE",
	                    "EGL_PRESERVED_RESOURCES",
	                    "EGL_SAMPLES",
	                    "EGL_SAMPLE_BUFFERS",
	                    "EGL_SURFACE_TYPE",
	                    "EGL_TRANSPARENT_TYPE",
	                    "EGL_TRANSPARENT_RED_VALUE",
	                    "EGL_TRANSPARENT_GREEN_VALUE",
	                    "EGL_TRANSPARENT_BLUE_VALUE",
	                    "EGL_BIND_TO_TEXTURE_RGB",
	                    "EGL_BIND_TO_TEXTURE_RGBA",
	                    "EGL_MIN_SWAP_INTERVAL",
	                    "EGL_MAX_SWAP_INTERVAL",
	                    "EGL_LUMINANCE_SIZE",
	                    "EGL_ALPHA_MASK_SIZE",
	                    "EGL_COLOR_BUFFER_TYPE",
	                    "EGL_RENDERABLE_TYPE",
	                    "EGL_CONFORMANT"
	            };
	            int[] value = new int[1];
	            for (int i = 0; i < attributes.length; i++) {
	                int attribute = attributes[i];
	                String name = names[i];
	                if ( egl.eglGetConfigAttrib(display, config, attribute, value)) {
	                    Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
	                } else {
	                    // Log.w(TAG, String.format("  %s: failed\n", name));
	                    while (egl.eglGetError() != EGL10.EGL_SUCCESS);
	                }
	            }
	        }

	        // Subclasses can adjust these values:
	        protected int mRedSize;
	        protected int mGreenSize;
	        protected int mBlueSize;
	        protected int mAlphaSize;
	        protected int mDepthSize;
	        protected int mStencilSize;
	        private int[] mValue = new int[1];
	    }

	// called by c++==========================================================================
	    public static void openIMEKeyboard() {
			final Message msg = new Message();
			msg.what = GenesisGLSurfaceView.HANDLER_OPEN_IME_KEYBOARD;
			msg.obj = GenesisGLSurfaceView.mGenesisGLSurfaceView.mGenesisRenderer.getContentText();
			GenesisGLSurfaceView.sHandler.sendMessage(msg);
		}

		public static void closeIMEKeyboard() {
			final Message msg = new Message();
			msg.what = GenesisGLSurfaceView.HANDLER_CLOSE_IME_KEYBOARD;
			GenesisGLSurfaceView.sHandler.sendMessage(msg);
		}
		//text input methods===================================================================================
		public void insertText(final String pText) {
			this.queueEvent(new Runnable() {
				@Override
				public void run() {
					GenesisGLSurfaceView.this.mGenesisRenderer.handleInsertText(pText);
				}
			});
		}
		public void deleteBackward() {
			this.queueEvent(new Runnable() {
				@Override
				public void run() {
					GenesisGLSurfaceView.this.mGenesisRenderer.handleDeleteBackward();
				}
			});
		}
}