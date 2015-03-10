package llk.wxf;

import java.util.Random;
import android.util.Log;

public class GameLogic {
	/**
	 * map为图标的分布图，其中
	 * 0：空白
	 * 0-w*h，不包括0和w*h
	 * -1：边框的外延
	 */
	int[][] map;
	int[][] pairMap;
	int[] adrawLinep;
	int w, h;
	Random random = new Random();
	int index;
	
	public static final int NONE = 0, TYPELINE = 1, TYPEZHE1 = 2, TYPEZHE2 = 3;
	public static int lintType;

	public int[][] getmap() {
		return map;
	}
	
	public int[][] getPairMap(){
		return pairMap;
	}	

	public GameLogic(int w, int h, int index) {
		this.w = w;
		this.h = h;
		map = new int[h][w];
		pairMap = new int[h][w];
		for(int i = 0; i < h; i++){
			for(int j = 0; j < w; j++){
				pairMap[i][j] = -1;
			}
		}
		//this.index = index;
		test = false;
		//+3原因：两边各夹1个，最后一个标志位
		ppx = new int[w + 3];
		ppy = new int[h + 3];
		ppx1 = new int[w + 3];
		ppx2 = new int[w + 3];
		ppy1 = new int[h + 3];
		ppy2 = new int[h + 3];
		adrawLinep = new int[8];
		this.createMap(index);
	}

	// 联通检测
	public int getConnection(int x1, int y1, int x2, int y2) {
		if (x1 == x2 && y1 == y2)
			return NONE;
		if (map[y1][x1] != map[y2][x2])
			return NONE;
		if (!test)
			cleanA(adrawLinep);
		if (linelink(x1, y1, x2, y2)) {
			if (!test) {
				adrawLinep[0] = x1;// ////line-------------------->
				adrawLinep[1] = y1;// ////line-------------------->
				adrawLinep[2] = x2;// ////line-------------------->
				adrawLinep[3] = y2;// ////line-------------------->
			}
			//Log.v("linelink:", "Complete");
			return TYPELINE;
		} else if (zhe1(x1, y1, x2, y2)) {
			if (!test) {
				adrawLinep[0] = x1;// ////line-------------------->
				adrawLinep[1] = y1;// ////line-------------------->
				adrawLinep[4] = x2;// ////line-------------------->
				adrawLinep[5] = y2;// ////line-------------------->
			}
			//Log.v("zhe1:", "Complete");
			return TYPEZHE1;
		} else if (zhe2(x1, y1, x2, y2)) {
			if (!test) {
				adrawLinep[0] = x1;// ////line-------------------->
				adrawLinep[1] = y1;// ////line-------------------->
				adrawLinep[6] = x2;// ////line-------------------->
				adrawLinep[7] = y2;// ////line-------------------->
			}
			//Log.v("zhe2:", "Complete");
			return TYPEZHE2;
		}
		return NONE;
	}

	// 直线联通检测
	int ll_tmp1, ll_tmp2;
	public boolean linelink(int x1, int y1, int x2, int y2) {
		if (x1 == x2 || y1 == y2) {
			if((x1 == -1) || (y1 == -1) || (x1 == w) || (y1 == h)){
				return true;
			}
			if (Math.abs(x1 - x2) + Math.abs(y1 - y2) <= 1) {
				return true;
			} else {
				ll_tmp1 = x1 == x2 ? x1 : x1 + ((x2 - x1) / Math.abs(x2 - x1));
				ll_tmp2 = y1 == y2 ? y1 : y1 + ((y2 - y1) / Math.abs(y2 - y1));
				return map[ll_tmp2][ll_tmp1] == 0 ? linelink(ll_tmp1, ll_tmp2, x2, y2)
						: false;
			}
		} else {
			return false;
		}
	}

	//	*********
	//			*
	//			*
	public boolean zhe1(int x1, int y1, int x2, int y2) {
		if (x1 != x2 && y1 != y2) {
			if (map[y1][x2] == 0 && this.linelink(x2, y1, x1, y1)
					&& this.linelink(x2, y1, x2, y2)) {
				if (!test) {
					adrawLinep[2] = x2;// ////line-------------------->
					adrawLinep[3] = y1;// ////line-------------------->
				}
				return true;
			}
			if (map[y2][x1] == 0 && this.linelink(x1, y2, x1, y1)
					&& this.linelink(x1, y2, x2, y2)) {
				if (!test) {
					adrawLinep[2] = x1;// ////line-------------------->
					adrawLinep[3] = y2;// ////line-------------------->
				}
				return true;
			}
		}
		return false;
	}

	// 拾取点四周的空点
	int[] ppx, ppy, ppx1, ppy1, ppx2, ppy2;
	int z2i, z2j, z2Cnt;

	void pickpoint(int x, int y) {
		cleanA(ppx);
		cleanA(ppy);
		// X向左
		z2i = 1;
		z2Cnt = 0;
		while (x - z2i >= -1) {//0
			if((x - z2i) == -1){
				ppx[z2Cnt] = x - z2i;
				z2Cnt++;
				z2i++;
			}
			else if (map[y][x - z2i] == 0) {
				ppx[z2Cnt] = x - z2i;
				z2Cnt++;
				z2i++;
			} else {
				z2i = w + 1;//注意
			}
		}
		// X向右
		z2i = 1;
		while (x + z2i <= w) {
			if((x + z2i) == w){
				ppx[z2Cnt] = x + z2i;
				z2Cnt++;
				z2i++;				
			}
			else if(map[y][x + z2i] == 0){
				ppx[z2Cnt] = x + z2i;
				z2Cnt++;
				z2i++;
			}else{
				z2i = w + 1;//注意
			}
		}
		ppx[z2Cnt] = -2;
		// Y向上
		z2i = 1;
		z2Cnt = 0;
		while (y - z2i >= -1) {//0
			if((y - z2i) == -1){
				ppy[z2Cnt] = y - z2i;
				z2Cnt++;
				z2i++;
			}else if(map[y - z2i][x] == 0) {
				ppy[z2Cnt] = y - z2i;
				z2Cnt++;
				z2i++;
			} else {
				z2i = h + 1;
			}
		}
		// y向下
		z2i = 1;
		while (y + z2i <= h)
			if((y + z2i) == h){
				ppy[z2Cnt] = y + z2i;
				z2Cnt++;
				z2i++;
			}else if(map[y + z2i][x] == 0) {// X
				ppy[z2Cnt] = y + z2i;
				z2Cnt++;
				z2i++;
			} else {
				z2i = h + 1;
			}
		ppy[z2Cnt] = -2;
	}

	public boolean zhe2(int x1, int y1, int x2, int y2) {
		cleanA(ppx1);
		cleanA(ppx2);
		cleanA(ppy1);
		cleanA(ppy2);
		pickpoint(x1, y1);
		ppx1 = ppx.clone();
		ppy1 = ppy.clone();
		pickpoint(x2, y2);
		ppx2 = ppx.clone();
		ppy2 = ppy.clone();
		for (z2i = 0; z2i < w + 3; z2i++) {
			if (ppx1[z2i] == -2)
				z2i = w + 3;
			else {
				for (z2j = 0; z2j < w + 3; z2j++) {
					if (ppx2[z2j] == -2)
						z2j = w + 3;
					else {
						if (ppx1[z2i] == ppx2[z2j]) {
							if (linelink(ppx1[z2i], y1, ppx2[z2j], y2)) {
								if (!test) {
									adrawLinep[2] = ppx1[z2i];// ////line-------------------->
									adrawLinep[3] = y1;// ////line-------------------->
									adrawLinep[4] = ppx2[z2j];// ////line-------------------->
									adrawLinep[5] = y2;// ////line-------------------->
								}
								return true;
							}
						}
					}
				}
			}
		}
		for (z2i = 0; z2i < h + 3; z2i++) {
			if (ppy1[z2i] == -2)
				z2i = h + 3;
			else {
				for (z2j = 0; z2j < h + 3; z2j++) {
					if (ppy2[z2j] == -2)
						z2j = h + 3;
					else {
						if (ppy1[z2i] == ppy2[z2j]) {
							if (linelink(x1, ppy1[z2i], x2, ppy2[z2j])) {
								if (!test) {
									adrawLinep[2] = x1;// ////line-------------------->
									adrawLinep[3] = ppy1[z2i];// ////line-------------------->
									adrawLinep[4] = x2;// ////line-------------------->
									adrawLinep[5] = ppy2[z2j];// ////line-------------------->
								}
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private int ca_i;
	// 清除数组
	public void cleanA(int[] a) {
		for (ca_i = 0; ca_i < a.length; ca_i++) {
			a[ca_i] = -5;
		}
	}

	// 创建地图
	private int cm_i, cm_j, cm_m, cm_n, cm_cnt;
	public void createMap(int index) {
		this.index = index;
		cm_cnt = 0;
		for (cm_i = 0; cm_i < map.length; cm_i++) {
			for (cm_j = 0; cm_j < map[0].length; cm_j++) {
				map[cm_i][cm_j] = 1 + (cm_cnt >> 1) % index;
				// Log.v("creatMap", i + "*" + j + "-" + ii + map[i][j]);
				cm_cnt++;
			}
		}
		chaosA();
	}
	// 打乱地图
	int chaosA_tmp;
	public void chaosA() {
		for (cm_cnt = 0; cm_cnt < index * index; cm_cnt++) {
			cm_i = random.nextInt(h);
			cm_j = random.nextInt(w);
			cm_m = random.nextInt(h);
			cm_n = random.nextInt(w);

			chaosA_tmp = map[cm_i][cm_j];
			map[cm_i][cm_j] = map[cm_m][cm_n];
			map[cm_m][cm_n] = chaosA_tmp;
		}
		Log.v("chaosA:", "init complete");
		// 检测有通
		 if (testConnection() == 0) {
		 chaosA();
		 //Log.v("chaosA:", "next init complete");
		 }
	}

	// 检测是否有联通
	//int k, l, o, p;
	private boolean test;
	int tci, tcj, tcm, tcn, tcCnt;
	public int testConnection() {
		//Log.v("gamelogic testConnection:", "Come in");
		test = true;
		tcCnt = 0;		
		for(tci = 0; tci < h; tci++){
			for(tcj = 0; tcj < w; tcj++){
				pairMap[tci][tcj] = -1;
			}
		}
		for(tci = 0; tci < h; tci++){
			for(tcj = 0; tcj < w; tcj++){
				if((map[tci][tcj] != 0) && (pairMap[tci][tcj] == -1)){
					//continue;
					for(tcm = tci; tcm < h; tcm++){
						for(tcn = 0; tcn < w; tcn++){//notice
							if((map[tcm][tcn] == map[tci][tcj]) && (pairMap[tcm][tcn] == -1)){
								if(getConnection(tcj, tci, tcn, tcm) > 0){
									//Log.v("gamelogic testConnection:", tci + "*" + tcj 
											//+ "-" + tcm + "*" + tcn);
									pairMap[tci][tcj] = pairMap[tcm][tcn] = tcCnt;
									tcCnt++;
									tcn = w;
									tcm = h;
								}
							}
						}
					}
				}
				
			}
		}
		test = false;
		//Log.v("gamelogic testConnection:", tcCnt +"");
		return tcCnt;
	}
}
