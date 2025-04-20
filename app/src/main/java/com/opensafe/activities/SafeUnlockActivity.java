package com.opensafe.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.opensafe.R;

public class SafeUnlockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_unlock);

        // Use Glide to display the GIF of the safe opening
        ImageView gifImageView = findViewById(R.id.safe_gif_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.safe_unlock_gif) // Assumes you have a GIF resource in drawable folder
                .into(gifImageView);
    }
}
