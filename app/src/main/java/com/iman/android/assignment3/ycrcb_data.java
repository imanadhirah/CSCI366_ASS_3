package com.iman.android.assignment3;

public class ycrcb_data {
    public byte[][] Y;
    public byte[][] Cr;
    public byte[][] Cb;

    public ycrcb_data(){
        Y = new byte[0][0];
        Cr = new byte[0][0];
        Cb = new byte[0][0];
    }

    public ycrcb_data(int width, int height){
        Y = new byte[width][height];
        Cr = new byte[width][height];
        Cb = new byte[width][height];
    }

    public ycrcb_data(int y_width, int y_height,int cr_width, int cr_height,int cb_width, int cb_height){
        Y = new byte[y_width][y_height];
        Cr = new byte[cr_width][cr_height];
        Cb = new byte[cb_width][cb_height];
    }


}
