package com.example.hazem.timetracker;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 100;

    LinearLayout v;
    LinearLayout.LayoutParams params;
    LinearLayout.LayoutParams buttonParams;
    StringBuilder startTimeString;
    List<UsageStats> lUsageStatsList;
    UsageStatsManager lUsageStatsManager;
    SimpleDateFormat sdf;
    GregorianCalendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        v = (LinearLayout) findViewById(R.id.layout);
        v.setOrientation(LinearLayout.VERTICAL);

        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 8, 8);

        buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(8, 8, 8, 8);

        startTimeString = new StringBuilder();

        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);


        calendar = new GregorianCalendar(TimeZone.getTimeZone("US/Central"));


        try {
            fillStats();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


    }


    @Override
    protected void onResume() {
        super.onResume();

        v.removeAllViews();
        try {
            fillStats();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    public void fillStats() throws PackageManager.NameNotFoundException {
        if (hasPermission()){
            getStats();

        }else{
            requestPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "resultCode " + resultCode);
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS:
                try {
                    fillStats();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void requestPermission() {
        Toast.makeText(this, "Need to request permission", Toast.LENGTH_SHORT).show();
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
    }

    private boolean hasPermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;

    }

    @SuppressLint("InlinedApi")
    private void getStats() throws PackageManager.NameNotFoundException {
        lUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        lUsageStatsList = lUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

        Collections.sort(lUsageStatsList, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats lhs, UsageStats rhs) {
                return lhs.getLastTimeUsed()<rhs.getLastTimeUsed()?1:lhs.getLastTimeUsed()>rhs.getLastTimeUsed()?-1:0;
            }
        });

        StringBuilder lStringBuilder = new StringBuilder();

        for (UsageStats lUsageStats:lUsageStatsList){
           LinearLayout layout = new LinearLayout(this);

            // App Icon
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(getPackageManager().getApplicationIcon(lUsageStats.getPackageName()));
            icon.setLayoutParams(params);
            icon.getLayoutParams().height = 150;
            icon.getLayoutParams().width = 150;
            layout.addView(icon);
            TextView data = new TextView(this);

            // Get App name.
            final PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(lUsageStats.getPackageName(), 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
            lStringBuilder.append(applicationName);

            Button details = new Button(this);
            details.setText(">");
            details.setTag(lUsageStatsList.indexOf(lUsageStats));
            details.setLayoutParams(buttonParams);
            details.setTextSize(15);
            details.setMaxWidth(30);
            details.setMaxHeight(10);

            details.setOnClickListener(this);

            lStringBuilder.append("\nLast time used: ");
            calendar.setTimeInMillis(lUsageStats.getLastTimeUsed());
            lStringBuilder.append(sdf.format(calendar.getTime()));
            lStringBuilder.append("\nTotal time used today: ");
            String totalTime = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(lUsageStats.getTotalTimeInForeground()),
                    TimeUnit.MILLISECONDS.toMinutes(lUsageStats.getTotalTimeInForeground()) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lUsageStats.getTotalTimeInForeground())),
                    TimeUnit.MILLISECONDS.toSeconds(lUsageStats.getTotalTimeInForeground()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lUsageStats.getTotalTimeInForeground())));
            lStringBuilder.append(totalTime);
            lStringBuilder.append("\r\n");
            
            data.setText(lStringBuilder.toString());
            layout.addView(data);
            layout.addView(details);
            lStringBuilder.setLength(0);
            v.addView(layout);
        }
    }

    @Override
    public void onClick(View v) {
        UsageStats s = lUsageStatsList.get((int) v.getTag());
        // Event Query
        UsageEvents events = lUsageStatsManager.queryEvents(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

        while (events.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            events.getNextEvent(event);
            if (event.getPackageName().equals(s.getPackageName()) && event.getEventType() == 1) {
                long startTime = event.getTimeStamp();
                startTimeString.append("\nStart time used: ");
                calendar.setTimeInMillis(startTime);
                startTimeString.append(sdf.format(calendar.getTime()));
                startTimeString.append("\n");
            }
        }

        Intent i = new Intent(this, DetailsActivity.class);
        Bundle b = new Bundle();
        b.putString("details", startTimeString.toString());
        startTimeString.setLength(0);
        i.putExtras(b);
        startActivity(i);
    }
}