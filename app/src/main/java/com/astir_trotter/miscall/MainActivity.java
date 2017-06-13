package com.astir_trotter.miscall;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(new CustomPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void check(View view) {
        makeCall(((AppCompatEditText) findViewById(R.id.phonenumber)).getText().toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void makeCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "permission denied.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void endCall() {

    }


    class CustomPhoneStateListener extends PhoneStateListener {
        private SoundMeter soundMeter = new SoundMeter();
        private Handler handler = new Handler();
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (soundMeter.getAmplitude() > 0)
                    Log.d("CallState", "Connected");

                handler.postDelayed(runnable, 10);
            }
        };

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                Log.d("CallState", "off-hook");
                soundMeter.start();
                runnable.run();
            } else {
                Log.d("CallState", "Else");
                soundMeter.stop();
                handler.removeCallbacks(runnable);
            }
        }
    }

    public class SoundMeter {
        // This file is used to record voice
        static final private double EMA_FILTER = 0.6;

        private MediaRecorder mRecorder = null;
        private boolean mStarted;
        private double mEMA = 0.0;

        void start() {

            if (mRecorder == null) {

                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                mStarted = false;
                try {
                    mRecorder.prepare();
                    mRecorder.start();
                    mStarted = true;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mEMA = 0.0;
            }
        }

        void stop() {
            if (mRecorder != null & mStarted) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        }

        double getAmplitude() {
            if (mRecorder != null)
                return  (mRecorder.getMaxAmplitude()/2700.0);
            else
                return 0;

        }

        double getAmplitudeEMA() {
            double amp = getAmplitude();
            mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
            return mEMA;
        }
    }
}
