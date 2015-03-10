package llk.wxf;

import java.util.Random;

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

public class WVGALevelGameView extends SurfaceView implements Callback, Runnable {
    private SurfaceHolder sfh;
    private Paint paint, paintalpha;
    private Thread th;
    private boolean flag;
    private Canvas canvas;
    public int screenW, screenH;

    // 游戏状态
    public final int PLAYING = 1, PAUSE = 2, NEXTLEVEL = 3, GAMEWIN = 4, GAMELOSS = 5;
    public int gameState, gameLevel, gameScore, missCnt;

    // private int faceIndex;

    private int blockWidth, caseWidth, halfCaseWidth;
    final int W = 7;
    final int H = 10;
    public int blockCount = 24;
    final int allblockCount = 29;

    private Bitmap[] block = new Bitmap[allblockCount];
    private Bitmap find_bmp, play_bmp, pause_bmp, refresh_bmp;
    private Bitmap lose_bg, nextlevel_bg, gamewin_bg, ok, ok_pressed, back, back_pressed;

    Random random = new Random();

    // 游戏矩阵坐标
    private int beginDrawX, beginDrawY, currentX, currentY, selectedX, selectedY, dstDrawY;
    // 第一栏坐标：等级，时间，分数
    private int levelX, levelY, levelL, timeX, timeY, timebarL, scoreX, scoreY, scoreL;
    // 第二栏坐标，button：find、pause、refresh
    private int baseline1, findX, findY, findW, findH, playX, playY, playW, playH, refreshX, refreshY, refreshW,
            refreshH, baseline2;
    // gameover dialog对话框
    private int losebgX, losebgY, losebgW, losebgH, okX, okY, okW, okH, backX, backY, backW;// ,
                                                                                            // backH

    private final int NONE_BTN = -1, FIND_BTN = 0, PLAY_BTN = 1, REFRESH_BTN = 2, OK_BTN = 3, BACK_BTN = 4;
    private int btn_pressed;
    private int findNum, refreshNum;

    private int btnPos[][];

    int[][] map;
    int[][] pairMap;
    int[][] tipPos = new int[2][2];
    int i, j, edgelineX, edgelineY;

    // 多加入一个为了防止数组越界
    public String[] levelStr = { "1.牛刀小试", "2.初显身手", "3.与众不同", "4.刮目相看", "5.出神入化", "6.登峰造极" };
    private float[] levelTime = { 45, 35, 55, 50, 45, 40 };// 40
    private int[] levelGameTime = { 2, 2, 3, 3, 3, 3 };// 3
    private int gameCnt;
    private float timeElapse;
    private GameLogic logic;

    private int bounsCnt;

    // 使用声音和震动资源的地方：onTouchEvent中的touchdown和touchmove中两处。

    private MainActivity context;

    public WVGALevelGameView(Context context) {
        super(context);
        this.context = (MainActivity) context;
        sfh = this.getHolder();
        sfh.addCallback(this);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(20);
        paint.setAlpha(255);
        paintalpha = new Paint();
        paintalpha.setAlpha(125);

        resetParam();
        gameLevel = 0;
        logic = new GameLogic(W, H, blockCount);
        map = logic.getmap();

        loadFP();
    }

    private void resetBestLevel() {
        if (MainActivity.bestLevel < gameLevel) {
            MainActivity.bestLevel = gameLevel;
        }
    }

    // gameLevel被排除在外，在游戏失败或成功时使用，恢复参数。
    private void resetParam() {
        gameScore = 0;
        timeCnt = 0;
        missCnt = 0;
        findNum = 3;// 10;
        refreshNum = 3;// 10;
        bounsCnt = 1;
        tipPos[0][0] = -1;
        tipPos[0][1] = -1;
        tipPos[1][0] = -1;
        tipPos[1][1] = -1;
        currentX = currentY = -1;
        btn_pressed = -1;
        timeElapse = 0;
        gameState = PLAYING;
        gameCnt = 0;
    }

    private final int MUCHTIME = 1, LITTLETIME = 2, ALMOSTNOTIME = 3;
    private int timeState;

    // 游戏逻辑，主要是控制时间。
    public void logic() {
        timeCnt++;
        // 只有在PLAYING的状态下，才会增加计时。
        if (((timeCnt % 8) == 0) && (gameState == PLAYING)) {
            timeElapse++;
            // 小于8s后的震动提示，如果当前模式为震动模式，则没有震动提示。
            if ((timeState == ALMOSTNOTIME) && (MainActivity.tipType != MainActivity.VIBRATE)) {
                context.vibrator.vibrate(40);
            }
        }
        // 根据剩余时间，判定剩余时间状态。
        if ((levelTime[gameLevel] - timeElapse) < 8) {
            timeState = ALMOSTNOTIME;
        } else if ((levelTime[gameLevel] - timeElapse) < 15) {
            timeState = LITTLETIME;
        } else {
            timeState = MUCHTIME;
        }
        // 如果超过了最大时间，游戏结束，并给出提示。
        if ((timeElapse >= levelTime[gameLevel]) && (gameState == PLAYING)) {
            gameState = GAMELOSS;
            switch (MainActivity.tipType) {
            case MainActivity.SOUND:
                context.sp.play(context.gameoverSound, 1f, 1f, 1, 0, 1);
                break;
            case MainActivity.VIBRATE:// new long[]{100,100,100,500},-1
                context.vibrator.vibrate(100);
                break;
            }
        }
        if (beginDrawY > dstDrawY) {
            beginDrawY -= (caseWidth * 2);
        }
        // Log.v("logic in GameView", beginDrawY + "");
    }

    int type;
    int[] typeScore = { 0, 10, 25, 35 };

    // OVERRIDE触摸动作
    private boolean isConnected(int x1, int y1, int x2, int y2) {
        // 越界则返回
        if ((x1 < 0) || (y1 < 0)) {
            return false;
        }
        // 得到连接的类型
        type = logic.getConnection(x1, y1, x2, y2);

        // 如果有连线，需要有很多动作：消除两个图标、成绩增加、时间增加、
        // 如果分数上涨加奖励、如果游戏结束等级加一并显示对话框
        if ((type > 0) && (gameState == PLAYING)) {
            map[y1][x1] = map[y2][x2] = 0;
            gameScore += typeScore[type];
            if (gameScore > bounsCnt * 500) {
                findNum++;
                refreshNum++;
                bounsCnt++;
            }
            timeElapse -= 2;
            tipPos[0][0] = -1;
            tipPos[0][1] = -1;
            tipPos[1][0] = -1;
            tipPos[1][1] = -1;

            // 记录去掉的对数
            missCnt++;
            if (missCnt == ((W * H) >> 1)) {// 通过当前关卡。
                // Log.v("isCOnnection", "come in");
                missCnt = 0;
                gameCnt++;
                if (gameCnt < levelGameTime[gameLevel]) {
                    beginDrawY = dstDrawY + 14 * caseWidth;
                    logic.createMap(blockCount + gameLevel);
                    map = logic.getmap();
                    // Log.v("isCOnnection gameCnt", "come in");
                } else {
                    // 如果当前等级为最高等级，获得胜利
                    if (gameLevel >= (levelStr.length - 1)) {
                        gameState = GAMEWIN;
                    } else {
                        gameState = NEXTLEVEL;
                    }
                    gameCnt = 0;
                }
            } else {
                // 如果没有可以连接的，打乱。
                if ((logic.testConnection() == 0)) {
                    logic.chaosA();
                }
            }
            return true;
        }
        return false;
    }

    private int fi, fj, fcnt, pairCnt, rdmIdx;

    // 查找并显示相同的块
    private void find() {
        pairCnt = logic.testConnection();
        if (pairCnt == 0) {
            logic.chaosA();
        } else {
            rdmIdx = random.nextInt(pairCnt);
            pairMap = logic.getPairMap();
            fcnt = 0;
            for (fi = 0; fi < H; fi++) {
                for (fj = 0; fj < W; fj++) {
                    if (pairMap[fi][fj] == rdmIdx) {
                        tipPos[fcnt][0] = fi;
                        tipPos[fcnt][1] = fj;
                        fcnt++;
                    }
                }
            }
        }
    }

    int tei;
    int mx, my;
    float eventX, eventY;

    // 处理gameState == Playing时的触摸事件。
    private void playingTouchEvent(MotionEvent event) {
        eventX = event.getX();
        eventY = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:// button按下事件只能在DOWN中捕获
            btn_pressed = NONE_BTN;
            if ((eventY > findY) && (eventY < (findY + findH))) {
                for (tei = 0; tei < 3; tei++) {
                    if ((eventX > btnPos[tei][0]) && (eventX < (btnPos[tei][0] + findW))) {
                        btn_pressed = tei;
                    }
                }
            }
        case MotionEvent.ACTION_MOVE:// 图标的按键捕获可以在DOWN和MOVE中捕获
            if (eventX > beginDrawX && eventX < beginDrawX + caseWidth * W && eventY > beginDrawY
                    && eventY < beginDrawY + caseWidth * H) {

                mx = (int) ((event.getX() - beginDrawX) / caseWidth);
                my = (int) ((event.getY() - beginDrawY) / caseWidth);

                if ((currentX == mx) && (currentY == my)) {// 防止重复
                    return;
                }
                // Log.v("onTouchEvent:", mx + "*" + my);
                // 如果但前非空白
                if (map[my][mx] != 0) {
                    selectedX = mx;
                    selectedY = my;

                    // 播放声音的话，两次按下都会响。
                    if (MainActivity.tipType == MainActivity.SOUND) {
                        context.sp.play(context.touchSound, 1f, 1f, 1, 0, 1);
                    }
                    // 如果是可以联通的。
                    if (isConnected(currentX, currentY, selectedX, selectedY)) {
                        currentX = currentY = -1;
                        switch (MainActivity.tipType) {
                        case MainActivity.SOUND:
                            context.sp.play(context.matchSound, 1f, 1f, 1, 0, 1);
                            break;
                        case MainActivity.VIBRATE:// new
                                                  // long[]{100,100,100,500},-1
                            context.vibrator.vibrate(40);
                            break;
                        }
                    } else {
                        if (MainActivity.tipType == MainActivity.VIBRATE) {
                            context.vibrator.vibrate(20);
                        }
                        currentX = selectedX;
                        currentY = selectedY;
                    }
                }
            }
            break;
        case MotionEvent.ACTION_UP:// UP处理的时间：button的抬起,pairPos归零
            if ((eventY > findY) && (eventY < (findY + findH))) {
                for (tei = 0; tei < 3; tei++) {
                    if ((eventX > btnPos[tei][0]) && (eventX < (btnPos[tei][0] + findW))) {
                        if (btn_pressed == tei) {
                            switch (btn_pressed) {
                            case FIND_BTN:
                                if (findNum > 0) {
                                    find();
                                    findNum--;
                                    // 出现提示后，原当前位置复位。
                                    currentX = currentY = -1;
                                }
                                break;
                            case PLAY_BTN:
                                currentX = currentY = -1;
                                gameState = PAUSE;
                                storeMap = new int[H][W];
                                for (int i = 0; i < H; i++) {
                                    for (j = 0; j < W; j++) {
                                        storeMap[i][j] = map[i][j];
                                        map[i][j] = (map[i][j] > 0) ? 1 : 0;
                                    }
                                }
                                break;
                            case REFRESH_BTN:
                                if (refreshNum > 0) {
                                    logic.chaosA();
                                    // 提示复位:游戏刚刚开始、按摇乱键打乱之后、进入下一关。
                                    currentX = currentY = -1;
                                    tipPos[0][0] = -1;
                                    tipPos[0][1] = -1;
                                    tipPos[1][0] = -1;
                                    tipPos[1][1] = -1;
                                    refreshNum--;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            btn_pressed = NONE_BTN;
            break;
        }
    }

    private int[][] storeMap;

    // 在PAUSE状态下的触摸事件
    private void pauseTouchEvent(MotionEvent event) {
        eventX = event.getX();
        eventY = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:// button按下事件只能在DOWN中捕获
            btn_pressed = NONE_BTN;
            if ((eventY > findY) && (eventY < (findY + findH))) {
                if ((eventX > playX) && (eventX < (playX + playW))) {
                    btn_pressed = PLAY_BTN;
                }
            }
            break;
        case MotionEvent.ACTION_UP:// UP处理的时间：button的抬起
            if ((eventY > findY) && (eventY < (findY + findH))) {
                if ((eventX > playX) && (eventX < (playX + playW))) {
                    if (btn_pressed == PLAY_BTN) {
                        gameState = PLAYING;
                        for (int i = 0; i < H; i++) {
                            for (j = 0; j < W; j++) {
                                map[i][j] = storeMap[i][j];
                            }
                        }
                    }
                }
            }
            btn_pressed = -1;
            break;
        }
    }

    // 弹出dialog时的触摸事件：NEXTLEVEL、GAMEWIN、GAMELOSS
    private void dialogTouchEvent(MotionEvent event) {
        eventX = event.getX();
        eventY = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:// button按下事件只能在DOWN中捕获
            btn_pressed = NONE_BTN;
            if ((eventY > okY) && (eventY < (okY + okH))) {
                if ((eventX > okX) && (eventX < (okX + okW))) {
                    btn_pressed = OK_BTN;
                } else if ((eventX > backX) && (eventX < (backX + backW))) {
                    btn_pressed = BACK_BTN;
                }
            }
            break;
        case MotionEvent.ACTION_UP:// UP处理的时间：button的抬起,pairPos归零
            if ((eventY > okY) && (eventY < (okY + okH))) {
                if ((eventX > okX) && (eventX < (okX + okW))) {
                    tei = OK_BTN;
                } else if ((eventX > backX) && (eventX < (backX + backW))) {
                    tei = BACK_BTN;
                }

                if (btn_pressed == tei) {
                    resetBestLevel();
                    switch (btn_pressed) {
                    case OK_BTN:
                        switch (gameState) {
                        case GAMELOSS:// 游戏失败，从本关开始。
                            resetParam();
                            logic.createMap(blockCount + gameLevel);
                            map = logic.getmap();
                            break;
                        case NEXTLEVEL:
                            // 开始新的一关
                            gameLevel++;// 等级加一
                            logic.createMap(blockCount + gameLevel);
                            map = logic.getmap();
                            // 提示复位:游戏刚刚开始、按摇乱键打乱之后、进入下一关。
                            currentX = currentY = -1;
                            tipPos[0][0] = -1;
                            tipPos[0][1] = -1;
                            tipPos[1][0] = -1;
                            tipPos[1][1] = -1;
                            timeElapse = 0;
                            findNum = 3;// 10;
                            refreshNum = 3;// 10;
                            gameState = PLAYING;
                            break;
                        case GAMEWIN:
                            // // 开始新的一关
                            resetBestLevel();
                            resetParam();
                            gameLevel = 0;
                            logic.createMap(blockCount + gameLevel);
                            map = logic.getmap();
                            break;
                        }
                        break;
                    case BACK_BTN:
                        context.showMenuView();
                        break;
                    }
                }
            }
            btn_pressed = -1;
            break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (gameState) {
        case PLAYING:
            playingTouchEvent(event);
            break;
        case PAUSE:
            pauseTouchEvent(event);
            break;
        case NEXTLEVEL:
        case GAMELOSS:
        case GAMEWIN:
            dialogTouchEvent(event);
            break;
        }
        return true;
    }

    public void mydraw() {
        canvas = sfh.lockCanvas();
        canvas.drawRect(0, 0, screenW, screenH, paint);
        switch (gameState) {
        case PLAYING:
        case PAUSE:
            paintPlaying(canvas);
            break;
        case NEXTLEVEL:
        case GAMELOSS:
        case GAMEWIN:
            paintPlaying(canvas);
            paintDialog(canvas);
            break;
        }
        sfh.unlockCanvasAndPost(canvas);
    }

    // 绘制对话框
    private void paintDialog(Canvas canvas) {
        switch (gameState) {
        case NEXTLEVEL:
            canvas.drawBitmap(nextlevel_bg, losebgX, losebgY, paint);
            break;
        case GAMELOSS:
            canvas.drawBitmap(lose_bg, losebgX, losebgY, paint);
            break;
        case GAMEWIN:
            canvas.drawBitmap(gamewin_bg, losebgX, losebgY, paint);
            break;
        }
        if (btn_pressed == this.OK_BTN) {
            canvas.drawBitmap(ok_pressed, okX, okY, paint);
        } else {
            canvas.drawBitmap(ok, okX, okY, paint);
        }
        if (btn_pressed == this.BACK_BTN) {
            canvas.drawBitmap(back_pressed, backX, backY, paint);
        } else {
            canvas.drawBitmap(back, backX, backY, paint);
        }
    }

    private int blockIndex;
    boolean isAlpha;
    private int remainTimeRect;
    float timeStep;

    // 绘制游戏界面
    public void paintPlaying(Canvas canvas) {
        // 只有在GAMELOSS的状态下背景为灰色，其它状态下为白色。
        switch (gameState) {
        case GAMELOSS:
            canvas.drawColor(Color.DKGRAY);
            break;
        default:
            canvas.drawColor(Color.WHITE);

        }
        canvas.clipRect(0, 0, screenW, screenH);

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(findNum + "", findX + findW + 2, findY + findH, paint);
        canvas.drawText(refreshNum + "", refreshX + refreshW + 2, refreshY + refreshH, paint);

        switch (timeState) {
        case MUCHTIME:
            paint.setColor(Color.BLUE);
            break;
        case LITTLETIME:
            paint.setColor(Color.GREEN);
            break;
        case ALMOSTNOTIME:
            paint.setColor(Color.RED);
            break;
        }
        // 时间条
        timeStep = 200 / levelTime[gameLevel];
        // Log.v("timeStr", timeStep + "");
        remainTimeRect = (int) (timeElapse * timeStep);
        if (remainTimeRect < 0) {
            remainTimeRect = 0;
        }
        if (remainTimeRect >= 200) {
            remainTimeRect = 200;
        }
        // 时间数字
        canvas.drawText((int) (levelTime[gameLevel] - timeElapse) + "", timeX + timebarL + 2, timeY, paint);
        canvas.drawRect(timeX + 1, timeY - paint.getTextSize() + 4, timeX + timebarL - 1 - remainTimeRect, timeY - 1,
                paint);
        // paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        // 等级
        canvas.drawText(levelStr[gameLevel], levelX, levelY, paint);
        // 分数
        canvas.drawText("分数：" + gameScore, scoreX, scoreY, paint);
        // 边线1
        canvas.drawLine(0, baseline1, screenW, baseline1, paint);
        // 边线2
        canvas.drawLine(0, baseline2, screenW, baseline2, paint);

        // 底部按钮绘制
        isAlpha = (btn_pressed == FIND_BTN) ? true : false;
        paintBlock(canvas, find_bmp, findX, findY, findW, findH, isAlpha);
        isAlpha = (btn_pressed == PLAY_BTN) ? true : false;
        if (gameState == PLAYING) {
            paintBlock(canvas, pause_bmp, playX, playY, playW, playH, isAlpha);
        } else if (gameState == PAUSE) {
            paintBlock(canvas, play_bmp, playX, playY, playW, playH, isAlpha);
        }
        isAlpha = (btn_pressed == REFRESH_BTN) ? true : false;
        paintBlock(canvas, refresh_bmp, refreshX, refreshY, refreshW, refreshH, isAlpha);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        // 时间边框
        canvas.drawRect(timeX, timeY - paint.getTextSize() + 3, timeX + timebarL, timeY, paint);

        for (i = 0; i < H; i++) {
            for (j = 0; j < W; j++) {
                if ((map[i][j] > 0) && (map[i][j] <= block.length)) {
                    blockIndex = map[i][j] - 1;
                    // paintBlock(canvas, blockIndex, j, i);j-x i- y
                    if (((j == currentX) && (i == currentY)) || (i == tipPos[0][0] && j == tipPos[0][1])
                            || (i == tipPos[1][0] && j == tipPos[1][1])) {
                        paintBlock(canvas, block[blockIndex], beginDrawX + j * caseWidth + 2, beginDrawY + i
                                * caseWidth + 2, blockWidth, blockWidth, true);
                    } else {
                        paintBlock(canvas, block[blockIndex], beginDrawX + j * caseWidth + 2, beginDrawY + i
                                * caseWidth + 2, blockWidth, blockWidth, false);
                    }
                }
            }
        }

        // 画连接线段
        edgelineX = halfCaseWidth;
        edgelineY = halfCaseWidth;
        if (logic.adrawLinep[6] != -5) {// 画线边缘判断，只有可能出现在中间2点。而且是二折的情况
            if ((logic.adrawLinep[2] == -1) || (logic.adrawLinep[4] == -1)) {
                // edgelineX = (blockWidth >> 2) * 3;
                edgelineX = blockWidth - 5;
            } else if ((logic.adrawLinep[2] == W) || (logic.adrawLinep[4] == W)) {
                // edgelineX = blockWidth >> 2;
                edgelineX = 5;
            }
            if ((logic.adrawLinep[3] == -1) || (logic.adrawLinep[5] == -1)) {
                // edgelineY = (blockWidth >> 2) * 3;
                edgelineY = blockWidth - 5;
            } else if ((logic.adrawLinep[3] == H) || (logic.adrawLinep[5] == H)) {
                // edgelineY = blockWidth >> 2;
                edgelineY = 5;
            }
        }
        paint.setColor(Color.RED);// R.drawable.red
        paint.setStyle(Paint.Style.FILL);

        if (logic.adrawLinep[2] != -5)// 如果线段数组前两个数不等于0
            canvas.drawLine(beginDrawX + logic.adrawLinep[0] * caseWidth + halfCaseWidth, beginDrawY
                    + logic.adrawLinep[1] * caseWidth + halfCaseWidth, beginDrawX + logic.adrawLinep[2] * caseWidth
                    + edgelineX, beginDrawY + logic.adrawLinep[3] * caseWidth + edgelineY, paint);
        if (logic.adrawLinep[4] != -5)// 如果线段数组前三个数不等于0
            canvas.drawLine(beginDrawX + logic.adrawLinep[2] * caseWidth + edgelineX, beginDrawY + logic.adrawLinep[3]
                    * caseWidth + edgelineY, beginDrawX + logic.adrawLinep[4] * caseWidth + edgelineX, beginDrawY
                    + logic.adrawLinep[5] * caseWidth + edgelineY, paint);
        if (logic.adrawLinep[6] != -5)// 如果线段数组前四个数不等于0
            canvas.drawLine(beginDrawX + logic.adrawLinep[4] * caseWidth + edgelineX, beginDrawY + logic.adrawLinep[5]
                    * caseWidth + edgelineY, beginDrawX + logic.adrawLinep[6] * caseWidth + halfCaseWidth, beginDrawY
                    + logic.adrawLinep[7] * caseWidth + halfCaseWidth, paint);
        logic.cleanA(logic.adrawLinep);
    }

    private void paintBlock(Canvas canvas, Bitmap bmp, int x, int y, int w, int h, boolean alpha) {
        if (alpha) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(x - 1, y - 1, x + w + 1, y + h + 1, paint);
            canvas.drawBitmap(bmp, x, y, paintalpha);
        } else {
            canvas.drawBitmap(bmp, x, y, paint);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        // TODO Auto-generated method stub
        Log.v("GameView:", "surfaceCreated");
        screenW = this.getWidth();
        screenH = this.getHeight();

        this.requestFocus();
        int i;

        levelX = 0;
        levelY = (int) (paint.getTextSize() * 3 / 2);
        float[] elementsLen = new float[levelStr[0].length()];
        paint.getTextWidths(levelStr[0], elementsLen);
        levelL = 0;// (int) (levelStr[0].length() * paint.getTextSize());
        for (i = 0; i < levelStr[0].length(); i++) {
            levelL += elementsLen[i];
        }

        float cnt1[] = { (float) 0.0 };
        paint.getTextWidths("0", cnt1);
        timeX = levelX + levelL + 20;
        timeY = (int) (paint.getTextSize() * 3 / 2);
        timebarL = 202;

        float cnt2[] = new float[8];
        String scoreStr = "分数：00000";
        paint.getTextWidths(scoreStr, cnt2);
        scoreL = 0;
        for (int sci = 0; sci < cnt2.length; sci++) {
            scoreL += cnt2[sci];
        }
        scoreX = screenW - scoreL;
        scoreY = (int) (paint.getTextSize() * 3 / 2);

        baseline1 = (int) (paint.getTextSize() * 2);

        beginDrawX = (screenW - W * caseWidth) >> 1;
        beginDrawY = (int) (baseline1 + paint.getTextSize() / 2);
        dstDrawY = beginDrawY;

        baseline2 = (int) (baseline1 + H * caseWidth + paint.getTextSize());
        findW = find_bmp.getWidth();
        findH = find_bmp.getHeight();
        findX = 0;
        findY = (int) (baseline2 + paint.getTextSize() / 2);
        playW = play_bmp.getWidth();
        playH = play_bmp.getHeight();
        playX = (screenW - playW) >> 1;
        playY = findY;
        refreshW = refresh_bmp.getWidth();
        refreshH = refresh_bmp.getHeight();
        refreshX = (int) (screenW - refreshW - paint.getTextSize() * 2);
        refreshY = findY;
        btnPos = new int[3][2];
        btnPos[0][0] = findX;
        btnPos[0][1] = findY;
        btnPos[1][0] = playX;
        btnPos[1][1] = playY;
        btnPos[2][0] = refreshX;
        btnPos[2][1] = refreshY;

        losebgW = lose_bg.getWidth();
        losebgH = lose_bg.getHeight();
        losebgX = (screenW - losebgW) >> 1;
        losebgY = (screenH - losebgH) >> 1;
        okW = ok.getWidth();
        int step = (losebgW - 2 * okW) >> 2;
        okH = ok.getHeight();
        okX = losebgX + step;
        okY = losebgY + (losebgH >> 1);
        backW = back.getWidth();
        // backH = back.getHeight();
        backX = losebgX + losebgW - step - backW;
        backY = okY;
        // Log.v("btn pos in gameview", losebgY + "-" + losebgH + "-" + okY);
        // 提示复位:游戏刚刚开始、按摇乱键打乱之后、进入下一关。
        currentX = currentY = -1;
        tipPos[0][0] = -1;
        tipPos[0][1] = -1;
        tipPos[1][0] = -1;
        tipPos[1][1] = -1;

        btn_pressed = -1;

        flag = true;
        th = new Thread(this);
        th.start();
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        // TODO Auto-generated method stub
        // Log.v("GameView:", "surfaceDestroyed");
        flag = false;
    }

    private long start, during, timeCnt;

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while (flag) {
            start = System.currentTimeMillis();
            logic();
            mydraw();

            during = System.currentTimeMillis() - start;
            if (during < 125) {
                try {
                    Thread.sleep(125 - during);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 按键抬起事件监听
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Log.v("onKeyDown of onKeyUp:", "out");
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Log.v("onKeyUp of GameView:", "in");
            resetBestLevel();
            context.showMenuView();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 加载外部图片-->LLKView
    public void loadFP() {
        Resources res = getResources();

        play_bmp = BitmapFactory.decodeResource(res, R.drawable.play);
        pause_bmp = BitmapFactory.decodeResource(res, R.drawable.pause);
        find_bmp = BitmapFactory.decodeResource(res, R.drawable.find);
        refresh_bmp = BitmapFactory.decodeResource(res, R.drawable.refresh);

        lose_bg = BitmapFactory.decodeResource(res, R.drawable.lose_bg);
        nextlevel_bg = BitmapFactory.decodeResource(res, R.drawable.nextlevel_bg);
        gamewin_bg = BitmapFactory.decodeResource(res, R.drawable.gamewin_bg);
        ok = BitmapFactory.decodeResource(res, R.drawable.ok);
        ok_pressed = BitmapFactory.decodeResource(res, R.drawable.ok_pressed);
        back = BitmapFactory.decodeResource(res, R.drawable.back);
        back_pressed = BitmapFactory.decodeResource(res, R.drawable.back_pressed);

        // index = QQFACE;
        switch (MainActivity.picType) {
        case MainActivity.QQFACE:
            int[] randomIndex = new int[54];
            randomIndex[0] = R.drawable.qq_1;
            randomIndex[1] = R.drawable.qq_2;
            randomIndex[2] = R.drawable.qq_3;
            randomIndex[3] = R.drawable.qq_4;
            randomIndex[4] = R.drawable.qq_5;
            randomIndex[5] = R.drawable.qq_6;
            randomIndex[6] = R.drawable.qq_7;
            randomIndex[7] = R.drawable.qq_8;
            randomIndex[8] = R.drawable.qq_9;
            randomIndex[9] = R.drawable.qq_10;
            randomIndex[10] = R.drawable.qq_11;
            randomIndex[11] = R.drawable.qq_12;
            randomIndex[12] = R.drawable.qq_13;
            randomIndex[13] = R.drawable.qq_14;
            randomIndex[14] = R.drawable.qq_15;
            randomIndex[15] = R.drawable.qq_16;
            randomIndex[16] = R.drawable.qq_17;
            randomIndex[17] = R.drawable.qq_18;
            randomIndex[18] = R.drawable.qq_19;
            randomIndex[19] = R.drawable.qq_20;
            randomIndex[20] = R.drawable.qq_21;
            randomIndex[21] = R.drawable.qq_22;
            randomIndex[22] = R.drawable.qq_23;
            randomIndex[23] = R.drawable.qq_24;
            randomIndex[24] = R.drawable.qq_25;
            randomIndex[25] = R.drawable.qq_26;
            randomIndex[26] = R.drawable.qq_27;
            randomIndex[27] = R.drawable.qq_28;
            randomIndex[28] = R.drawable.qq_29;
            randomIndex[29] = R.drawable.qq_30;
            randomIndex[30] = R.drawable.qq_31;
            randomIndex[31] = R.drawable.qq_32;
            randomIndex[32] = R.drawable.qq_33;
            randomIndex[33] = R.drawable.qq_34;
            randomIndex[34] = R.drawable.qq_35;
            randomIndex[35] = R.drawable.qq_36;
            randomIndex[36] = R.drawable.qq_37;
            randomIndex[37] = R.drawable.qq_38;
            randomIndex[38] = R.drawable.qq_39;
            randomIndex[39] = R.drawable.qq_40;
            randomIndex[40] = R.drawable.qq_41;
            randomIndex[41] = R.drawable.qq_42;
            randomIndex[42] = R.drawable.qq_43;
            randomIndex[43] = R.drawable.qq_44;
            randomIndex[44] = R.drawable.qq_45;
            randomIndex[45] = R.drawable.qq_46;
            randomIndex[46] = R.drawable.qq_47;
            randomIndex[47] = R.drawable.qq_48;
            randomIndex[48] = R.drawable.qq_49;
            randomIndex[49] = R.drawable.qq_50;
            randomIndex[50] = R.drawable.qq_51;
            randomIndex[51] = R.drawable.qq_52;
            randomIndex[52] = R.drawable.qq_53;
            randomIndex[53] = R.drawable.qq_54;
            int i,
            x,
            y,
            tmp;
            for (i = 1; i < 100; i++) {
                // 1 - 53
                x = random.nextInt(53) + 1;
                y = random.nextInt(53) + 1;
                tmp = randomIndex[x];
                randomIndex[x] = randomIndex[y];
                randomIndex[y] = tmp;
            }
            for (i = 0; i < 29; i++) {
                block[i] = BitmapFactory.decodeResource(res, randomIndex[i]);
            }
            // block[0] = BitmapFactory.decodeResource(res, R.drawable.qq_1);
            // block[1] = BitmapFactory.decodeResource(res, R.drawable.qq_2);
            // block[2] = BitmapFactory.decodeResource(res, R.drawable.qq_3);
            // block[3] = BitmapFactory.decodeResource(res, R.drawable.qq_4);
            // block[4] = BitmapFactory.decodeResource(res, R.drawable.qq_5);
            // block[5] = BitmapFactory.decodeResource(res, R.drawable.qq_6);
            // block[6] = BitmapFactory.decodeResource(res, R.drawable.qq_7);
            // block[7] = BitmapFactory.decodeResource(res, R.drawable.qq_8);
            // block[8] = BitmapFactory.decodeResource(res, R.drawable.qq_9);
            // block[9] = BitmapFactory.decodeResource(res, R.drawable.qq_10);
            // block[10] = BitmapFactory.decodeResource(res, R.drawable.qq_11);
            // block[11] = BitmapFactory.decodeResource(res, R.drawable.qq_12);
            // block[12] = BitmapFactory.decodeResource(res, R.drawable.qq_13);
            // block[13] = BitmapFactory.decodeResource(res, R.drawable.qq_14);
            // block[14] = BitmapFactory.decodeResource(res, R.drawable.qq_15);
            // block[15] = BitmapFactory.decodeResource(res, R.drawable.qq_16);
            // block[16] = BitmapFactory.decodeResource(res, R.drawable.qq_17);
            // block[17] = BitmapFactory.decodeResource(res, R.drawable.qq_18);
            // block[18] = BitmapFactory.decodeResource(res, R.drawable.qq_19);
            // block[19] = BitmapFactory.decodeResource(res, R.drawable.qq_20);
            // block[20] = BitmapFactory.decodeResource(res, R.drawable.qq_21);
            // block[21] = BitmapFactory.decodeResource(res, R.drawable.qq_22);
            // block[22] = BitmapFactory.decodeResource(res, R.drawable.qq_23);
            // block[23] = BitmapFactory.decodeResource(res, R.drawable.qq_24);
            // block[24] = BitmapFactory.decodeResource(res, R.drawable.qq_25);
            // block[25] = BitmapFactory.decodeResource(res, R.drawable.qq_26);
            // block[26] = BitmapFactory.decodeResource(res, R.drawable.qq_27);
            // block[27] = BitmapFactory.decodeResource(res, R.drawable.qq_28);
            // block[28] = BitmapFactory.decodeResource(res, R.drawable.qq_29);
            break;
        // case BEAUTY:
        // block[0] = BitmapFactory.decodeResource(res, R.drawable.icon_0_1);
        // block[1] = BitmapFactory.decodeResource(res, R.drawable.icon_0_2);
        // block[2] = BitmapFactory.decodeResource(res, R.drawable.icon_0_3);
        // block[3] = BitmapFactory.decodeResource(res, R.drawable.icon_0_4);
        // block[4] = BitmapFactory.decodeResource(res, R.drawable.icon_0_5);
        // block[5] = BitmapFactory.decodeResource(res, R.drawable.icon_0_6);
        // block[6] = BitmapFactory.decodeResource(res, R.drawable.icon_0_7);
        // block[7] = BitmapFactory.decodeResource(res, R.drawable.icon_0_8);
        // block[8] = BitmapFactory.decodeResource(res, R.drawable.icon_0_9);
        // block[9] = BitmapFactory.decodeResource(res, R.drawable.icon_0_10);
        // block[10] = BitmapFactory.decodeResource(res, R.drawable.icon_0_11);
        // block[11] = BitmapFactory.decodeResource(res, R.drawable.icon_0_12);
        // block[12] = BitmapFactory.decodeResource(res, R.drawable.icon_0_13);
        // block[13] = BitmapFactory.decodeResource(res, R.drawable.icon_0_14);
        // block[14] = BitmapFactory.decodeResource(res, R.drawable.icon_0_15);
        // block[15] = BitmapFactory.decodeResource(res, R.drawable.icon_0_16);
        // block[16] = BitmapFactory.decodeResource(res, R.drawable.icon_0_17);
        // block[17] = BitmapFactory.decodeResource(res, R.drawable.icon_0_18);
        // block[18] = BitmapFactory.decodeResource(res, R.drawable.icon_0_19);
        // block[19] = BitmapFactory.decodeResource(res, R.drawable.icon_0_20);
        // block[20] = BitmapFactory.decodeResource(res, R.drawable.icon_0_21);
        // block[21] = BitmapFactory.decodeResource(res, R.drawable.icon_0_22);
        // block[22] = BitmapFactory.decodeResource(res, R.drawable.icon_0_23);
        // block[23] = BitmapFactory.decodeResource(res, R.drawable.icon_0_24);
        // block[24] = BitmapFactory.decodeResource(res, R.drawable.icon_1_1);
        // block[25] = BitmapFactory.decodeResource(res, R.drawable.icon_1_2);
        // block[26] = BitmapFactory.decodeResource(res, R.drawable.icon_1_3);
        // block[27] = BitmapFactory.decodeResource(res, R.drawable.icon_1_4);
        // block[28] = BitmapFactory.decodeResource(res, R.drawable.icon_1_5);
        // break;
        case MainActivity.TAOFACE:
            block[0] = BitmapFactory.decodeResource(res, R.drawable.tb_1);
            block[1] = BitmapFactory.decodeResource(res, R.drawable.tb_2);
            block[2] = BitmapFactory.decodeResource(res, R.drawable.tb_3);
            block[3] = BitmapFactory.decodeResource(res, R.drawable.tb_4);
            block[4] = BitmapFactory.decodeResource(res, R.drawable.tb_5);
            block[5] = BitmapFactory.decodeResource(res, R.drawable.tb_6);
            block[6] = BitmapFactory.decodeResource(res, R.drawable.tb_7);
            block[7] = BitmapFactory.decodeResource(res, R.drawable.tb_8);
            block[8] = BitmapFactory.decodeResource(res, R.drawable.tb_9);
            block[9] = BitmapFactory.decodeResource(res, R.drawable.tb_10);
            block[10] = BitmapFactory.decodeResource(res, R.drawable.tb_11);
            block[11] = BitmapFactory.decodeResource(res, R.drawable.tb_12);
            block[12] = BitmapFactory.decodeResource(res, R.drawable.tb_13);
            block[13] = BitmapFactory.decodeResource(res, R.drawable.tb_14);
            block[14] = BitmapFactory.decodeResource(res, R.drawable.tb_15);
            block[15] = BitmapFactory.decodeResource(res, R.drawable.tb_16);
            block[16] = BitmapFactory.decodeResource(res, R.drawable.tb_17);
            block[17] = BitmapFactory.decodeResource(res, R.drawable.tb_18);
            block[18] = BitmapFactory.decodeResource(res, R.drawable.tb_19);
            block[19] = BitmapFactory.decodeResource(res, R.drawable.tb_20);
            block[20] = BitmapFactory.decodeResource(res, R.drawable.tb_21);
            block[21] = BitmapFactory.decodeResource(res, R.drawable.tb_22);
            block[22] = BitmapFactory.decodeResource(res, R.drawable.tb_23);
            block[23] = BitmapFactory.decodeResource(res, R.drawable.tb_24);
            block[24] = BitmapFactory.decodeResource(res, R.drawable.tb_25);
            block[25] = BitmapFactory.decodeResource(res, R.drawable.tb_26);
            block[26] = BitmapFactory.decodeResource(res, R.drawable.tb_27);
            block[27] = BitmapFactory.decodeResource(res, R.drawable.tb_28);
            block[28] = BitmapFactory.decodeResource(res, R.drawable.tb_29);
            break;
        }
        blockWidth = block[0].getWidth();
        caseWidth = blockWidth + 4;
        halfCaseWidth = caseWidth >> 1;
    }

}
