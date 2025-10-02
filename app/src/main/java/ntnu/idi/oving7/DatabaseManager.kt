package ntnu.idi.oving7

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseManager(context: Context) : SQLiteOpenHelper(context, "movies.db", null, 3) {

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE DIRECTOR(_id INTEGER PRIMARY KEY AUTOINCREMENT, director TEXT NOT NULL)")
        db?.execSQL("CREATE TABLE MOVIE(_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, director_id INTEGER NOT NULL, FOREIGN KEY(director_id) REFERENCES DIRECTOR(_id) ON DELETE CASCADE)")

        db?.execSQL("CREATE TABLE ACTOR(_id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, movie_id INTEGER NOT NULL, FOREIGN KEY(movie_id) REFERENCES MOVIE(_id) ON DELETE CASCADE)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ACTOR")
        db?.execSQL("DROP TABLE IF EXISTS MOVIE")
        db?.execSQL("DROP TABLE IF EXISTS DIRECTOR")

        onCreate(db)
    }

    fun insertMovieData(movies: MoviesWrapper) {
        writableDatabase.apply {
            beginTransaction()
            try {
                movies.movies.forEach { m ->
                    val dirId = getOrInsertDirector(this, m.director)
                    val movieId = insert("MOVIE", null, ContentValues().apply {
                        put("title", m.title)
                        put("director_id", dirId)
                    })
                    m.actors.forEach { a ->
                        insert("ACTOR", null, ContentValues().apply {
                            put("actor", a)
                            put("movie_id", movieId)
                        })
                    }
                }
                setTransactionSuccessful()
            } finally { endTransaction() }
        }
    }

    fun getAllMovieTitles(): List<String> {
        val titles = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT title FROM Movie", null).use { cursor ->
            while (cursor.moveToNext()) {
                titles.add(cursor.getString(0))
            }
        }
        return titles
    }

    fun getMoviesByDirector(name: String): List<String> {
        val titles = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT m.title FROM MOVIE m JOIN DIRECTOR d ON m.director_id = d._id WHERE d.director = ?",
            arrayOf(name)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                titles.add(cursor.getString(0))
            }
        }
        return titles
    }

    fun getActorsInMovie(name: String): List<String> {
        val actors = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT a.actor FROM ACTOR a JOIN MOVIE m ON a.movie_id = m._id WHERE m.title = ?",
            arrayOf(name)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                actors.add(cursor.getString(0))
            }
        }
        return actors
    }


    private fun getOrInsertDirector(db: SQLiteDatabase, name: String): Long {
        val cursor = db.query(
            "DIRECTOR",
            arrayOf("_id"),
            "director = ?",
            arrayOf(name),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        val values = ContentValues().apply { put("director", name) }
        return db.insert("DIRECTOR", null, values)
    }
}