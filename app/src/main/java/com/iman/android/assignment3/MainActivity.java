package com.iman.android.assignment3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    final int PICK_IMAGE_REQUEST = 111;
    boolean imageLoaded = false;
    ImageView imgView, comImgView;
    TextView tv1, tv2;
    Bitmap inputBM, outputBM, tempBM;
    BufferedImage ycb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1 = (TextView) findViewById(R.id.title1);
        tv2 = (TextView) findViewById(R.id.title2);
        imgView = (ImageView) findViewById(R.id.uncompressed_image);
        comImgView = (ImageView) findViewById(R.id.compressed_image);
    }

    public void loadImage(View view) {
        Intent i = new Intent();
        // Show only images, no videos or anything else
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(i, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();
            InputStream inputFile;
            float fileSize = 0;
            try {
                inputFile = getContentResolver().openInputStream(uri);
                //get file size and convert to kB
                fileSize = (float)inputFile.available() / 1024;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //get last part of path (file name)
            int cut = uri.getLastPathSegment().lastIndexOf('/');
            tv1.setText(uri.getLastPathSegment().substring(cut+1) + " - " + String.format("%.02f",fileSize) + " kB");
            try {
                inputBM = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imgView.setImageBitmap(inputBM);
                imageLoaded = true;
                compress(inputBM, "file1.cmp");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
//
//    public void playOriClick(View v) {
//        //code to check if this checkbox is checked!
//        CheckBox checkBox = (CheckBox) findViewById(R.id.compress);
//        if(checkBox.isChecked()){
//            checkBox.toggle();
//        }
//    }
//
//    public void playDeNoiseClick(View v) {
//        //code to check if this checkbox is checked!
//        CheckBox checkBox = (CheckBox) findViewById(R.id.decompress);
//        if(checkBox.isChecked()){
//            checkBox.toggle();
//        }
//    }

    private void compressIman(Bitmap bm,String filename){
        byte[][] Y = new byte[bm.getWidth()][bm.getHeight()];
        byte[][] Cr = new byte[(bm.getWidth()/2)][(bm.getHeight()/2)];
        byte[][] Cb = new byte[(bm.getWidth()/2)][(bm.getHeight()/2)];

        convertYcrcbSubSample(Y,Cr,Cb,bm);

        WriteToFileByte(Y,Cr,Cb,filename); // .chs
    }

    private void decompress(String filename)
    {
        byte[][] Y, Cr, Cb;

        ReadToFileByte(Y,Cr,Cb,filename);



        Bitmap bm = new Bitmap();


         convertRGB( bm,Y,  Cr, Cb);

         //View bm

    }

    //This compress method process pixels in 3x3 block
    //It uses the concept of Color Filter Array as in the camera sensor
    //Each pixel only keep 1 color value (either R or G or B)
    //The other 2 colors obtain from interpolation of the neigboring pixels
    private void compress(Bitmap bm, String fileName) {
        int width = bm.getWidth(); //must store width
        int height = bm.getHeight(); //must store height
        int horiz = perfect3 (width);
        int vert = perfect3(height);

        byte[] compressedPixel = new byte[9];
        byte[] header = new byte[4];
        int index;
        FileOutputStream compressedFile;  //convert height & width in to byte
        try {
            compressedFile = openFileOutput(fileName, MODE_PRIVATE);
            try {   //convert width (32-bit integer) to 4-byte
                header[0] = (byte) (width & 0xFF);
                header[1] = (byte) (width >> 8 & 0xFF); //right shift by 8 bit
                header[2] = (byte) (width >> 16 & 0xFF);
                header[3] = (byte) (width >> 24 & 0xFF);
                compressedFile.write(header);
                header[0] = (byte) (height & 0xFF);
                header[1] = (byte) (height >> 8 & 0xFF);
                header[2] = (byte) (height >> 16 & 0xFF);
                header[3] = (byte) (height >> 24 & 0xFF);

                compressedFile.write(header);

                //store actual pixel
                //create a temporary Bitmap that has the width & height in multiple of 3
                tempBM = Bitmap.createScaledBitmap(inputBM, horiz, vert, false);

                for (int x = 0; x < horiz; x+=3) {
                    for (int y = 0; y < vert; y+=3) {
                        for (int m=0; m<3; m++) {
                            for (int n=0; n<3; n++) {
                                index = m*3 + n;
                                //for pixel 0, 4, 8, keep only the red channel
                                if ((index == 0) || (index == 4) || (index == 8))
                                    compressedPixel[index] = (byte) Color.red(tempBM.getPixel(x+m,y+n));
                                    //for pixel 2, 6, keep only the blue channel
                                else if ((index == 2) || (index == 6))
                                    compressedPixel[index] = (byte) Color.blue(tempBM.getPixel(x+m,y+n));
                                    //for pixel 1, 3, 5, 7, keep only the green channel
                                else
                                    compressedPixel[index] = (byte) Color.green(tempBM.getPixel(x+m,y+n));

                            }
                        }

                        compressedFile.write(compressedPixel);

                    }
                }
                compressedFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    //Convert width or height to multiple of 3
    // (do divide perfectly to 3x3) enlarge image by 1 or 2 pixel
    private int perfect3(int num) {
        if (num % 3 == 0)
            return num;
        else if (num % 3 == 1)
            return num + 2;
        else
            return num + 1;
    }

    public void convertRGB(Bitmap bm,byte[][] Y, byte[][] Cr, byte[][] Cb)
    {
        //R = Y + 1.402 * Cr
        //G = Y - 0.3441*Cb - 0.714136*Cr
        //B = Y + 1.772*Cb

        for (int i=0; i<bm.getHeight();i++)
        {

            for (int j=0;j<bm.getWidth();j++)
            {
                byte r,g,b;

                r = (byte) (Y[i][j] + 1.402 * Cr[i][j]);
                g = (byte) (Y[i][j] - 0.3441*Cb[i][j] - 0.714136*Cr[i][j]);
                b = (byte) (Y[i][j] + 1.772*Cb[i][j]);

                bm.setPixel(i,j,Color.rgb(r,g,b));
            }

        }
    }

    public void convertYcrcbSubSample(byte[][] Y, byte[][] Cr, byte[][] Cb,Bitmap bm){
        int ind = 0;
        int iS = 0;
        int jS = 0;
        boolean ib = false;
        boolean jb = false;
        for (int i=0; i<bm.getHeight();i++)
        {
            if(i%2 == 0)
                ib = true;
            else
                ib = false;
            for (int j=0;j<bm.getWidth();j++)
            {
                if(j%2 == 0)
                    jb = true;
                else
                    jb = false;

                int p = bm.getPixel(i,j);
                byte r = (byte) Color.red(p);
                byte g = (byte) Color.green(p);
                byte b = (byte)Color.blue(p);

                Y[i][j] = (byte) ( 0.299 * r + 0.587* g + 0.114* b);
                if(ib && jb)
                {
                    Cr[iS][jS] = (byte)( (-0.169) * r + (-0.331)* g + 0.500 * b);
                    Cb[iS][jS] = (byte) ( 0.500 * r + (-0.419) * g +(-0.081) * b);
                    jS++;
                }

                ind++;
            }
            if (ib)
                iS++;
        }

    }

    public void WriteToFileByte(byte[][] Y,byte[][]Cr,byte[][]Cb, String filename)
    {

    }

    public void ReadToFileByte(byte[][] Y,byte[][]Cr,byte[][]Cb, String filename)
    {

    }
    //The inverse of the compress method above
    public void decompress (View view) {

        FileInputStream compressedFile;
        int width = 0, height = 0;
        byte[] header = new byte[4];
        byte[] compressedPixel = new byte[9];
        int pixel;
        int red, green, blue, alpha = 255;
        int index;

        float fileSize = 0;
        try {
            //by default 'file.cmp'  will save in application folder.
            compressedFile = openFileInput("file1.cmp");
            try {
                fileSize = (float)compressedFile.available() / 1024;
                compressedFile.read(header);

                //convert back from byte to integer
                //merge & convert the first 4-byte of the file to integer (width)
                width = byteToInt(header[3]) << 24 | byteToInt(header[2]) << 16 | byteToInt(header[1]) << 8 | byteToInt(header[0]);
                compressedFile.read(header);
                //merge & convert the second 4-byte of the file to integer (height)
                height = byteToInt(header[3]) << 24 | byteToInt(header[2]) << 16 | byteToInt(header[1]) << 8 | byteToInt(header[0]);

                int horiz = perfect3(width);
                int vert = perfect3(height);
                //outputBM - the Bitmap that store the decompress pixels
                outputBM = Bitmap.createBitmap(horiz, vert, tempBM.getConfig());

                for (int x = 0; x < horiz; x+=3) {
                    for (int y = 0; y < vert; y+=3) {
                        compressedFile.read(compressedPixel);
                        for (int m=0; m<3; m++) {
                            for (int n=0; n<3; n++) {
                                index = m*3 + n;
                                //The interpolation calculation of each pixel in 3x3 region
                                if (index == 0) {
                                    red = byteToInt(compressedPixel[0]);
                                    green = (byteToInt(compressedPixel[1]) + byteToInt(compressedPixel[3]))/2;
                                    blue = (byteToInt(compressedPixel[2]) + byteToInt(compressedPixel[6]))/2;
                                }
                                else if (index == 1) {
                                    red = (byteToInt(compressedPixel[0]) + byteToInt(compressedPixel[4])) / 2;
                                    green = byteToInt(compressedPixel[1]);
                                    blue = 2*byteToInt(compressedPixel[2])/3 + byteToInt(compressedPixel[6])/3;
                                }
                                else if (index == 2) {
                                    red = byteToInt(compressedPixel[4]);
                                    green = (byteToInt(compressedPixel[1]) + byteToInt(compressedPixel[5]))/2;
                                    blue = byteToInt(compressedPixel[2]);
                                }
                                else if (index == 3) {
                                    red = (byteToInt(compressedPixel[0]) + byteToInt(compressedPixel[4])) / 2;
                                    green = byteToInt(compressedPixel[3]);
                                    blue = 2*byteToInt(compressedPixel[6])/3 + byteToInt(compressedPixel[2])/3;
                                }
                                else if (index == 4) {
                                    red = byteToInt(compressedPixel[4]);
                                    green = (byteToInt(compressedPixel[1]) + byteToInt(compressedPixel[3]) + byteToInt(compressedPixel[5]) + byteToInt(compressedPixel[7]))/4;
                                    blue = (byteToInt(compressedPixel[2]) + byteToInt(compressedPixel[6]))/2;
                                }
                                else if (index == 5) {
                                    red = (byteToInt(compressedPixel[4]) + byteToInt(compressedPixel[8])) / 2;
                                    green = byteToInt(compressedPixel[5]);
                                    blue = 2*byteToInt(compressedPixel[2])/3 + byteToInt(compressedPixel[6])/3;
                                }
                                else if (index == 6) {
                                    red = byteToInt(compressedPixel[4]);
                                    green = (byteToInt(compressedPixel[3]) + byteToInt(compressedPixel[7]))/2;
                                    blue = byteToInt(compressedPixel[6]);
                                }
                                else if (index == 7) {
                                    red = (byteToInt(compressedPixel[4]) + byteToInt(compressedPixel[8])) / 2;
                                    green = byteToInt(compressedPixel[7]);
                                    blue = 2*byteToInt(compressedPixel[6])/3 + byteToInt(compressedPixel[2])/3;
                                }
                                else {
                                    red = byteToInt(compressedPixel[8]);
                                    green = (byteToInt(compressedPixel[5]) + byteToInt(compressedPixel[7]))/2;
                                    blue = (byteToInt(compressedPixel[2]) + byteToInt(compressedPixel[6]))/2;
                                }
                                //set alpha to fix value.
                                pixel = Color.argb(alpha,red,green,blue);
                                outputBM.setPixel(x+m,y+n, pixel);
                            }
                        }

                    }
                }

                compressedFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            comImgView.setImageBitmap(outputBM);
            tv2.setText("file1.cmp" + " - " + String.format("%.02f",fileSize) + " kB");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    //Convert byte (8-bit) data to integer
    private int byteToInt(byte b) {
        return b & 0xFF;
    }
}
