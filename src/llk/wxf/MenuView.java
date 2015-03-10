package llk.wxf;

import java.util.Vector;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MenuView extends SurfaceView implements Callback, Runnable {
	private SurfaceHolder sfh;
	private Paint paint;
	private Canvas canvas;
	public int screenW, screenH;
	private Resources res;

	private Thread th;
	private boolean flag;

	private Bitmap menu;
	private Bitmap[] btnBmp;

	/*
	 * 记录坐标值 strPos[i][0]：宽度 strPos[i][1]：高度 strPos[i][2]：x坐标 strPos[i][3]：y坐标
	 */
	private int btnPos[][], btnW, btnH;
	private MainActivity context;

	private int downindex, upindex, moveindex, mpreindex;
	private boolean isTouchDown;
	private final int START = 0, SET = 1, HELP = 2, EXIT = 3;

	//使用声音和震动资源的地方：onTouchEvent中的touchdown和touchmove中两处。

	public MenuView(Context context) {
		super(context);
		this.context = (MainActivity) context;
		// TODO Auto-generated constructor stub
		sfh = this.getHolder();
		sfh.addCallback(this);
		paint = new Paint();
		paint.setColor(Color.WHITE);
		this.setFocusable(true);
		this.setFocusableInTouchMode(true);

		// myDraw();
		res = this.getResources();
		menu = BitmapFactory.decodeResource(res, R.drawable.menu_bg);
		btnBmp = new Bitmap[8];
		btnBmp[0] = BitmapFactory.decodeResource(res, R.drawable.start);
		btnBmp[1] = BitmapFactory.decodeResource(res, R.drawable.set);
		btnBmp[2] = BitmapFactory.decodeResource(res, R.drawable.help);
		btnBmp[3] = BitmapFactory.decodeResource(res, R.drawable.exit);
		btnBmp[4] = BitmapFactory.decodeResource(res, R.drawable.start_pressed);
		btnBmp[5] = BitmapFactory.decodeResource(res, R.drawable.set_pressed);
		btnBmp[6] = BitmapFactory.decodeResource(res, R.drawable.help_pressed);
		btnBmp[7] = BitmapFactory.decodeResource(res, R.drawable.exit_pressed);

	}

	int mx, my, ti;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mx = (int) event.getX();
		my = (int) event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downindex = -1;
			for (ti = 0; ti < 4; ti++) {
				if ((mx > btnPos[ti][0]) && (mx < btnPos[ti][0] + btnW)) {
					if ((my > btnPos[ti][1]) && (my < (btnPos[ti][1] + btnH))) {
						downindex = ti;
						moveindex = downindex;
						mpreindex = downindex;
						isTouchDown = true;
						switch (MainActivity.tipType) {
						case MainActivity.SOUND:
							context.sp.play(context.touchSound, 1f, 1f, 1, 0, 1);
							break;
						case MainActivity.VIBRATE:
							context.vibrator.vibrate(20);
							break;
						}
					}
				}
			}
		case MotionEvent.ACTION_MOVE:
			moveindex = -1;
			for (ti = 0; ti < 4; ti++) {
				if ((mx > btnPos[ti][0]) && (mx < btnPos[ti][0] + btnW)) {
					if ((my > btnPos[ti][1]) && (my < (btnPos[ti][1] + btnH))) {
						moveindex = ti;
						if (moveindex != mpreindex) {
							mpreindex = moveindex;
							switch (MainActivity.tipType) {
							case MainActivity.SOUND:
								context.sp.play(context.touchSound, 1f, 1f, 1, 0, 1);
								break;
							case MainActivity.VIBRATE://new long[]{100,100,100,500},-1
								context.vibrator.vibrate(20);
								break;
							}
						}
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			upindex = -1;
			for (ti = 0; ti < 4; ti++) {
				if ((mx > btnPos[ti][0]) && (mx < btnPos[ti][0] + btnW)) {
					if ((my > btnPos[ti][1]) && (my < (btnPos[ti][1] + btnH))) {
						upindex = ti;
						// Log.v("get Index", downindex + "");
						if (upindex == downindex) {
							switch (upindex) {
							case START:
								//this.surfaceDestroyed(sfh);
								context.showGameView();
								break;
							case SET:
								context.showSetView();
								break;
							case HELP:
								context.showHelpView();
								String helpStr = "关于游戏\n游戏规则：两个相同的图标如果可以用三根以内的直线连接在一起就可以消除，" +
										"在规定的时间内消除所有图标即获得胜利。单线连接10分，双线25分，三线35分。每消除一对图标，" +
										"时间加两秒。\n游戏模式：关卡模式共有六个关卡，牛刀小试（45秒2轮）、初显身手（35秒2轮）、" +
										"与众不同（55秒3轮）、刮目相看（50秒3轮）、出神入化（45秒3轮）、登峰造极（40秒3轮）；" +
										"闯关模式共有50秒时间。最高成绩保存在设置页面。\n摇乱与提示：在游戏界面有摇乱和提示按钮。" +
										"摇乱功能将所有图标随机交换位置，提示功能显示当前可以被消除的两个图标。两个功能的初始次数为5次，" +
										"分数每涨500分，各加一次。";
								splitStr(helpStr);
								//viewState = HELPVIEW;
								break;
							case EXIT:
								//context.onKeyUp(KeyEvent.KEYCODE_BACK, null);
								context.onDestroy();
								System.exit(0);
								break;
							}
						}
					}
				}
			}
			isTouchDown = false;
			break;
		}
		//Log.v("MenuView", "down" + downindex + "*" + "up" + upindex);
		return true;
	}

	/**
	 * 按键按下事件监听
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		//Log.v("onKeyDown of GameView:", "in");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (context.currentView == MainActivity.HELPVIEW) {
				context.currentView = MainActivity.MENUVIEW;
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	int di;
	int dp, dpx, dpy;
	String dstr;

	public void myDraw() {
		// Log.v("MenuView", "begin draw");
		canvas = sfh.lockCanvas();

		paint.setStyle(Paint.Style.FILL);
		switch (context.currentView) {
		case MainActivity.MENUVIEW:
			canvas.drawBitmap(menu, 0, 0, paint);
			for (di = 0; di < 4; di++) {
				if (isTouchDown && (di == moveindex)) {
					canvas.drawBitmap(btnBmp[di + 4], btnPos[di][0],
							btnPos[di][1], paint);
				} else {
					canvas.drawBitmap(btnBmp[di], btnPos[di][0], btnPos[di][1],
							paint);
				}
			}
			break;
		case MainActivity.HELPVIEW:
			canvas.drawColor(Color.LTGRAY);
			paint.setColor(Color.BLACK);
			paint.setAntiAlias(true);
			// canvas.clipRect(0, 0, screenW, screenH);
			for (di = 0; di < lines; di++) {
				dstr = subStrVec.elementAt(di);
				dp = subPosVec.elementAt(di);
				dpy = dp & 0xffff;
				dpx = (dp >> 16) & 0xffff;
				canvas.drawText(dstr, dpx, dpy, paint);
			}
			break;
		}

		sfh.unlockCanvasAndPost(canvas);
	}

	private Vector<String> subStrVec = null;
	private Vector<Integer> subPosVec = null;
	private int lines;
	public void splitStr(String mainStr) {
		int i, strLen = mainStr.length();
		if (strLen == 0) {
			return;
		}
		if (subStrVec != null) {
			subStrVec.removeAllElements();
		} else {
			subStrVec = new Vector<String>();
		}
		if (subPosVec != null) {
			subPosVec.removeAllElements();
		} else {
			subPosVec = new Vector<Integer>();
		}
		// subColor = new Vector<Character>();

		char ch;
		String tmp;
		int start, end, px, py;
		float len;

		float[] chLen = new float[strLen];
		paint.getTextWidths(mainStr, chLen);

		start = 0;
		len = 0;
		lines = 0;
		//Log.v("split string", "come in");
		for (i = 0; i < strLen; i++) {
			ch = mainStr.charAt(i);
			if (ch == '\n') {// 碰到‘/n’需要换行，并且下一行首行缩进。
				end = i;
				tmp = mainStr.substring(start, end);
				subStrVec.add(tmp);
				lines++;

				// 首行居中
				if (lines == 1) {
					px = (int) ((screenW - (end - start) * paint.getTextSize()) / 2);
					py = (int) (lines * paint.getTextSize());
					subPosVec.add((px << 16) | py);
				}

				// 跳过该标识符
				start = end + 1;
				i++;

				// 计算的是下一行的坐标
				len = 2 * paint.getTextSize();
				px = (int) len;
				py = (int) ((lines + 1) * paint.getTextSize());
				subPosVec.add((px << 16) | py);

				//Log.v("split string" + lines, tmp);
				continue;
			}
			len += chLen[i];
			if (len > screenW) {
				i--;
				end = i;
				tmp = mainStr.substring(start, end);
				subStrVec.add(tmp);
				lines++;
				start = end;
				len = 0;
				px = (int) len;
				py = (int) ((lines + 1) * paint.getTextSize());
				subPosVec.add((px << 16) | py);
				//Log.v("split string" + lines, tmp);
			}
			// Log.v("line if splitStr", lines + "-" + "tttt");
		}
		if (len > 0) {
			end = i;
			tmp = mainStr.substring(start, end);
			subStrVec.add(tmp);
			lines++;
			start = end;
			len = 0;
			//Log.v("split string" + lines, tmp);
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		Log.v("MenuView:", "surfaceCreated");
		screenW = this.getWidth();// 320;//
		screenH = this.getHeight();// 455;//
		if((screenW > 360) && (screenH > 640)){
			MainActivity.screenType = MainActivity.WVGA;
			paint.setTextSize(25);
		}else{
			MainActivity.screenType = MainActivity.HVGA;
			paint.setTextSize(15);
		}
		
		btnPos = new int[4][2];
		btnW = btnBmp[0].getWidth();
		btnH = btnBmp[0].getHeight();

		int i, x = (screenW - btnW) >> 1;
		for (i = 0; i < 4; i++) {
			btnPos[i][0] = x;
		}
		btnPos[0][1] = screenH / 3 + btnH;
		btnPos[1][1] = btnPos[0][1] + 2 * btnH;
		btnPos[2][1] = btnPos[1][1] + 2 * btnH;
		btnPos[3][1] = btnPos[2][1] + 2 * btnH;

		isTouchDown = false;

		this.requestFocus();
		th = new Thread(this);
		flag = true;
		th.start();
		// myDraw();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		Log.v("MenuView:", "surfaceDestroyed");
		flag = false;
		// this.setFocusable(false);
		// this.setFocusableInTouchMode(false);
	}

	private long start, during;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (flag) {
			start = System.currentTimeMillis();
			myDraw();
			during = System.currentTimeMillis() - start;
			if (during < 250) {
				try {
					Thread.sleep(250 - during);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
