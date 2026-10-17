package com.ayuemin.agent
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class ChatDatabase(context: Context): SQLiteOpenHelper(context,"ayuemin.db",null,2){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,chat TEXT,role TEXT,body TEXT,time INTEGER)")
  db.execSQL("CREATE VIRTUAL TABLE search USING fts4(body)")
  db.execSQL("CREATE TRIGGER messages_ai AFTER INSERT ON messages BEGIN INSERT INTO search(rowid,body) VALUES(new.id,new.body); END")
  db.execSQL("CREATE TABLE usages(id INTEGER PRIMARY KEY,chat TEXT,input INTEGER,output INTEGER,cached INTEGER,expense REAL)")
  db.execSQL("CREATE TABLE summaries(chat TEXT PRIMARY KEY,text TEXT,until_id INTEGER DEFAULT 0)")
  createVectors(db)
 }
 private fun createVectors(db:SQLiteDatabase){db.execSQL("CREATE TABLE IF NOT EXISTS vectors(message_id INTEGER NOT NULL,model TEXT NOT NULL,dimensions INTEGER NOT NULL,data TEXT NOT NULL,PRIMARY KEY(message_id,model))")}
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){if(oldVersion<2)createVectors(db)}
}
