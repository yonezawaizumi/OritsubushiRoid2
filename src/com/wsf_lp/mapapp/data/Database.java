package com.wsf_lp.mapapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
//import android.os.Debug;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipException;

import com.wsf_lp.android.Prefs;
import com.wsf_lp.android.ZippedAssetStreamAndName;
import com.wsf_lp.mapapp.MapAreaV2;

//すべてのメソッドは専用のワーカスレッドからのみ呼び出せる
//UIスレッドからの呼び出しはDatabaseServiceが制御する

public class Database {
	public static String UPLOAD_FILE_NAME = "upload.tsv";
	private static final String[] EMPTY = new String[]{};
	/**
	 * 地図上に表示する駅の種類
	 * @author yonezawaizumi
	 *
	 */
	public static class VisibilityType {
		public static final int ALL_STATIONS = 0;
		public static final int COMPLETED_STATIONS = 1;
		public static final int INCOMPLETED_STATIONS = 2;
	    public static final int NUM_TYPES = 3;
	}

	public interface OnProgressListener {
		public void onProgress(int percentile);
	}

	public static class MethodName {
		public static final String GET_STATIONS = "getStations";
		public static final String SET_VISIBILITY_TYPE = "setVisibilityType";
		public static final String LOAD_LINES = "getStationLines";
		public static final String RELOAD_STATION = "reloadStation";
		public static final String GET_TOTAL = "getTotalGroup";
		public static final String GET_OPERATOR_TYPES = "getOperatorTypeGroups";
		public static final String RELOAD_OPERATOR_TYPE = "reloadOperatorTypeGroup";
		public static final String GET_OPERATORS_STATISTICS = "getOperatorGroups";
		public static final String GET_OPERATORS = "getOperatorGroupsNoStatistics";
		public static final String GET_PREFS = "getPrefGroups";
		public static final String RELOAD_OPERATOR = "reloadOperatorGroup";
		public static final String RELOAD_PREF = "reloadPrefGroup";
		public static final String GET_LINES = "getLineGroups";
		public static final String RELOAD_LINE = "reloadLineGroup";
		public static final String GET_YOMI = "getYomiGroups";
		public static final String GET_YOMI_2 = "getYomi2Groups";
		public static final String RELOAD_YOMI = "reloadYomiGroup";
		public static final String RELOAD_YOMI_2 = "reloadYomi2Group";
		public static final String GET_YEAR = "getYearGroups";
		public static final String GET_MONTH = "getMonthGroups";
		public static final String GET_DAY = "getDayGroups";
		public static final String RELOAD_YEAR = "reloadYearGroup";
		public static final String RELOAD_MONTH = "reloadMonthGroup";
		public static final String RELOAD_DAY = "reloadDayGroup";
		public static final String GET_STATIONS_BY_LINE = "getStationsByLine";
		public static final String GET_STATIONS_BY_YOMI = "getStationsByYomi";
		public static final String GET_STATIONS_BY_PREF = "getStationsByPref";
		public static final String GET_STATIONS_BY_COMP_DATE = "getStationsByCompDate";
		public static final String UPDATE_COMPLETION = "updateCompletion";
		public static final String UPDATE_MEMO = "updateMemo";
		public static final String PREPARE_SYNC = "prepareSync";
		public static final String UPDATE_SYNC = "updateSync";
	}

	public static class SyncPreparationProperties {
		public String updateDate;
		public String databaseVersion;
		public File file;
	}


	/**
	 * 初期化、アプリアップデート、同期アップデートの際の更新処理
	 * @author yonezawaizumi
	 *
	 */
	private static class Updater {
		/**
		 * 入力ストリームからSQL文を読み出し、データベースを更新する
		 * @param sqlite データベース
		 * @param stream 入力ストリーム
		 * @param appendixSql 入力ストリームのすべてのSQL文を実行した後に追加で実行するSQL文 必要なければnull 入力ストリームにSQL文がない場合は実行されない
		 * @return 1行以上のSQL文を実行しコミットした場合true それ以外の場合false
		 */
		public static boolean update(SQLiteDatabase sqlite, InputStream stream, String appendixSql, OnProgressListener listener, long streamSize) {
			InputStreamReader reader_ = null;
			BufferedReader reader = null;
			long causedSize = 0L;
			int percentile = 0;
			if(listener != null) {
				listener.onProgress(0);
			}
			try {
				reader_ = new InputStreamReader(stream, "UTF-8");
				reader = new BufferedReader(reader_);
				boolean updated = false;
				//SQLiteDatabaseが文字列リテラル内の改行を処理してくれない！！！自力処理かよ…(#･∀･)
				StringBuilder builder = new StringBuilder();
				int quoteCount = 0;
				sqlite.beginTransaction();
				for(;;) {
					String line = reader.readLine();
					if(line == null) {
						break;
					}
					for(char ch : line.toCharArray()) {
						if(ch == '\'') {
							++quoteCount;
						}
					}
					builder.append(line);
					String sql = builder.toString().trim();
					if(sql.length() == 0 || (quoteCount & 1) == 1) {
						continue;
					}
					sqlite.execSQL(sql);
					builder.setLength(0);
					quoteCount = 0;
					updated = true;
					if(listener != null) {
						causedSize += line.getBytes().length;
						final int newPercentile = (int)(causedSize * 100 / streamSize);
						if(percentile < newPercentile) {
							percentile = newPercentile;
							listener.onProgress(percentile);
						}
					}

				}
				if(updated && appendixSql != null) {
					sqlite.execSQL(appendixSql);
				}
				if(updated) {
					sqlite.setTransactionSuccessful();
				}
				return updated;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (SQLiteException e) {
				e.printStackTrace();
				return false;
			} finally {
				sqlite.endTransaction();
				if(reader != null) { try { reader.close(); } catch(IOException e) {} }
				if(reader_ != null) { try { reader_.close(); } catch(IOException e) {} }
				if(stream != null) { try { stream.close(); } catch(IOException e) {} }
			}
		}
	}

	/**
	 * データベース初期化処理
	 * @author yonezawaizumi
	 *
	 */
	private static class Helper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "oritsubushiroid.sqlite";
		private static final String DATABASE_ASSET_NAME = "database.zip";

		private Context context;
		private boolean updated;
		private OnProgressListener listener;

		/**
		 * コンストラクタ
		 * @param context サービスのコンテキスト
		 */
		public Helper(Context context, int version, OnProgressListener listener) {
			super(context, DATABASE_NAME, null, version);
			this.context = context;
			this.listener = listener;
			updated = false;
		}

		/**
		 * 圧縮されたSQLファイルを読み出せるようにする
		 * @return SQLファイルの入力ストリームおよびファイル名
		 * @throws IOException
		 * @throws FileNotFoundException
		 * @throws ZipException
		 */
		private static ZippedAssetStreamAndName getZippedSql(Context context) throws IOException, FileNotFoundException, ZipException {
			return ZippedAssetStreamAndName.open(context.getResources(), DATABASE_ASSET_NAME);
		}

		/**
		 * 圧縮されたSQLファイルによりデータベースを更新・初期化する
		 * @param sqlite データベース
		 */
		private void update(SQLiteDatabase sqlite, OnProgressListener listener) {
			if(updated) {
				return;
			}
			InputStream stream = null;
			try {
				ZippedAssetStreamAndName zip = getZippedSql(context);
				stream = zip.getInputStream();
				updated = Updater.update(sqlite, stream, "INSERT OR IGNORE INTO completions SELECT s_id, 0, 0, '' FROM stations", listener, zip.getSize());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				if(stream != null) { try { stream.close(); } catch(IOException e) {} }
			}
		}

		@Override
		public void onCreate(SQLiteDatabase sqlite) {
			update(sqlite, listener);
		}

		@Override
		public void onUpgrade(SQLiteDatabase sqlite, int oldVersion, int newVersion) {
			update(sqlite, listener);
		}
	}

	private SQLiteDatabase sqlite;
	private SparseArray<Operator> operators = new SparseArray<Operator>();
	private Group totalStatisticsCache = null;
	private ArrayList<Group> operatorTypesStatisticsCache = new ArrayList<Group>();
	private ArrayList<Group> prefsStatisticsCache = new ArrayList<Group>();
	private ArrayList<Group> yomiStatisticsCache = new ArrayList<Group>();
	private SparseArray<ArrayList<Group>> operatorStatisticsCache = new SparseArray<ArrayList<Group>>();
	private int visibilityType = VisibilityType.ALL_STATIONS;
	private int filterType = DatabaseFilter.NONE;
	private String filterValue = "";
	private String filterSqlCondition = "";
	private String tablesForFilterCondition = "stations, completions";
	private String areaSql = "SELECT 1";
	//private int maxStations;
	private String maxStationsString;
	//private OnProgressListener listener;

	//初期化時および同期アップデート時に呼び出す
	private void reload() {
		operators.clear();
		Cursor cursor = sqlite.rawQuery("SELECT o_id, operator, type FROM operators WHERE enabled = 1 ORDER BY type, o_id", new String[0]);
		while(cursor.moveToNext()) {
			Operator operator = new Operator();
			operator.setCode(cursor.getInt(0));
			operator.setName(cursor.getString(1));
			operator.setOperatorType(cursor.getInt(2));
			operators.put(operator.getCode(), operator);
		}
		cursor.close();
		clearOperatorStatisticsCache(null);
	}

	/**
	 * データベースを初期化し、必要に応じて更新する
	 * @param context サービスのコンテキスト
	 */
	public int initialize(Context context, Integer maxStations, int version, OnProgressListener listener) throws SQLiteException {
		//this.listener = listener;
		setMaxStations(maxStations);
		Helper helper = new Helper(context, version, listener);
		sqlite = helper.getWritableDatabase();
		reload();
		updateResources(context.getResources());
		return sqlite.getVersion();
	}

	/**
	 * ダウンロードしたSQLをマージする
	 */
	public int updateSync(byte[] sqlBytes) {
		InputStream stream = null;
		stream = new ByteArrayInputStream(sqlBytes);
		boolean updated = Updater.update(sqlite, stream, null, null, sqlBytes.length);
		if(updated) {
			visibilityType = VisibilityType.ALL_STATIONS;
			reload();
			return sqlite.getVersion();
		} else {
			return 0;
		}
	}

	/**
	 * 地図表示フィルタ種類を設定する
	 * @param resources カレントリソース
	 * @param filterType フィルタ種類を表す、DatabaseFilterクラスで定義された整数定数
	 * @param filterValue フィルタ値
	 */
	public OritsubushiNotificationIntent setFilterType(Resources resources, int filterType, String filterValue) {
		if(this.filterType != filterType || !(filterValue != null ? filterValue : "").equals(this.filterValue)) {
			this.filterType = filterType;
			this.filterValue = filterValue;
			updateResources(resources);
			return new OritsubushiNotificationIntent().setMapStatusChanged();
		} else {
			return null;
		}
	}

	/**
	 * 地図表示駅フィルタを設定する
	 * @param resources カレントリソース
	 * @param visibilityType このクラスで定義された、表示駅フィルタを表す整数定数
	 */
	public OritsubushiNotificationIntent setVisibilityType(Resources resources, int visibilityType) {
		if(this.visibilityType != visibilityType) {
			this.visibilityType = visibilityType;
			updateResources(resources);
			return new OritsubushiNotificationIntent().setMapStatusChanged();
		} else {
			return null;
		}
	}

	/**
	 * リソースを更新する
	 * 具体的には、地図表示フィルタの文字列更新、都道府県の更新を行う
	 * @param resources
	 */
	public void updateResources(Resources resources) {
		String tables = "stations, completions";
		String word = filterValue;
		String condition;
		StringBuilder builder = new StringBuilder();
	    if(word.length() > 0) {
	    	boolean escaped = word.indexOf('*') >= 0 || word.indexOf('_') >= 0;
	    	if(escaped) {
	    		word = word.replace("*", "**").replace("%", "*%").replace("_", "*_");
	    	}
	    	word = word.replace("'", "''");
	    	String format = null;
	    	boolean formatted = false;
	    	switch(filterType) {
	    	case DatabaseFilter.NAME_FORWARD:
	    		format = "stations.station LIKE '%s%%";
	    		break;
	    	case DatabaseFilter.NAME:
	    		format = "stations.station LIKE '%%%s%%";
	    		break;
	    	case DatabaseFilter.LINE_FORWARD:
	    		tables = "stations, completions, stations_lines, \"lines\"";
	            format = "stations_lines.s_id = stations.s_id AND lines.l_id = stations_lines.l_id AND lines.line LIKE '%s%%' AND lines.enabled = 1";
	            break;
	        case DatabaseFilter.LINE:
	            tables = "stations, completions, stations_lines, \"lines\"";
	            format = "stations_lines.s_id = stations.s_id AND lines.l_id = stations_lines.l_id AND lines.line LIKE '%%%s%%' AND lines.enabled = 1";
	            break;
	        case DatabaseFilter.YOMI_FORWARD:
	        	format = "stations.yomi LIKE '%s%%'";
	            break;
	        case DatabaseFilter.YOMI:
	        	format = "stations.yomi LIKE '%%%s%%'";
	            break;
            case DatabaseFilter.PREF:
            {
            	for(Integer pref : Prefs.getMatchedPrefs(word, resources)) {
            		builder.append(pref.toString());
            		builder.append(',');
            	}
            	if(builder.length() > 0) {
            		builder.insert(0,  "stations.pref IN (");
            		builder.setLength(builder.length() - 1);
            		builder.append(')');
            		format = builder.toString();
            		builder.setLength(0);
            	} else {
            		format = "0";
            	}
                break;
            }
            case DatabaseFilter.DATE:
            {
            	CompletionDateGroup.DateSpan span = CompletionDateGroup.getCompletionDateSpan(resources, word);
            	if(span == null) {
            		format = "1 ";
            	} else if(span.span > 0) {
                    builder.append("completions.comp_date BETWEEN ");
                    builder.append(span.begin);
                    builder.append(" AND ");
                    builder.append(span.begin + span.span);
                    builder.append(' ');
                    format = builder.toString();
                    builder.setLength(0);
                } else {
                	builder.append("completions.comp_date = ");
                	builder.append(span.begin);
                    builder.append(' ');
                    format = builder.toString();
                    builder.setLength(0);
                }
                escaped = false;
                formatted = true;
                break;
            }
	            default:
	                escaped = false;
	                formatted = true;
	                format = "1 ";
	                break;
	        }
	    	if(formatted) {
	    		condition = format;
	    	} else {
	    		builder.append(String.format(format, word));
	    		if(escaped) {
	    			builder.append(" ESCAPE '*' ");
	    		} else {
		    		builder.append(' ');
	    		}
		    	condition = builder.toString();
		    	builder.setLength(0);
	    	}
	    } else {
	        condition = "1 ";
	    }
	    tablesForFilterCondition = tables;

	    switch(visibilityType) {
	        case VisibilityType.COMPLETED_STATIONS:
	            word = "AND completions.comp_date > 0";
	            break;
	        case VisibilityType.INCOMPLETED_STATIONS:
	        	word = "AND completions.comp_date = 0";
	            break;
	        default:
	        	word = "";
	            break;
	    }
	    builder.append("stations.enabled = 1 AND ");
	    builder.append(condition);
	    builder.append(" AND completions.s_id = stations.s_id ");
	    builder.append(word);
	    filterSqlCondition = builder.toString();
	    builder.setLength(0);
	    builder.append("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date, "
                + "(lat - ?1) * (lat - ?1) + (lng - ?2) * (lng - ?2) AS distance FROM ");
	    builder.append(tablesForFilterCondition);
	    builder.append(" WHERE stations.lat >= ?3 AND stations.lat < ?4 AND stations.lng >= ?5 AND stations.lng < ?6 AND ");
	    builder.append(filterSqlCondition);
	    builder.append(" ORDER BY stations.weight, distance LIMIT ?7");
	    areaSql = builder.toString();
	}

	public OritsubushiNotificationIntent setMaxStations(Integer maxStations) {
		//this.maxStations = maxStations;
		final String newMaxStationsString = maxStations.toString();
		if(newMaxStationsString.equals(maxStationsString)) {
			return null;
		} else if(maxStationsString == null) {
			maxStationsString = newMaxStationsString;
			return null;
		} else {
			maxStationsString = newMaxStationsString;
			return new OritsubushiNotificationIntent().setMapStatusChanged();
		}
	}

	public SparseArrayCompat<Station> getStations(MapAreaV2 mapArea) {
		Cursor cursor = sqlite.rawQuery(areaSql, new String[] {
				Integer.toString(mapArea.getCenterLatitude()),
				Integer.toString(mapArea.getCenterLongitude()),
				Integer.toString(mapArea.getMinLatitude()),
				Integer.toString(mapArea.getMaxLatitude()),
				Integer.toString(mapArea.getMinLongitude()),
				Integer.toString(mapArea.getMaxLongitude()),
				maxStationsString
			} );
		SparseArrayCompat<Station> newStations = new SparseArrayCompat<Station>(cursor.getCount());
		while(cursor.moveToNext()) {
			Station station = new Station();
			station.setFromCursor(cursor, operators);
			newStations.put(station.getCode(), station);
		}
		cursor.close();
		return newStations;
	}

	//Android サービス接続ぶつ切れ対策
	public Station reloadStation(Station station) {
		final String[] param = new String[] { Integer.toString(station.getCode()) };
		Cursor cursor = sqlite.rawQuery("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date "
				+ "FROM stations completions "
				+ "WHERE stations.enabled = 1 AND completions.s_id = stations.s_id ", param);
		if(cursor.moveToNext()) {
			station.setFromCursor(cursor, operators);
		} else {
			station.setExpired();
		}
		cursor.close();
		return station.isExpired() ? station : getStationLines(station);
	}

	public Station getStationLines(Station station) {
		Cursor cursor = sqlite.rawQuery("SELECT 'lines'.* FROM 'lines', stations_lines"
				+ " WHERE stations_lines.s_id = ? AND lines.l_id = stations_lines.l_id AND lines.enabled = 1"
				+ " ORDER BY lines.l_id",
				new String[]{Integer.toString(station.getCode())});
		ArrayList<Line> lines = station.getLines();
		lines.clear();
		lines.ensureCapacity(cursor.getCount());
		while(cursor.moveToNext()) {
			Line line = new Line();
			line.setFromCursor(cursor);
			lines.add(line);
		}
		cursor.close();
		return station;
	}

	public OritsubushiNotificationIntent updateCompletion(Station station) {
		final String[] param = new String[] { Integer.toString(station.getCode()) };
		sqlite.beginTransaction();
		try {
			Cursor cursor = sqlite.rawQuery("SELECT comp_date FROM completions WHERE s_id = ?", param);
			final int recent = cursor.moveToNext() ? cursor.getInt(0) : 0;
			cursor.close();
			if(recent == station.getCompletionDate()) {
				Log.d("database", "completion not modified");
				return null;
			}
			ContentValues values = new ContentValues(2);
			values.put("comp_date", station.getCompletionDate());
			long updatedDate = System.currentTimeMillis() / 1000;	//for V2.2
			values.put("update_date", (int)updatedDate);
			if(sqlite.update("completions",  values,  "s_id = ?",  param) == 1) {
				sqlite.setTransactionSuccessful();
				Log.d("database", "completion updated");
				station.setUpdatedDate(updatedDate * 1000);	//for V2.2
				if((recent != 0) != (station.getCompletionDate() != 0)) {
					clearOperatorStatisticsCache(station);
					Log.d("database", "broadcast updating");
				} else {
					Log.d("database", "completion status not changed");
				}
				//TODO: sequence
				return new OritsubushiNotificationIntent().setStation(station, /*sequence*/0);
			} else {
				Log.d("database", "completion update failed");
				return null;
			}
		} catch(SQLiteException e) {
			e.printStackTrace();
			return null;
		} finally {
			sqlite.endTransaction();
		}
	}

	public void updateMemo(Station station) {
		final String[] params = new String[] { Integer.toString(station.getCode()), station.getMemo() };
		ContentValues values = new ContentValues(2);
		values.put("memo", station.getMemo());
		values.put("update_date", (int)(System.currentTimeMillis() / 1000));
		if(sqlite.update("completions",  values,  "s_id = ? AND memo != ?",  params) == 1) {
			Log.d("database", "memo updated");
		} else {
			Log.d("database", "memo not modified");
		}
	}

	public Group getTotalGroup() {
		if(totalStatisticsCache == null) {
			totalStatisticsCache = new Group();
			totalStatisticsCache.setCode(0);
			Cursor cursor = sqlite.rawQuery("SELECT COUNT(*) FROM stations WHERE enabled = 1", EMPTY);
			if(cursor.moveToNext()) {
				totalStatisticsCache.setTotal(cursor.getInt(0));
			}
			cursor.close();
			cursor = sqlite.rawQuery("SELECT COUNT(completions.s_id) FROM completions, stations WHERE completions.comp_date > 0 AND stations.s_id = completions.s_id AND stations.enabled = 1", EMPTY);
			if(cursor.moveToNext()) {
				totalStatisticsCache.setCompletions(cursor.getInt(0));
			}
			cursor.close();
		}
		return new Group(totalStatisticsCache);
	}

	public Pair<Group, List<Group>> getOperatorTypeGroups() {
//		Debug.startMethodTracing("optype");
		if(operatorTypesStatisticsCache.size() == 0) {
			Cursor cursor = sqlite.rawQuery("SELECT operators.type, COUNT(stations.s_id) FROM operators, stations "
					+ "WHERE operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1 "
					+ "GROUP BY operators.type", EMPTY);
			final int count = cursor.getCount();
			operatorTypesStatisticsCache.ensureCapacity(count);
			SparseIntArray map = new SparseIntArray(count);
			int index = 0;
			while(cursor.moveToNext()) {
				Group group = new Group();
				group.setCode(cursor.getInt(0));
				group.setTotal(cursor.getInt(1));
				operatorTypesStatisticsCache.add(group);
				map.put(group.getCode(), index++);
			}
			cursor.close();
			cursor = sqlite.rawQuery("SELECT operators.type, COUNT(completions.s_id) FROM operators, stations, completions "
					+ "WHERE operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0 "
					+ "GROUP BY operators.type", EMPTY);
			while(cursor.moveToNext()) {
				operatorTypesStatisticsCache.get(map.get(cursor.getInt(0))).setCompletions(cursor.getInt(1));
		    }
			cursor.close();
		}
//		Debug.stopMethodTracing();
		return new Pair<Group, List<Group>>(getTotalGroup(), Collections.unmodifiableList(operatorTypesStatisticsCache));
	}

	private Group reloadGroup(Group group, String totalSql, String completionSql) {
		group = new Group(group);
		final String[] param = new String[] { Integer.toString(group.getCode()) };
		Cursor cursor = sqlite.rawQuery(totalSql, param);
		if(cursor.moveToNext()) {
			group.setTotal(cursor.getInt(0));
		}
		cursor.close();
		if(group.getCode() != 0) {
			cursor = sqlite.rawQuery(completionSql, param);
			if(cursor.moveToNext()) {
				group.setCompletions(cursor.getInt(0));
			}
			cursor.close();
		}
		return group;
	}

	public Group reloadOperatorTypeGroup(Group group) {
		return reloadGroup(group,
				"SELECT COUNT(stations.s_id) FROM stations, operators "
						+ "WHERE operators.type = ? AND operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1",
				"SELECT COUNT(completions.s_id) FROM operators, stations, completions "
						+ "WHERE operators.type = ? AND operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1 "
						+ "AND completions.s_id = stations.s_id AND completions.comp_date > 0");
	}

	private Group searchGroup(ArrayList<Group> cache, int code) {
		for(Group group : cache) {
			if(group.getCode() == code) {
				return group;
			}
		}
		return null;
	}

	private void clearOperatorStatisticsCache(Station station) {
		totalStatisticsCache = null;
		if(station != null) {
			Operator operator = station.getOperator();
			final int type = operator.getOperatorType();
			Group group = searchGroup(operatorTypesStatisticsCache, type);
			if(group != null) {
				group.setCompletions(reloadOperatorTypeGroup(group).getCompletions());
			}
			ArrayList<Group> groups = operatorStatisticsCache.get(type);
			if(groups != null) {
				group = searchGroup(groups, operator.getCode());
				if(group != null) {
					group.setCompletions(reloadOperatorGroup(group).getCompletions());
				}
			}
			group = searchGroup(prefsStatisticsCache, station.getPref());
			if(group != null) {
				group.setCompletions(reloadPrefGroup(group).getCompletions());
			}
			group = searchGroup(yomiStatisticsCache, YomiUtils.getYomi1Code(station.getYomi()));
			if(group != null) {
				group.setCompletions(reloadYomiGroup(group).getCompletions());
			}
			/*final int date = station.getCompletionDate();
			group = searchGroup(yearStatisticsCache, date <= 1 ? date : date / 10000);
			if(group != null) {
				group.setCompletions(reloadYearGroup(group).getCompletions());
			}*/
		} else {
			operatorTypesStatisticsCache.clear();
			operatorStatisticsCache.clear();
			prefsStatisticsCache.clear();
			yomiStatisticsCache.clear();
			//yearStatisticsCache.clear();
		}
	}

	public Pair<Boolean, List<Group>> getOperatorGroupsNoStatistics(Integer operatorType) {
		if(OperatorTypes.needsNoStatisticsLoad(operatorType)) {
			final String[] param = new String[] { operatorType.toString() };
			ArrayList<Group> cache = operatorStatisticsCache.get(operatorType);
			if(cache != null) {
				return new Pair<Boolean, List<Group>>(true, new ArrayList<Group>(cache));
			}
			Cursor cursor = sqlite.rawQuery("SELECT o_id, operator FROM operators WHERE type = ? AND enabled = 1 ORDER BY o_id", param);
			ArrayList<Group> groups = new ArrayList<Group>(cursor.getCount());
			while(cursor.moveToNext()) {
				Group group = new Group();
				group.setCode(cursor.getInt(0));
				group.setTitle(cursor.getString(1));
				group.setTotal(0);
				groups.add(group);
			}
			cursor.close();
			return new Pair<Boolean, List<Group>>(false, groups);
		} else {
			return new Pair<Boolean, List<Group>>(true, getOperatorGroups(operatorType));
		}
	}

	public List<Group> getOperatorGroups(Integer operatorType) {
//		Debug.startMethodTracing("op");
		final String[] param = new String[] { operatorType.toString() };
		ArrayList<Group> cache = operatorStatisticsCache.get(operatorType);
		if(cache == null) {
			Cursor cursor = sqlite.rawQuery("SELECT operators.o_id, operators.operator, COUNT(stations.s_id) FROM operators, stations "
					+ "WHERE operators.type = ? AND operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1 "
					+ "GROUP BY operators.o_id ORDER BY operators.o_id", param);
			final int count = cursor.getCount();
			cache = new ArrayList<Group>(count);
			SparseIntArray map = new SparseIntArray(count);
			int index = 0;
			while(cursor.moveToNext()) {
				final Group group = new Group();
				group.setCode(cursor.getInt(0));
				group.setTitle(cursor.getString(1));
				group.setTotal(cursor.getInt(2));
				cache.add(group);
				map.put(group.getCode(), index++);
			}
			cursor.close();
			cursor = sqlite.rawQuery("SELECT operators.o_id, COUNT(completions.s_id) FROM operators, stations, completions "
					+ "WHERE operators.type = ? AND operators.enabled = 1 AND stations.o_id = operators.o_id AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0 "
					+ "GROUP BY operators.o_id", param);
			while(cursor.moveToNext()) {
				final Group group = cache.get(map.get(cursor.getInt(0)));
				if(group != null) {
					group.setCompletions(cursor.getInt(1));
				}
			}
			cursor.close();
			operatorStatisticsCache.put(operatorType, cache);
		}
//		Debug.stopMethodTracing();
		return Collections.unmodifiableList(cache);
	}

	public Group reloadOperatorGroup(Group group) {
		return reloadGroup(group,
				"SELECT COUNT(*) FROM stations WHERE o_id = ? AND enabled = 1",
				"SELECT COUNT(completions.s_id) FROM stations, completions "
						+ "WHERE stations.o_id = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0");
	}

	public List<Group> getLineGroups(Integer operatorCode) {
		final String[] param = new String[] { operatorCode.toString() };
		Cursor cursor = sqlite.rawQuery("SELECT lines.l_id, lines.line, COUNT(stations.s_id) "
				+ "FROM \"lines\", stations, stations_lines "
				+ "WHERE lines.o_id = ? AND lines.enabled = 1 AND stations_lines.l_id = lines.l_id AND stations.s_id = stations_lines.s_id AND stations.enabled = 1 "
				+ "GROUP BY lines.l_id ORDER BY lines.l_id", param);
		final int count = cursor.getCount();
		ArrayList<Group> groups = new ArrayList<Group>(count);
		SparseIntArray map = new SparseIntArray(count);
		int index = 0;
		while(cursor.moveToNext()) {
			final Group group = new Group();
			group.setCode(cursor.getInt(0));
			group.setTitle(cursor.getString(1));
			group.setTotal(cursor.getInt(2));
			groups.add(group);
			map.put(group.getCode(), index++);
		}
		cursor.close();
		cursor = sqlite.rawQuery("SELECT lines.l_id, COUNT(completions.s_id) "
				+ "FROM \"lines\", stations, stations_lines, completions "
				+ "WHERE lines.o_id = ? AND lines.enabled = 1 AND stations_lines.l_id = lines.l_id AND stations.s_id = stations_lines.s_id "
				+ " AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0 "
				+ "GROUP BY lines.l_id", param);
		while(cursor.moveToNext()) {
			final Group group = groups.get(map.get(cursor.getInt(0)));
			if(group != null) {
				group.setCompletions(cursor.getInt(1));
			}
		}
		cursor.close();
		return groups;
	}

	public Group reloadLineGroup(Group group) {
		return reloadGroup(group,
				"SELECT COUNT(stations.s_id) FROM \"lines\", stations, stations_lines "
						+ "WHERE lines.l_id = ? AND lines.enabled = 1 AND stations_lines.l_id = lines.l_id AND stations.s_id = stations_lines.s_id  AND stations.enabled = 1",
				"SELECT COUNT(completions.s_id) FROM \"lines\", stations, stations_lines, completions "
						+ "WHERE lines.l_id = ? AND lines.enabled = 1 AND stations_lines.l_id = lines.l_id AND stations.s_id = stations_lines.s_id "
						+ "AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0");
	}

	public List<Station> getStationsByLine(Integer lineCode) {
		final String[] param = new String[] { lineCode.toString() };
		Cursor cursor = sqlite.rawQuery("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date "
				+ "FROM stations, stations_lines, completions "
				+ "WHERE stations_lines.l_id = ? AND stations.s_id = stations_lines.s_id AND stations.enabled = 1 AND completions.s_id = stations.s_id "
				+ "ORDER BY stations_lines.s_sort", param);
		ArrayList<Station> newStations = new ArrayList<Station>(cursor.getCount());
		while(cursor.moveToNext()) {
			Station station = new Station();
			station.setFromCursor(cursor, operators);
			newStations.add(station);
		}
		cursor.close();
		return newStations;
	}

	public Pair<Group, List<Group>> getPrefGroups() {
		if(prefsStatisticsCache.size() == 0) {
			Cursor cursor = sqlite.rawQuery("SELECT stations.pref, COUNT(stations.s_id) FROM stations WHERE stations.enabled = 1 GROUP BY stations.pref ORDER BY stations.pref", EMPTY);
			final int count = cursor.getCount();
			prefsStatisticsCache.ensureCapacity(count);
			SparseIntArray map = new SparseIntArray(count);
			int index = 0;
			while(cursor.moveToNext()) {
				Group group = new Group();
				group.setCode(cursor.getInt(0));
				group.setTotal(cursor.getInt(1));
				prefsStatisticsCache.add(group);
				map.put(group.getCode(), index++);
			}
			cursor.close();
			cursor = sqlite.rawQuery("SELECT stations.pref, COUNT(completions.s_id) FROM stations, completions "
					+ "WHERE stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0 GROUP BY stations.pref", EMPTY);
			while(cursor.moveToNext()) {
				prefsStatisticsCache.get(map.get(cursor.getInt(0))).setCompletions(cursor.getInt(1));
		    }
			cursor.close();
		}
		return new Pair<Group, List<Group>>(getTotalGroup(), Collections.unmodifiableList(prefsStatisticsCache));
	}

	public Group reloadPrefGroup(Group group) {
		return reloadGroup(group,
				"SELECT COUNT(*) FROM stations WHERE pref = ? AND enabled = 1",
				"SELECT COUNT(completions.s_id) FROM stations, completions "
						+ "WHERE stations.pref = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0");
	}

	public List<Station> getStationsByPref(Integer prefCode) {
		final String[] param = new String[] { prefCode.toString() };
		Cursor cursor = sqlite.rawQuery("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date "
				+ "FROM stations, completions "
				+ "WHERE stations.pref = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id ORDER BY stations.address", param);
		ArrayList<Station> newStations = new ArrayList<Station>(cursor.getCount());
		while(cursor.moveToNext()) {
			Station station = new Station();
			station.setFromCursor(cursor, operators);
			newStations.add(station);
		}
		cursor.close();
		return newStations;
	}

	public Pair<Group, List<Group>> getYomiGroups() {
		if(yomiStatisticsCache.size() == 0) {
			Cursor cursor = sqlite.rawQuery("SELECT SUBSTR(stations.yomi, 1, 1) AS yomi1, COUNT(stations.s_id) "
					+ "FROM stations WHERE stations.enabled = 1 GROUP BY yomi1 ORDER BY yomi1", EMPTY);
			final int count = cursor.getCount();
			yomiStatisticsCache.ensureCapacity(count);
			SparseIntArray map = new SparseIntArray(count);
			int index = 0;
			while(cursor.moveToNext()) {
				final Group group = new Group();
				final String yomi1 = cursor.getString(0);
				group.setCode(YomiUtils.getYomi1Code(yomi1));
				group.setTitle(yomi1);
				group.setTotal(cursor.getInt(1));
				yomiStatisticsCache.add(group);
				map.put(group.getCode(), index++);
			}
			cursor.close();
			cursor = sqlite.rawQuery("SELECT SUBSTR(stations.yomi, 1, 1) AS yomi1, COUNT(completions.s_id) "
					+ "FROM stations, completions WHERE stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0 GROUP BY yomi1", EMPTY);
			while(cursor.moveToNext()) {
				yomiStatisticsCache.get(map.get(YomiUtils.getYomi1Code(cursor.getString(0)))).setCompletions(cursor.getInt(1));
		    }
			cursor.close();
		}
		return new Pair<Group, List<Group>>(getTotalGroup(), Collections.unmodifiableList(yomiStatisticsCache));
	}

	public List<Group> getYomi2Groups(Group group) {
		final String[] param = new String[] { new String(new char[] { (char)(group.getCode() & 0xffff) }) };
		Cursor cursor = sqlite.rawQuery("SELECT SUBSTR(stations.yomi, 1, 2) AS yomi2, COUNT(stations.s_id) "
				+ "FROM stations WHERE SUBSTR(stations.yomi, 1, 1) = ? AND stations.enabled = 1 GROUP BY yomi2 ORDER BY yomi2", param);
		final int count = cursor.getCount();
		ArrayList<Group> groups = new ArrayList<Group>(count);
		SparseIntArray map = new SparseIntArray(count);
		int index = 0;
		while(cursor.moveToNext()) {
			final Group group_ = new Group();
			final String yomi2 = cursor.getString(0);
			final int code = YomiUtils.getYomi2Code(yomi2);
			group_.setCode(code);
			group_.setTitle(yomi2);
			group_.setTotal(cursor.getInt(1));
			groups.add(group_);
			map.put(code, index++);
		}
		cursor.close();
		cursor = sqlite.rawQuery("SELECT SUBSTR(stations.yomi, 1, 2) AS yomi2, COUNT(completions.s_id) "
				+ "FROM stations, completions "
				+ "WHERE SUBSTR(stations.yomi, 1, 1) = ? AND completions.s_id = stations.s_id AND stations.enabled = 1 AND completions.comp_date > 0 "
				+ "GROUP BY yomi2", param);
		while(cursor.moveToNext()) {
			groups.get(map.get(YomiUtils.getYomi2Code(cursor.getString(0)))).setCompletions(cursor.getInt(1));
	    }
		cursor.close();
		return groups;
	}

	private Group reloadYomiGroup(Group group, String yomiLength, String yomi) {
		final String[] params = new String[]{ yomiLength, yomi };
		final String totalSql = "SELECT COUNT(*) FROM stations WHERE SUBSTR(yomi, 1, ?) = ? AND enabled = 1";
		final String completionSql = "SELECT COUNT(completions.s_id) FROM stations, completions "
			+ "WHERE SUBSTR(stations.yomi, 1, ?) = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id AND completions.comp_date > 0";
		Cursor cursor = sqlite.rawQuery(totalSql, params);
		if(cursor.moveToNext()) {
			group.setTotal(cursor.getInt(0));
		}
		cursor.close();
		if(group.getCode() != 0) {
			cursor = sqlite.rawQuery(completionSql, params);
			if(cursor.moveToNext()) {
				group.setCompletions(cursor.getInt(0));
			}
			cursor.close();
		}
		return group;
	}

	public Group reloadYomiGroup(Group group) {
		return reloadYomiGroup(group, "1", new String(new char[] { (char)(group.getCode() & 0xffff) }));
	}

	public Group reloadYomi2Group(Group group) {
		return reloadYomiGroup(group, "2", new String(new char[] { (char)(group.getCode() & 0xffff), (char)(group.getCode() >>> 16) }));
	}

	public List<Station> getStationsByYomi(String yomi2) {
		final String[] param = new String[]{ yomi2 };
		Cursor cursor = sqlite.rawQuery("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date "
				+ "FROM stations, completions "
				+ "WHERE SUBSTR(stations.yomi, 1, 2) = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id "
				+ "ORDER BY stations.address", param);
		ArrayList<Station> newStations = new ArrayList<Station>(cursor.getCount());
		while(cursor.moveToNext()) {
			Station station = new Station();
			station.setFromCursor(cursor, operators);
			newStations.add(station);
		}
		cursor.close();
		return newStations;
	}

	public Pair<Group, List<Group>> getYearGroups() {
		final Group totalGroup = getTotalGroup();
		final int total = totalGroup.getTotal();
		final int comp = totalGroup.getCompletions();
		//TODO:仮にキャッシュなし
		ArrayList<Group> yearStatisticsCache = new ArrayList<Group>();
		//if(yearStatisticsCache.size() == 0) {
			Cursor cursor = sqlite.rawQuery("SELECT completions.comp_date / 10000 AS year, COUNT(stations.s_id) "
					+ "FROM stations, completions "
					+ "WHERE completions.s_id = stations.s_id AND stations.enabled = 1 AND completions.comp_date > 0 "
					+ "GROUP BY year ORDER BY year DESC", EMPTY);
			final int count = cursor.getCount();
			yearStatisticsCache.ensureCapacity(count + 1);
			while(cursor.moveToNext()) {
				final Group group = new CompletionDateGroup();
				final int year = cursor.getInt(0);
				group.setCode(year > 0 ? year : 1);
				group.setTotal(comp);
				group.setCompletions(cursor.getInt(1));
				yearStatisticsCache.add(group);
			}
			cursor.close();
			if(total - comp > 0) {
				final Group group = new CompletionDateGroup();
				group.setCode(0);
				group.setTotal(total);
				group.setCompletions(total - comp);
				yearStatisticsCache.add(group);
			}
		//}
		return new Pair<Group, List<Group>>(totalGroup, Collections.unmodifiableList(yearStatisticsCache));
	}

	public List<Group> getMonthGroups(Group yearGroup) {
		final int comp = yearGroup.getCompletions();
		final int year = yearGroup.getCode() * 10000;
		final String[] params = new String[] { Integer.toString(year), Integer.toString(year + 1231) };
		Cursor cursor = sqlite.rawQuery("SELECT completions.comp_date / 100 AS month, COUNT(stations.s_id) "
				+ "FROM stations, completions "
				+ "WHERE completions.s_id = stations.s_id AND stations.enabled = 1 AND completions.comp_date BETWEEN ? AND ? "
				+ "GROUP BY month ORDER BY month", params);
		final int count = cursor.getCount();
		ArrayList<Group> groups = new ArrayList<Group>(count);
		Group anbiguousGroup = null;
		while(cursor.moveToNext()) {
			final Group group = new CompletionDateGroup();
			final int month = cursor.getInt(0);
			group.setCode(month);
			group.setTotal(comp);
			group.setCompletions(cursor.getInt(1));
			if(month % 100 != 0) {
				groups.add(group);
			} else {
				anbiguousGroup = group;
			}
		}
		cursor.close();
		if(anbiguousGroup != null) {
			groups.add(anbiguousGroup);
		}
		return groups;
	}

	public List<Group> getDayGroups(Group monthGroup) {
		final int comp = monthGroup.getCompletions();
		final int month = monthGroup.getCode() * 100;
		final String[] params = new String[] { Integer.toString(month), Integer.toString(month + 31) };
		Cursor cursor = sqlite.rawQuery("SELECT completions.comp_date, COUNT(stations.s_id) "
				+ "FROM stations, completions "
				+ "WHERE completions.s_id = stations.s_id AND stations.enabled = 1 AND completions.comp_date BETWEEN ? AND ? "
				+ "GROUP BY completions.comp_date ORDER BY completions.comp_date", params);
		final int count = cursor.getCount();
		ArrayList<Group> groups = new ArrayList<Group>(count);
		Group anbiguousGroup = null;
		while(cursor.moveToNext()) {
			final Group group = new CompletionDateGroup();
			final int day = cursor.getInt(0);
			group.setCode(day);
			group.setTotal(comp);
			group.setCompletions(cursor.getInt(1));
			if(day % 100 != 0) {
				groups.add(group);
			} else {
				anbiguousGroup = group;
			}
		}
		cursor.close();
		if(anbiguousGroup != null) {
			groups.add(anbiguousGroup);
		}
		return groups;
	}

	private int loadCompCount(int minDate, int maxDate) {
		final String[] params = new String[]{ Integer.toString(minDate), Integer.toString(maxDate) };
		Cursor cursor = sqlite.rawQuery("SELECT COUNT(stations.s_id) FROM stations, completions "
				+ "WHERE completions.s_id = stations.s_id AND stations.enabled = 1 AND completions.comp_date BETWEEN ? AND ?", params);
		final int result = cursor.moveToNext() ? cursor.getInt(0) : 0;
		cursor.close();
		return result;
	}

	public Group reloadYearGroup(Group group) {
		group = new Group(group);
		final Group totalGroup = getTotalGroup();
		int year = group.getCode();
		if(year > 0) {
			group.setTotal(totalGroup.getCompletions());
			int minDate, maxDate;
			if(year > 1) {
				year *= 10000;
				minDate = year;
				maxDate = year + 1231;
			} else {
				minDate = maxDate = 1;
			}
			group.setCompletions(loadCompCount(minDate, maxDate));
		} else {
			group.setTotal(totalGroup.getTotal());
			group.setCompletions(totalGroup.getIncompletions());
		}
		return group;
	}

	public Group reloadMonthGroup(Group group) {
		group = new Group(group);
		int month = group.getCode() * 100;
		int year = month - month % 10000;
		group.setTotal(loadCompCount(year, year + 1231));
		int minDate, maxDate;
		if(month % 10000 != 0) {
			minDate = month;
			maxDate = month + 31;
		} else {
			minDate = maxDate = month;
		}
		group.setCompletions(loadCompCount(minDate, maxDate));
		return group;
	}

	public Group reloadDayGroup(Group group) {
		group = new Group(group);
		final int date = group.getCode();
		final int month = date - date % 100;
		group.setTotal(loadCompCount(month, month + 31));
		group.setCompletions(loadCompCount(date, date));
		return group;
	}

	public List<Station> getStationsByCompDate(Group group) {
		int compDate = group.getCode();
		if(999 < compDate && compDate < 999999) {
			compDate *= 100;
		}
		final String[] param = new String[]{ Integer.toString(compDate) };
		Cursor cursor = sqlite.rawQuery("SELECT stations.*, completions.comp_date, completions.memo, completions.update_date "
				+ "FROM stations, completions "
				+ "WHERE completions.comp_date = ? AND stations.enabled = 1 AND completions.s_id = stations.s_id "
				+ "ORDER BY stations.s_id", param);
		ArrayList<Station> newStations = new ArrayList<Station>(cursor.getCount());
		while(cursor.moveToNext()) {
			Station station = new Station();
			station.setFromCursor(cursor, operators);
			newStations.add(station);
		}
		cursor.close();
		return newStations;
	}

	public SyncPreparationProperties prepareSync(SyncPreparationProperties properties) {
		FileOutputStream outputStream = null;
		BufferedWriter writer = null;
		Cursor cursor = null;
		try {
			properties.file = new File(properties.file.getPath(), UPLOAD_FILE_NAME);
			properties.file.deleteOnExit();
			outputStream = new FileOutputStream(properties.file);
			OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, "UTF-8");
			writer = new BufferedWriter(streamWriter);
			cursor = sqlite.rawQuery("SELECT * FROM completions WHERE update_date > ?", new String[] { properties.updateDate });
			while(cursor.moveToNext()) {
				writer.write(String.format("%d\t%d\t%d\t%s\n",
						cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3)));
			}
			properties.databaseVersion = Integer.toString(sqlite.getVersion());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(writer != null) { try { writer.close(); } catch(IOException ee) {} }
			if(outputStream != null) { try { outputStream.close(); } catch(IOException ee) {} }
			if(cursor != null) { cursor.close(); }
		}
		return properties;
	}

}