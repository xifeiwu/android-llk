package llk.wxf;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

	public Vibrator vibrator;
	public SoundPool sp;
	public int touchSound, matchSound, gameoverSound;
	private SharedPreferences sharedData;
	private Editor editor;

	public static int bestLevel, bestScore;
	
	public static final int WVGA = 1, HVGA = 2;
	public static int screenType;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//全屏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		//震动与声音
		vibrator = (Vibrator) getApplication().getSystemService(
				Service.VIBRATOR_SERVICE);
		sp = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
		touchSound = sp.load(this, R.raw.touch, 1);
		matchSound = sp.load(this, R.raw.match, 1);
		gameoverSound = sp.load(this, R.raw.gameover, 1);

		// 永久存储
		sharedData = this
				.getSharedPreferences("KLLLK", Context.MODE_PRIVATE);
		editor = sharedData.edit();
		
		tipType = SOUND;
		picType = QQFACE;
		gameType = LEVEL;
		bestLevel = 0;
		bestScore = 0;
		int tempIndex;
		tempIndex = sharedData.getInt("TipAndPicAndGameType", -1);
		if (tempIndex != -1) {
			tipType = tempIndex & 0xf;
			picType = (tempIndex >> 4) & 0xf;
			gameType = (tempIndex >> 8) & 0xf;
		}
		tempIndex = sharedData.getInt("LevelAndScore", -1);
		if (tempIndex != -1) {
			bestScore = tempIndex & 0xffff;
			bestLevel = (tempIndex >> 16) & 0xffff;
		}
		//Log.v("SharedPreference:", tipType + "-" + picType);
		showMenuView();
	}

	//界面宏
	public static final int MENUVIEW = 1, HELPVIEW = 2, SETTINGVIEW = 3, LEVELGAMEVIEW = 4, 
			SCOREGAMEVIEW = 5, GOLDPOINTSVIEW = 6;
	public int currentView;
	
	public void showMenuView() {
		currentView = MENUVIEW;
		setContentView(new MenuView(this));
	}
	
	public void showHelpView(){
		currentView = HELPVIEW;		
	}
	
	public void showGameView() {
		switch(gameType){
		case LEVEL:
			currentView = LEVELGAMEVIEW;
			if(screenType == WVGA){
				setContentView(new WVGALevelGameView(this));
			}else{
				setContentView(new HVGALevelGameView(this));
			}
			break;
		case SCORE:
			currentView = SCOREGAMEVIEW;
			if(screenType == WVGA){
				setContentView(new WVGAScoreGameView(this));
			}else{
				setContentView(new HVGAScoreGameView(this));
			}
			break;			
		}
	}

	//游戏中的各种参数
	public static final int SOUND = 1, VIBRATE = 2, NONE = 3, QQFACE = 5,
			TAOFACE = 6, LEVEL = 8, SCORE = 9;
	public static int tipType, picType, gameType;
	public String[] levelStr = { "牛刀小试", "初显身手", "与众不同", "刮目相看", "出神入化", "登峰造极"};
	
	public void showSetView() {
		currentView = SETTINGVIEW;
		setContentView(R.layout.setting);
		Button set_btn = (Button) findViewById(R.id.set_ok_btn);
		final TextView textview_setting = (TextView) findViewById(R.id.textview_setting);
		final TextView textview_tip = (TextView) findViewById(R.id.textview_tip);
		final TextView textview_pic = (TextView) findViewById(R.id.textview_pic);
		final TextView textview_gametype = (TextView) findViewById(R.id.textview_gametype);
		final RadioButton sound_rb = (RadioButton) findViewById(R.id.sound_rb);
		final RadioButton vibrate_rb = (RadioButton) findViewById(R.id.vibrate_rb);
		final RadioButton none_rb = (RadioButton) findViewById(R.id.none_rb);
		final RadioButton qqface_rb = (RadioButton) findViewById(R.id.qqface_rb);
		final RadioButton taoface_rb = (RadioButton) findViewById(R.id.taoface_rb);
		final RadioButton level_rb = (RadioButton) findViewById(R.id.level_rb);
		final RadioButton score_rb = (RadioButton) findViewById(R.id.score_rb);
		
		switch(screenType){
		case WVGA:
			break;
		case HVGA:
			set_btn.setTextSize(15);
			textview_setting.setTextSize(15);
			textview_tip.setTextSize(15);
			textview_pic.setTextSize(15);
			textview_gametype.setTextSize(15);
			sound_rb.setTextSize(15);
			vibrate_rb.setTextSize(15);
			none_rb.setTextSize(15);
			qqface_rb.setTextSize(15);
			taoface_rb.setTextSize(15);
			level_rb.setTextSize(15);
			score_rb.setTextSize(15);
			break;
		}
		level_rb.setText("闯关模式" + "(最高状态：" + levelStr[bestLevel] + ")");
		score_rb.setText("冲分模式" + "(最高成绩：" + bestScore + ")");
		switch (tipType) {
		case SOUND:
			sound_rb.setChecked(true);
			break;
		case VIBRATE:
			vibrate_rb.setChecked(true);
			break;
		case NONE:
			none_rb.setChecked(true);
			break;
		}
		switch (picType) {
		case QQFACE:
			qqface_rb.setChecked(true);
			break;
		case TAOFACE:
			taoface_rb.setChecked(true);
			break;
		}
		switch (gameType) {
		case LEVEL:
			level_rb.setChecked(true);
			break;
		case SCORE:
			score_rb.setChecked(true);
			break;
		}
		set_btn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (sound_rb.isChecked()) {
					tipType = SOUND;
				} else if (vibrate_rb.isChecked()) {
					tipType = VIBRATE;
				} else if (none_rb.isChecked()) {
					tipType = NONE;
				}
				if (qqface_rb.isChecked()) {
					picType = QQFACE;
				} else if (taoface_rb.isChecked()) {
					picType = TAOFACE;
				}
				if (level_rb.isChecked()) {
					gameType = LEVEL;
				} else if (score_rb.isChecked()) {
					gameType = SCORE;
				}
				showMenuView();
				Log.v("MainActivity", "id:" + tipType);
			}

		});
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		//Log.v("onkeyDown in MainActivity", "onKeyDown: " + event.getKeyCode());
		 if (keyCode == KeyEvent.KEYCODE_BACK){
		 //Log.v("onkeyDown in MainActivity", "back button is prerssed");
			 //if(currentView == SETTINGVIEW){
			 if((currentView == LEVELGAMEVIEW) || (currentView == SCOREGAMEVIEW) 
			 || (currentView == SETTINGVIEW) || (currentView == GOLDPOINTSVIEW)) {
				 showMenuView();
				 return true;
			 }
		 }
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		int tempIndex = ((gameType << 8) | (picType << 4) | tipType);
		editor.putInt("TipAndPicAndGameType", tempIndex);
		tempIndex = ((bestLevel << 16) | bestScore);
		editor.putInt("LevelAndScore", tempIndex);
		editor.commit();
		//Log.v("MainActivity", "onDeatroy");
		super.onDestroy();
	}


}