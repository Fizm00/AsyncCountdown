package com.example.asynctask;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private TextView resultTextView;
    private EditText countdownTimeEditText;
    private Button startTaskButton, cancelTaskButton, pauseTaskButton, resumeTaskButton;
    private ProgressBar progressBar;
    private LottieAnimationView lottieAnimationView;
    private CountdownTask countdownTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);
        countdownTimeEditText = findViewById(R.id.countdownTimeEditText);
        startTaskButton = findViewById(R.id.startTaskButton);
        cancelTaskButton = findViewById(R.id.cancelTaskButton);
        pauseTaskButton = findViewById(R.id.pauseTaskButton);
        resumeTaskButton = findViewById(R.id.resumeTaskButton);
        progressBar = findViewById(R.id.progressBar);
        lottieAnimationView = findViewById(R.id.lottieAnimationView);

        startTaskButton.setOnClickListener(v -> startCountdownTask());
        cancelTaskButton.setOnClickListener(v -> cancelCountdownTask());
        pauseTaskButton.setOnClickListener(v -> pauseCountdownTask());
        resumeTaskButton.setOnClickListener(v -> resumeCountdownTask());

        // Hide pause and resume buttons initially
        pauseTaskButton.setVisibility(View.GONE);
        resumeTaskButton.setVisibility(View.GONE);
    }

    private void startCountdownTask() {
        String inputTime = countdownTimeEditText.getText().toString().trim();
        if (inputTime.isEmpty()) {
            Snackbar.make(findViewById(R.id.main), "Please enter a countdown time!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        int countdownTime = Integer.parseInt(inputTime) * 1000;
        resultTextView.setText("Starting Countdown...");
        startTaskButton.setEnabled(false);
        cancelTaskButton.setVisibility(View.VISIBLE);
        pauseTaskButton.setVisibility(View.VISIBLE);
        resumeTaskButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        countdownTask = new CountdownTask();
        countdownTask.execute(countdownTime);
    }

    private void cancelCountdownTask() {
        if (countdownTask != null && countdownTask.getStatus() == AsyncTask.Status.RUNNING) {
            countdownTask.cancel(true);
            resultTextView.setText("Countdown Cancelled!");
            resetUI();
        }
    }

    private void pauseCountdownTask() {
        if (countdownTask != null) {
            countdownTask.pause();
            pauseTaskButton.setVisibility(View.GONE);
            resumeTaskButton.setVisibility(View.VISIBLE);
        }
    }

    private void resumeCountdownTask() {
        if (countdownTask != null) {
            countdownTask.resume();
            pauseTaskButton.setVisibility(View.VISIBLE);
            resumeTaskButton.setVisibility(View.GONE);
        }
    }

    private void resetUI() {
        startTaskButton.setEnabled(true);
        cancelTaskButton.setVisibility(View.GONE);
        pauseTaskButton.setVisibility(View.GONE);
        resumeTaskButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void triggerCompletionEffect() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();
    }

    private void showCompletionAnimation() {
        lottieAnimationView.setVisibility(View.VISIBLE);
        lottieAnimationView.playAnimation();
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }

    // AsyncTask Class for Countdown
    private class CountdownTask extends AsyncTask<Integer, Integer, Boolean> {
        private int totalTime; // Member variable to store the total countdown time
        private int remainingTime; // Member variable for remaining time
        private boolean isPaused = false; // Flag for pause state

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultTextView.setText("Countdown Started...");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            totalTime = params[0]; // Store the total countdown time
            remainingTime = totalTime; // Initialize remaining time
            int interval = 1000; // Update every second

            while (remainingTime > 0) {
                if (isCancelled()) {
                    return false; // Task was cancelled
                }

                if (isPaused) {
                    try {
                        // Wait while paused
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        return false;
                    }
                }

                publishProgress(remainingTime); // Update UI with remaining time
                remainingTime -= interval; // Decrease remaining time
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return true; // Countdown completed successfully
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = (int) (((float) values[0] / (float) totalTime) * 100);
            progressBar.setProgress(progress);
            resultTextView.setText("Remaining Time: " + (values[0] / 1000) + " seconds");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                resultTextView.setText("Countdown Finished!");
                triggerCompletionEffect();
                showCompletionAnimation();
                resetUI();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            resultTextView.setText("Countdown Cancelled!");
            resetUI();
        }

        // Pause the countdown
        public void pause() {
            isPaused = true;
        }

        // Resume the countdown
        public void resume() {
            isPaused = false;
            synchronized (this) {
                notify(); // Resume the countdown
            }
        }
    }
}
