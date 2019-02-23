package com.bignerdranch.android.criminalintent;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.PhotoViewAttacher;

import java.nio.file.Path;

public class ImageActivity extends AppCompatActivity {

    final static String TAG = "myLog";

    ImageView mImageView;
    PhotoViewAttacher mAttacher;
    String mPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);

        mImageView = (ImageView) findViewById(R.id.image_view);
        Bitmap bm = BitmapFactory.decodeFile(getIntent().getStringExtra("file_path"));
        mImageView.setImageBitmap(bm);

        mAttacher = new PhotoViewAttacher(mImageView);

    }
}
