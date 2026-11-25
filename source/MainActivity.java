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
  public class Bridge {
    @JavascriptInterface public void openApp(String pkg) { runOnUiThread(()->launch(pkg)); }
    @JavascriptInterface public void showApps() { runOnUiThread(()->apps()); }
    @JavascriptInterface public void toast(String msg) { runOnUiThread(()->Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show()); }
    @JavascriptInterface public String getDeviceName() { return android.os.Build.MODEL; }
  }
  private void launch(String pkg) {
    try { Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i!=null) startActivity(i); else apps(); }
    catch(Exception e){ apps(); }
  }
  private void apps() {
    List<ResolveInfo> list=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
    Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
    String[] names=new String[list.size()]; for(int i=0;i<list.size();i++) names[i]=list.get(i).loadLabel(getPackageManager()).toString();
    new AlertDialog.Builder(this).setTitle("التطبيقات المثبتة").setItems(names,(d,n)->{Intent i=getPackageManager().getLaunchIntentForPackage(list.get(n).activityInfo.packageName);if(i!=null)startActivity(i);}).setNegativeButton("إغلاق",null).show();
  }
  @Override public void onBackPressed(){ super.onBackPressed(); }
  @Override protected void onDestroy(){ if(web!=null)web.destroy(); super.onDestroy(); }
}
