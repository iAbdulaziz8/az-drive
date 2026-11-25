package com.azdrive.launcher;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;
import android.content.pm.ResolveInfo;

public class MainActivity extends Activity {
  private WebView web;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    hideSystemBars();
    getWindow().setStatusBarColor(Color.rgb(8,5,22));
    getWindow().setNavigationBarColor(Color.rgb(8,5,22));
    web = new WebView(this);
    web.setBackgroundColor(Color.rgb(8,5,22));
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setMediaPlaybackRequiresUserGesture(false);
    web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new Bridge(), "AZ");
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }
  private void hideSystemBars() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      getWindow().setDecorFitsSystemWindows(false);
      getWindow().getInsetsController().hide(
        android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars()
      );
      getWindow().getInsetsController().setSystemBarsBehavior(
        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      );
    } else {
      getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      );
    }
  }
  @Override public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) hideSystemBars();
  }
  public class Bridge {
    @JavascriptInterface public void openApp(String pkg) { runOnUiThread(()->launch(pkg)); }
    @JavascriptInterface public void showApps() { runOnUiThread(()->apps(false)); }
    @JavascriptInterface public void chooseApp() { runOnUiThread(()->apps(true)); }
    @JavascriptInterface public void toast(String msg) { runOnUiThread(()->Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show()); }
    @JavascriptInterface public String getDeviceName() { return android.os.Build.MODEL; }
  }
  private void launch(String pkg) {
    try { Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i!=null) startActivity(i); else apps(false); }
    catch(Exception e){ apps(false); }
  }
  private void apps(boolean choose) {
    List<ResolveInfo> list=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
    Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
    String[] names=new String[list.size()]; for(int i=0;i<list.size();i++) names[i]=list.get(i).loadLabel(getPackageManager()).toString();
    new AlertDialog.Builder(this).setTitle(choose?"إضافة تطبيق للاختصارات":"التطبيقات المثبتة").setItems(names,(d,n)->{
      ResolveInfo selected=list.get(n); String pkg=selected.activityInfo.packageName;
      if(choose) {
        String label=selected.loadLabel(getPackageManager()).toString().replace("\\","\\\\").replace("'","\\'");
        web.evaluateJavascript("onAppChosen('"+pkg+"','"+label+"')",null);
        Toast.makeText(this,"تمت إضافة الاختصار",Toast.LENGTH_SHORT).show();
      } else { Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null)startActivity(i); }
    }).setNegativeButton("إغلاق",null).show();
  }
  @Override public void onBackPressed(){ super.onBackPressed(); }
  @Override protected void onDestroy(){ if(web!=null)web.destroy(); super.onDestroy(); }
}
