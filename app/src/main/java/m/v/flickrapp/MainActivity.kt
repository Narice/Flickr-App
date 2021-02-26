package m.v.flickrapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageButton.setOnClickListener {
            AsyncFlickrJSONData()
                    .execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=trees&format=json")
        }

        listViewButton.setOnClickListener {
            startActivity(Intent(applicationContext, ListActivity::class.java))
        }

        geoButton.setOnClickListener {
            startActivity(Intent(applicationContext, GeoActivity::class.java))
        }
    }

    inner class AsyncFlickrJSONData : AsyncTask<String, Void, JSONObject>() {
        override fun doInBackground(vararg params: String): JSONObject? {
            try {
                val url = URL(params[0])
                val urlConnection: HttpsURLConnection =
                        url.openConnection() as HttpsURLConnection
                try {
                    val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                    val s: String? = readStream(`in`)
                    if (s != null) {
                        val result = s.substring(15, s.lastIndex)
                        return JSONObject(result)
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            Log.i("JFL", result.toString())
            AsyncBitmapDownloader()
                    .execute(result
                            ?.getJSONArray("items")
                            ?.getJSONObject(0)
                            ?.getJSONObject("media")
                            ?.getString("m")
                    )
        }

        private fun readStream(`is`: InputStream): String? {
            return try {
                val bo = ByteArrayOutputStream()
                var i = `is`.read()
                while (i != -1) {
                    bo.write(i)
                    i = `is`.read()
                }
                bo.toString()
            } catch (e: IOException) {
                ""
            }
        }
    }

    inner class AsyncBitmapDownloader : AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg params: String): Bitmap? {
            try {
                val url = URL(params[0])
                val urlConnection: HttpsURLConnection =
                        url.openConnection() as HttpsURLConnection
                try {
                    val inputStream: InputStream = BufferedInputStream(urlConnection.inputStream)
                    val b: Bitmap? = BitmapFactory.decodeStream(inputStream)
                    if (b != null) {
                        return b
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            Log.i("JFL", result.toString())
            image.setImageBitmap(result)
        }
    }
}