package com.nvllz.piggsy.ui.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import com.nvllz.piggsy.ui.ComposeAboutView;

public class AboutActivity extends BaseActivity {

    private ComposeAboutView aboutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        aboutView = new ComposeAboutView(this);
        setContentView(aboutView);
    }
}
