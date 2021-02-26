package m.v.flickrapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.bitmaplayout.view.*
import kotlinx.android.synthetic.main.textviewlayout.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class ListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        list.adapter = MyAdapter(this)

        AsyncFlickrJSONDataForList(list.adapter as MyAdapter)
            .execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=trees&format=json")
    }

    class MyAdapter(private val context: Context) : BaseAdapter() {

        val vector: Vector<String> = Vector()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            //val cView = convertView
            //    ?: LayoutInflater
            //        .from(context)
            //        .inflate(R.layout.textviewlayout, parent, false)
            //cView.textView.text = getItem(position) as String
            //return cView
            val cView = convertView
                ?: LayoutInflater
                    .from(context)
                    .inflate(R.layout.bitmaplayout, parent, false)
            MySingleton
                .getInstance(context.applicationContext)
                .requestQueue
                .add(
                    ImageRequest(
                        getItem(position) as String,
                        {response: Bitmap ->
                            cView.imageView.setImageBitmap(response)
                        },
                        500,
                        500,
                        ImageView.ScaleType.CENTER_INSIDE,
                        Bitmap.Config.ARGB_8888,
                        null
                    )
                )
            return cView
        }

        override fun getItem(position: Int): Any {
            return vector[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return vector.size
        }

        fun dd(url: String) {
            vector.add(url)
        }
    }

    class AsyncFlickrJSONDataForList(val adapter: MyAdapter) : AsyncTask<String, Void, JSONObject>() {
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
            val jsonArray = result?.getJSONArray("items")?: JSONArray()
            for (counter in 0 until jsonArray.length()) {

                adapter.dd(jsonArray
                    .getJSONObject(counter)
                    ?.getJSONObject("media")
                    ?.getString("m")?:""
                )
            }
            adapter.notifyDataSetChanged()
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

    class MySingleton constructor(context: Context) {
        companion object {
            @Volatile
            private var INSTANCE: MySingleton? = null
            fun getInstance(context: Context) =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: MySingleton(context).also {
                        INSTANCE = it
                    }
                }
        }
        val imageLoader: ImageLoader by lazy {
            ImageLoader(requestQueue,
                object : ImageLoader.ImageCache {
                    private val cache = LruCache<String, Bitmap>(20)
                    override fun getBitmap(url: String): Bitmap {
                        return cache.get(url)
                    }
                    override fun putBitmap(url: String, bitmap: Bitmap) {
                        cache.put(url, bitmap)
                    }
                })
        }
        val requestQueue: RequestQueue by lazy {
            // applicationContext is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            Volley.newRequestQueue(context.applicationContext)
        }
        fun <T> addToRequestQueue(req: Request<T>) {
            requestQueue.add(req)
        }
    }
}