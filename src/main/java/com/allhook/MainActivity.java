package com.allhook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Button;
import android.widget.LinearLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private TextView textView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isRefreshing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 10, 10);
        
        // 按钮布局
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button refreshBtn = new Button(this);
        refreshBtn.setText("🔄 刷新");
        refreshBtn.setOnClickListener(v -> refreshLog());
        
        Button clearBtn = new Button(this);
        clearBtn.setText("🗑️ 清除日志");
        clearBtn.setOnClickListener(v -> clearLog());
        
        buttonLayout.addView(refreshBtn);
        buttonLayout.addView(clearBtn);
        
        // 日志文本
        textView = new TextView(this);
        textView.setTextSize(10);
        textView.setPadding(20, 20, 20, 20);
        textView.setSingleLine(false);
        textView.setTextColor(0xFF000000);
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        
        layout.addView(buttonLayout);
        layout.addView(scrollView);
        setContentView(layout);
        
        // 自动刷新
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRefreshing) {
                    refreshLog();
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        isRefreshing = true;
        handler.post(refreshRunnable);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isRefreshing = false;
        handler.removeCallbacks(refreshRunnable);
    }
    
    private void refreshLog() {
        String log = getHookLog();
        textView.setText(log.isEmpty() ? "暂无日志\n\n请先使用目标 APP 触发检测" : log);
    }
    
    private String getHookLog() {
        StringBuilder log = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -s AllHook");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            log.append("读取日志失败：").append(e.getMessage());
        }
        return log.toString();
    }
    
    private void clearLog() {
        try {
            Runtime.getRuntime().exec("logcat -c");
            textView.setText("日志已清除");
        } catch (Exception e) {
            textView.setText("清除失败：" + e.getMessage());
        }
    }
}
