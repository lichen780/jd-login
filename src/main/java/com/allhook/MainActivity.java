package com.allhook;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("Xposed 模块已激活\n\n请在 Xposed 框架中勾选本模块和目标应用");
        textView.setPadding(50, 50, 50, 50);
        setContentView(textView);
    }
}
