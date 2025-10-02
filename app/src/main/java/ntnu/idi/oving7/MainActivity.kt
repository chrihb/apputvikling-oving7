package ntnu.idi.oving7

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import ntnu.idi.oving7.databinding.ActivityMainBinding
import androidx.core.graphics.toColorInt


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var db: DatabaseManager
    private lateinit var ui: ActivityMainBinding

    private val queries = listOf(
        "Select query...",
        "All movies",
        "All movies from Christopher Nolan",
        "Actors in Django Unchained"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        querySpinnerAdapter()


        val movies = readMoviesFile()

        db = DatabaseManager(this)
        db.insertMovieData(movies)

        writeMoviesToStorage(movies, "movies_internal.json")
    }

    override fun onResume() {
        super.onResume()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val color = prefs.getString("bg_color", "#FFFFFF")
        ui.queryResult.setBackgroundColor(color?.toColorInt() ?: Color.WHITE)
    }

    fun onSettingsClick(v: View?) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun readMoviesFile(): MoviesWrapper {
        val json = resources.openRawResource(R.raw.movies)
            .bufferedReader()
            .use { it.readText() }

        val wrapper = Gson().fromJson(json, MoviesWrapper::class.java)
        return MoviesWrapper(wrapper.movies)
    }

    fun writeMoviesToStorage(movies: MoviesWrapper, filename: String) {
        val json = Gson().toJson(movies)
        openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            output.write(json.toByteArray())
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selected = parent?.getItemAtPosition(position).toString()

        val result: List<String> = when (selected) {
            queries[1] -> db.getAllMovieTitles()
            queries[2] -> db.getMoviesByDirector("Christopher Nolan")
            queries[3] -> db.getActorsInMovie("Django Unchained")
            else -> emptyList()
        }

        val text = if (result.isEmpty()) {
            "No results found."
        } else {
            result.joinToString(separator = "\n") { "• $it" }
        }

        Log.d("MainActivity", "Query result:\n$text")

        ui.queryResult.text = text
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun querySpinnerAdapter() {
        val spinner = ui.querySpinner

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            queries
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }
}