package com.example.my123


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.my123.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var binNumber = ""
    var bankUrl = ""
    var latitude = ""
    var longitude = ""
    var phone = ""
    var binHistory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewHistory()


    }

    fun viewHistory() {
        val filename = "abc5.txt"

        try {
            var fileInputStream: FileInputStream? = openFileInput(filename)
            var inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append("$text\n")
            }
            //Displaying data on EditText
            binding.textOutHistory.setText(stringBuilder.toString()).toString()
            binHistory = stringBuilder.toString()
        } catch (e: FileNotFoundException){
            val fileOutputStream: FileOutputStream
            fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(binHistory.toByteArray())
        }



    }

    fun readHistory() {
        val file= "abc5.txt"
        val data = binHistory
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
        } catch (e: FileNotFoundException){
            e.printStackTrace()
        }catch (e: NumberFormatException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun goCoords(view: View) {

        if (latitude != "") {
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

    }

    fun cleanData() {
        binding.textOutScheme.text = ""
        binding.textOutBrand.text = ""
        binding.textOutLength.text = ""
        binding.textOutLuhn.text = ""
        binding.textOutType.text = ""
        binding.textOutPrepaid.text = ""
        binding.textOutCountry.text = ""
        binding.textOutCoordinates.text = ""
        binding.textOutBank.text = ""
        binding.textOutHttp.text = ""
        binding.textOutPhone.text = ""
        bankUrl = ""
        latitude = ""
        longitude = ""
        phone = ""
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    fun buttonClick(view: View) {

        if (binding.editTextNumber.text.toString().isNotEmpty()) {
            binNumber = binding.editTextNumber.text.toString()

            binHistory = "$binNumber\n"+ binHistory
            //binding.textOutHistory.text = binHistory

            cleanData()
            readHistory()
            viewHistory()
            //binNumber = 45717360

            GlobalScope.launch(Dispatchers.IO) {
                val url = URL("https://lookup.binlist.net/$binNumber")
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.setRequestProperty(
                    "Accept",
                    "application/json"
                ) // The format of response we want to get from the server
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = false
                // Check if the connection is successful
                val responseCode = httpsURLConnection.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val response = httpsURLConnection.inputStream.bufferedReader()
                        .use { it.readText() }  // defaults to UTF-8
                    withContext(Dispatchers.Main) {

                        val jsonObject = JSONTokener(response).nextValue() as JSONObject

                        if (!jsonObject.isNull("number")) {
                            val number = jsonObject.getJSONObject("number")
                            val length = valueJsonIfExist("length", number)
                            val luhn = valueJsonIfExist("luhn", number)
                            binding.textOutLength.text = length
                            binding.textOutLuhn.text = when (luhn) {
                                "true" -> "Yes"
                                "false" -> "No"
                                else -> ""
                            }
                        }

                        val scheme = valueJsonIfExist("scheme", jsonObject)
                        val type = valueJsonIfExist("type", jsonObject)
                        val brand = valueJsonIfExist("brand", jsonObject)
                        val prepaid = valueJsonIfExist("prepaid", jsonObject)
                        binding.textOutScheme.text = scheme
                        binding.textOutType.text = type
                        binding.textOutBrand.text = brand
                        binding.textOutPrepaid.text = when (prepaid) {
                            "true" -> "Yes"
                            "false" -> "No"
                            else -> ""
                        }


                        if (!jsonObject.isNull("country")) {
                            val country = jsonObject.getJSONObject("country")
                            val numeric = valueJsonIfExist("numeric", country)
                            val alpha2 = valueJsonIfExist("alpha2", country)
                            val countryName = valueJsonIfExist("name", country)
                            val emoji = valueJsonIfExist("emoji", country)
                            val currency = valueJsonIfExist("currency", country)
                            latitude = valueJsonIfExist("latitude", country)
                            longitude = valueJsonIfExist("longitude", country)

                            binding.textOutCountry.text = "$emoji $countryName"
                            binding.textOutCoordinates.text =
                                "(latitude: $latitude, longitude: $longitude)"
                        }

                        if (!jsonObject.isNull("bank")) {
                            val bank = jsonObject.getJSONObject("bank")
                            val bankName = valueJsonIfExist("name", bank)
                            bankUrl = valueJsonIfExist("url", bank)
                            phone = valueJsonIfExist("phone", bank)
                            val city = valueJsonIfExist("city", bank)

                            binding.textOutBank.text = "$bankName, $city"
                            binding.textOutHttp.text = bankUrl
                            binding.textOutPhone.text = phone

                        }


                    }
                } else {
                    Log.e("HTTPURLCONNECTION_ERROR", responseCode.toString())
                }
            }

        }

    }



    fun valueJsonIfExist(jName: String,jObject: JSONObject) : String {
       return if (!jObject.isNull(jName)) jObject.getString(jName) else ""
    }



}