package com.example.photoswalldemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

/**
 * Created by jms on 2016/6/1.
 */
public class TestActivity extends AppCompatActivity {

    private ListView lv;
    private TestAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        lv= (ListView) findViewById(R.id.lv);

        mAdapter=new TestAdapter(Images.imageThumbUrls,R.layout.photo_layout,this,lv);

        lv.setAdapter(mAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.clearExecutor();
        mAdapter.clearHandler();
    }
}
