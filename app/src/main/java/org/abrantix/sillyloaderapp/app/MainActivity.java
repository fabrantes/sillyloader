package org.abrantix.sillyloaderapp.app;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.ref.WeakReference;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SillyLoadingView v = (SillyLoadingView) findViewById(R.id.loading);
        v.start();

        Message msg = new Message();
        msg.obj = new WeakReference<MainActivity>(this);
        sHandler.sendMessageDelayed(msg, 5000);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sHandler.removeCallbacksAndMessages(null);
    }

    static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MainActivity a = ((WeakReference<MainActivity>) msg.obj).get();
            if (a == null) return;

            SillyLoadingView v = (SillyLoadingView) a.findViewById(R.id.loading);
            v.finish();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
