package com.ayuemin.agent;
import android.content.Context;
import android.database.sqlite.*;
/** Local schema; never truncate history during compaction. */
public final class ChatDatabase extends SQLiteOpenHelper {
 ChatDatabase(Context c){super(c,"ayuemin.db",null,1);}

    @Override public void onCreate(SQLiteDatabase d){
      d.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,chat TEXT,role TEXT,body TEXT,time INTEGER)");
      d.execSQL("CREATE VIRTUAL TABLE search USING fts4(body)");
      d.execSQL("CREATE TRIGGER messages_ai AFTER INSERT ON messages BEGIN INSERT INTO search(rowid,body) VALUES(new.id,new.body); END");
      d.execSQL("CREATE TABLE usages(id INTEGER PRIMARY KEY,chat TEXT,input INTEGER,output INTEGER,cached INTEGER,expense REAL)");
      d.execSQL("CREATE TABLE summaries(chat TEXT PRIMARY KEY,text TEXT,until_id INTEGER DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int o,int n){}
}
