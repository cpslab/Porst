package com.munisystem.porst;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.munisystem.porst.fragments.MainFragment;

public class MainActivity extends AppCompatActivity implements MainFragment.OnMainFragmentInteractionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commit();
    }

    @Override
    public void onCardItemClick(View v) {
        String text = "";
        switch (v.getId()) {
            case R.id.card_rental: {
                text = "貸出";
                break;
            }
            case R.id.card_return: {
                text = "返却";
                break;
            }
            case R.id.card_status: {
                text = "状態";
                break;
            }
        }
        Log.d(TAG, "onCardItemClick: " + text + " が押されました。");
    }
}
