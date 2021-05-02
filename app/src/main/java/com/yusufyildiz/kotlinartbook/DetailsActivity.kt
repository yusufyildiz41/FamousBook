package com.yusufyildiz.kotlinartbook

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_details.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.URI
import java.util.jar.Manifest

class DetailsActivity : AppCompatActivity() {
    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        var getIntent = intent
        var info = intent.getStringExtra("info")

        if(info.equals("new"))
        {
            artText2.setText("")
            artistText.setText("")
            yearText.setText("")
            saveButton.isVisible = true

            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.selectimage)
            imageView.setImageBitmap(selectedImageBackground)

        }
        else
        {
            saveButton.isInvisible = true
            val selectedId = intent.getIntExtra("id",1)

            val database = this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            var imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext())
            {
                artText2.setText(cursor.getString(artNameIx))
                artistText.setText(cursor.getString(artistNameIx))
                yearText.setText(cursor.getString(yearIx))
                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                imageView.setImageBitmap(bitmap)
            }
            cursor.close()

        }

    }
    fun save(view: View) {
        val artName = artText2.text.toString()
        val artistName = artistText.text.toString()
        val year = yearText.text.toString()
        if(selectedBitmap != null )
        {
            val smallBitmap = makeSmallerBitMap(selectedBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            selectedBitmap?.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val dataBase = this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)
                dataBase.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR ,artistname VARCHAR,year VARCHAR,image BLOB)")

                val sqlString ="INSERT INTO arts(artname, artistname, year, image) VALUES (?,?,?,?)"
                val statement = dataBase.compileStatement(sqlString)  // convert to sql code
                statement.bindString(1,artName) // bind is like adapter
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }
            catch (e:Exception)
            {
                e.printStackTrace()
            }
          //  finish() //it is close DetailsActivity
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) //The code is will make destroy that all open activities
            startActivity(intent)


        }
        else
        {
            val myMessage = Toast.makeText(applicationContext,"Please choose an image !!! ",Toast.LENGTH_LONG)
            myMessage.show()
        }



    }
    fun makeSmallerBitMap(image: Bitmap,maximumSize : Int) : Bitmap  //(input): output
    {
        // width = 200
        // height = 300
        var width = image.width
        var height = image.height

        val bitmapRatio : Double =width.toDouble()/height.toDouble()
        if(bitmapRatio>1) // that means the image is horizontal because width / height > 1
        {
            width = maximumSize
            height = (width/bitmapRatio).toInt()
        }
        else
        {
            height = maximumSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    fun selectImage(view: View)
    {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)

        }
        else
        {
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intentToGallery,2)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        if(requestCode == 1)
        {
            if (grantResults.size>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
            {
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intentToGallery,2)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if(requestCode ==2 && resultCode== RESULT_OK && data!=null)
        {
            selectedPicture = data.data

            try {

                if(selectedPicture != null)
                {

                    if (Build.VERSION.SDK_INT >= 20)
                    {
                        val source = ImageDecoder.createSource(this.contentResolver,selectedPicture!!)
                        selectedBitmap = ImageDecoder.decodeBitmap(source)
                        imageView.setImageBitmap(selectedBitmap)
                    }
                    else
                    {
                        selectedBitmap= MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPicture)
                        imageView.setImageBitmap(selectedBitmap)
                    }
                }

            }catch (e:Exception){

            }


        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}