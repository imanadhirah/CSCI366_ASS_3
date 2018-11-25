package com.iman.android.assignment3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    final int PICK_IMAGE_REQUEST = 111;
    boolean imageLoaded = false;
    ImageView imgView, comImgView;
    TextView tv1, tv2;
    Bitmap inputBM, outputBM;

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
                compressIman(inputBM, "file1.cmp");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void compressIman(Bitmap bm,String filename){

        int width = bm.getWidth();
        int height = bm.getHeight();
        int half_width = (int) Math.ceil(bm.getWidth()/2.0);
        int half_height = (int) Math.ceil(bm.getHeight()/2.0);

        Log.d("debug", "compressIman: "+ width +" " + height + " " + half_width + " " + half_height);
        ycrcb_data fulldata = new ycrcb_data(width, height);
        ycrcb_data Halfdata = new ycrcb_data(width, height,
                                            half_width, half_height,
                                            half_width, half_height);
        convertYcrcbFullSample(fulldata, bm);
        convertYcrcbSubSample(fulldata,Halfdata,bm);

        WriteToFileByte(Halfdata,filename,this); // .chs


    }

    public void decompressIman(View view)
    {

        String filename = "file1.cmp";
        ycrcb_data d = ReadToFileByte(filename, this);

        outputBM = Bitmap.createBitmap(inputBM.getWidth(), inputBM.getHeight(),inputBM.getConfig());

         convertRGB( outputBM,d);

         //View bm
        comImgView.setImageBitmap(outputBM);

    }

    public void convertRGB(Bitmap bm,ycrcb_data d)
    {
        Log.e("Debug", " start convertRGB: ");

        for (int i=0; i<bm.getWidth();i++)
        {
            int r=0,g=0,b=0;

            for (int j=0;j<bm.getHeight();j++)
            {
                float y = d.Y[i][j];
                float cr = d.Cr[i][j];
                float cb = d.Cb[i][j];


                if(y < 0){
                    y += 256;
                }


                r = (int)(y + 1.402 * cr) ;
                g = (int)(y - (0.3441 * cb ) - (0.714136 * cr) ) ;
                b = (int)(y + 1.772  * cb);

                bm.setPixel(i,j,Color.argb(255,r,g,b));

            }

        }

        Log.e("Debug", "Done: ");


    }
    public void convertYcrcbSubSample(ycrcb_data f, ycrcb_data h,Bitmap bm){
        int iS = 0;
        int jS = 0;
        boolean ib = false;
        boolean jb = false;

        for (int i=0; i<bm.getWidth();i++)
        {
            System.arraycopy(f.Y[i],0,h.Y[i],0,f.Y[i].length);

            if(i%2 == 0)
                ib = true;
            else
                ib = false;
            for (int j=0;j<bm.getHeight();j++)
            {
                if(j%2 == 0)
                    jb = true;
                else
                    jb = false;

                if(ib && jb)
                {
                    double temp=0;
                    double c = 1;

                    temp = f.Cb[i][j];
                    if(i+1 < f.Cb.length) {
                        temp += f.Cb[i + 1][j];
                        c++;
                    }

                    if(j+1 < f.Cb[0].length) {
                        temp += f.Cb[i][j + 1];
                        c++;
                    }

                    if(i+1 < f.Cb.length && j+1 < f.Cb[0].length) {
                        temp += f.Cb[i + 1][j + 1];
                        c++;
                    }

                    h.Cb[iS][jS] = (byte)(temp / c);

                    c = 1;
                    temp = f.Cr[i][j];
                    if(i+1 < f.Cr.length) {
                        temp += f.Cr[i + 1][j];
                        c++;
                    }

                    if(j+1 < f.Cr[0].length) {
                        temp += f.Cr[i][j + 1];
                        c++;
                    }

                    if(i+1 < f.Cr.length && j+1 < f.Cr[0].length) {
                        temp += f.Cr[i + 1][j + 1];
                        c++;
                    }

                    h.Cr[iS][jS] = (byte)(temp / c);

                    jS++;
                }
            }
            jS=0;

            if (ib)
                iS++;
        }

    }

    public void convertYcrcbFullSample(ycrcb_data f,Bitmap bm){

        for (int i=0; i<bm.getWidth();i++)
        {

            for (int j=0;j<bm.getHeight();j++)
            {

                int p = bm.getPixel(i,j);
                float r = (float) Color.red(p);
                float g = (float) Color.green(p);
                float b = (float)Color.blue(p);

                f.Y[i][j] = (byte) ( 0.299 * r + 0.587* g + 0.114* b);
                f.Cb[i][j] = (byte)((-0.169 * r) + (-0.331)* g + 0.500 * b) ;
                f.Cr[i][j] = (byte)(( 0.500 * r) + (-0.419) * g +(-0.081) * b);

            }
        }

    }

    public void WriteToFileByte(ycrcb_data h, String filename, Context c)
    {
        try {
            FileOutputStream outputStreamWriter = new FileOutputStream(c.openFileOutput(filename + ".chs", Context.MODE_PRIVATE).getFD());
            String text = String.valueOf(h.Y.length)+" "+String.valueOf(h.Y[0].length)+"\n";
            outputStreamWriter.write(text.getBytes());
            text = String.valueOf(h.Cr.length)+" "+String.valueOf(h.Cr[0].length)+"\n";
            outputStreamWriter.write(text.getBytes());
            text = String.valueOf(h.Cb.length)+" "+String.valueOf(h.Cb[0].length)+"\n";
            outputStreamWriter.write(text.getBytes());

            for(int i=0; i < h.Y.length; i++){
                outputStreamWriter.write(h.Y[i]);
            }

            for(int i=0; i < h.Cr.length; i++){
                outputStreamWriter.write(h.Cr[i]);
            }

            for(int i=0; i < h.Cb.length; i++){
                outputStreamWriter.write(h.Cb[i]);
            }

            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        long length = new File(getFilesDir().getAbsolutePath() + "/"+filename+".chs").length();
        float size = Float.valueOf(length) / 1000;

        tv2.setText("filesize " + size +" Kb");
        Log.e("Debug", "filesize " + size +" Kb");
    }

    public ycrcb_data ReadToFileByte( String filename, Context c)
    {

        try{
            FileInputStream in = new FileInputStream(c.openFileInput(filename + ".chs").getFD());
            String line = null;
            String[]line_t;

            ycrcb_data d = new ycrcb_data();

            byte[] cc = new byte[1];
            int index = 0;
            String[] tempText = new String[3];
            for (int i = 0; i < 3; i++)
            {
                tempText[i] = "";

            }
            while( ( in.read(cc)) != -1){

                if((char)cc[0] == '\n')
                {
                    index++;
                    if (index == 3)
                        break;
                }
                else
                {
                    tempText[index] += (char)cc[0];
                }
            }

            line_t = tempText[0].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Y" + line_t[0] + " " + line_t[1] );
            int Y_width = Integer.valueOf(line_t[0]);
            int Y_height = Integer.valueOf(line_t[1]);

            line_t = tempText[1].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Cr" + line_t[0] + " " + line_t[1] );
            int Cr_width = Integer.valueOf(line_t[0]);
            int Cr_height = Integer.valueOf(line_t[1]);

            line_t = tempText[2].split(" ");
            Log.d("DEBUG", "ReadToFileByte: size of Cb" + line_t[0] + " " + line_t[1] );
            int Cb_width = Integer.valueOf(line_t[0]);
            int Cb_height = Integer.valueOf(line_t[1]);

            byte[] temp1 = new byte[Cb_height];
            d = new ycrcb_data(Y_width, Y_height);
            for(int i=0; i < d.Y.length; i++){
                in.read(d.Y[i]);

            }

            for(int i=0; i < d.Cr.length; i=i+2){
                in.read(temp1);
                int ind=0;
                for(int j=0; j< d.Cr[0].length;j=j+2){
                    d.Cr[i][j] = temp1[ind];

                    if(i+1 < d.Cr.length)
                        d.Cr[i+1][j] = d.Cr[i][j];

                    if(j+1 < d.Cr[0].length)
                        d.Cr[i][j+1] = d.Cr[i][j];

                    if(i+1 < d.Cr.length && j+1 < d.Cr[0].length)
                        d.Cr[i+1][j+1] = d.Cr[i][j];
                    ind++;
                }
            }

            for(int i=0; i < d.Cb.length; i=i+2){
                in.read(temp1);
                int ind=0;
                for(int j=0; j< d.Cb[0].length;j=j+2){
                    d.Cb[i][j] = temp1[ind];

                    if(i+1 < d.Cb.length)
                        d.Cb[i+1][j] = d.Cb[i][j];

                    if(j+1 < d.Cb[0].length)
                        d.Cb[i][j+1] = d.Cb[i][j];

                    if(i+1 < d.Cb.length && j+1 < d.Cb[0].length)
                        d.Cb[i+1][j+1] = d.Cb[i][j];

                    ind++;
                }
            }
            return d;
        }

        catch(IOException e){
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return null;
    }

}
