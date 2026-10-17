package com.ayuemin.agent;
import android.content.Context;
import android.database.sqlite.*;
/** Local schema; never truncate history during compaction. */
public final class ChatDatabase extends SQLiteOpenHelper {
 ChatDatabase(Context c){super(c,"ayuemin.db",null,2);}

    @Override public void onCreate(SQLiteDatabase d){
      d.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,chat TEXT,role TEXT,body TEXT,time INTEGER)");
      d.execSQL("CREATE VIRTUAL TABLE search USING fts4(body)");
      d.execSQL("CREATE TRIGGER messages_ai AFTER INSERT ON messages BEGIN INSERT INTO search(rowid,body) VALUES(new.id,new.body); END");
      d.execSQL("CREATE TABLE usages(id INTEGER PRIMARY KEY,chat TEXT,input INTEGER,output INTEGER,cached INTEGER,expense REAL)");
      d.execSQL("CREATE TABLE summaries(chat TEXT PRIMARY KEY,text TEXT,until_id INTEGER DEFAULT 0)");
      createVectors(d);
    }
    private void createVectors(SQLiteDatabase d){d.execSQL("CREATE TABLE IF NOT EXISTS vectors(message_id INTEGER NOT NULL,model TEXT NOT NULL,dimensions INTEGER NOT NULL,data TEXT NOT NULL,PRIMARY KEY(message_id,model))");}
    @Override public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<2)createVectors(d);}
}
