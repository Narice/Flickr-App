package m.v.flickrapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_geo.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.function.Consumer
import javax.net.ssl.HttpsURLConnection

class GeoActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        geoImageButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
                return@setOnClickListener
            }
            locationManager.getCurrentLocation(
                LocationManager.NETWORK_PROVIDER,
                null, mainExecutor,
                Consumer { location: Location ->
                    Log.i("JFL", "Altitude: ${location.altitude}, Latitude: ${location.latitude}")
                    AsyncFlickrJSONData()
                        .execute("https://api.flickr.com/services/rest/?method=flickr.photos.search" +
                                "&license=4" +
                                "&api_key=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                                "&has_geo=1" +
                                "&lat=${location.latitude}" +
                                "&lon=${location.longitude}" +
                                "&per_page=1" +
                                "&format=json")
                })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
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
                        val result = s.substring(14, s.lastIndex)
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
            val photo = result
                ?.getJSONObject("photos")
                ?.getJSONArray("photo")
                ?.getJSONObject(0)
            val server = photo?.getString("server")
            val id = photo?.getString("id")
            val secret = photo?.getString("secret")
            AsyncBitmapDownloader()
                .execute(
                    "https://live.staticflickr.com/${server}/${id}_${secret}_m.jpg"
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
            geoImageView.setImageBitmap(result)
        }
    }
}
