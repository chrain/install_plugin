package com.xstd.ip.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Chrain on 13-12-11.
 */
public class SilenceAppDaoUtils {

	private static final String DATABASE_NAME = "sip.db";

	private static DaoSession sDaoSession;

	public static DaoSession getDaoSession(Context context) {

		if (sDaoSession == null) {

			DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, DATABASE_NAME, null);
			SQLiteDatabase database = helper.getWritableDatabase();
			DaoMaster m = new DaoMaster(database);

			sDaoSession = m.newSession();

		}

		return sDaoSession;
	}

	public static SilenceAppDao getSilenceAppDao(Context context) {
		if (sDaoSession == null) {
			return getDaoSession(context).getSilenceAppDao();
		}
		return sDaoSession.getSilenceAppDao();
	}
}
