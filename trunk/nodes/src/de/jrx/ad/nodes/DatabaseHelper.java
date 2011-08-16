package de.jrx.ad.nodes;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME="nodes";
	static final String NAME="name";
	static final String URI="uri";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 4);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, uri TEXT);");

		ContentValues cv=new ContentValues();

		cv.put(NAME, "localhost");
		cv.put(URI, "coap://[::1]/");
		db.insert("bookmarks", NAME, cv);

		cv.put(NAME, "localhost, well-known");
		cv.put(URI, "coap://[::1]/.well-known/core");
		db.insert("bookmarks", NAME, cv);

		cv.put(NAME, "Fake Sensor 1");
		cv.put(URI, "coap://[2001:db8::2:1]/");
		db.insert("bookmarks", NAME, cv);

		cv.put(NAME, "Fake Sensor 2");
		cv.put(URI, "coap://[2001:0638:0804:a100:0a00:27ff:fe00:0102]/");
		db.insert("bookmarks", NAME, cv);

		cv.put(NAME, "Fake Sensor 2 (ll)");
		cv.put(URI, "coap://[fe80::a00:27ff:fe00:102]/");
		db.insert("bookmarks", NAME, cv);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		android.util.Log.w("bookmarks", "Upgrading database, which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS bookmarks");
		onCreate(db);
	}
}