package com.larryliang.arredecoration;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.larryliang.arredecorationunity.UnityPlayerActivity;

public class UnityActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, UnityPlayerActivity.class));
    }
}
