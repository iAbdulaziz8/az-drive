package com.azdrive.launcher;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  int bg=Color.rgb(7,15,28), panel=Color.rgb(15,31,51), panel2=Color.rgb(20,45,68), cyan=Color.rgb(0,183,232), white=Color.WHITE, muted=Color.rgb(156,180,199);
  Handler h=new Handler(Looper.getMainLooper()); ExecutorService io=Executors.newSingleThreadExecutor();
  TextView clock,date,temp,weather,next,countdown,prayerRows; LinearLayout root,mapHolder; WebView map;
  String provider="osm"; String[] prayerNames={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
  String[] prayerKeys={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"}; String[] prayerTimes={"--:--","--:--","--:--","--:--","--:--","--:--"};
  long nextAt=-1; String nextName="جاري تحميل المواقيت"; int accent; boolean light=false;
  Runnable tick=new Runnable(){public void run(){if(clock!=null){Calendar c=Calendar.getInstance();clock.setText(new SimpleDateFormat("HH:mm",Locale.US).format(c.getTime()));date.setText(new SimpleDateFormat("EEEE، d MMMM yyyy",new Locale("ar")).format(c.getTime()));if(nextAt>0){long d=Math.max(0,nextAt-System.currentTimeMillis());countdown.setText(String.format(Locale.US,"%02d:%02d:%02d",d/3600000,(d/60000)%60,(d/1000)%60));}}h.postDelayed(this,1000);}};
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);provider=getPreferences(0).getString("provider","osm");light=getPreferences(0).getBoolean("light",false);accent=getPreferences(0).getInt("accent",cyan);if(light){bg=Color.rgb(229,240,249);panel=Color.WHITE;panel2=Color.rgb(237,246,252);white=Color.rgb(20,39,56);muted=Color.rgb(79,103,123);}build();h.post(tick);refresh();}
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),light?Color.rgb(195,216,231):Color.rgb(37,67,91));return d;}
  TextView label(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(10),dp(8),dp(10),dp(8));l.setBackground(shape(panel,17));return l;}
  LinearLayout.LayoutParams lp(int w,int ht){return new LinearLayout.LayoutParams(w<0?w:dp(w),ht<0?ht:dp(ht));}
  Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setAllCaps(false);b.setTextColor(primary?(light?Color.WHITE:bg):white);b.setBackground(shape(primary?accent:panel2,13));return b;}
  void build(){
    root=new LinearLayout(this);root.setOrientation(0);root.setPadding(dp(9),dp(8),dp(9),dp(8));root.setBackgroundColor(bg);
    LinearLayout rail=new LinearLayout(this);rail.setOrientation(1);rail.setGravity(Gravity.CENTER);rail.setBackground(shape(panel,20));
    String[][] nav={{"AZ","الرئيسية"},{"⌖","الملاحة"},{"▦","التطبيقات"},{"♫","الوسائط"},{"⚙","الإعدادات"}};
    for(int i=0;i<nav.length;i++){final int n=i;TextView t=label(nav[i][0]+"\n"+nav[i][1],13,accent,true);t.setGravity(Gravity.CENTER);rail.addView(t,lp(64,72));t.setOnClickListener(v->{if(n==1)showMapOptions();else if(n==2)showApps();else if(n==3)launch("com.google.android.youtube");else if(n==4)customize();else build();});}
    root.addView(rail,lp(68,-1));
    LinearLayout main=new LinearLayout(this);main.setOrientation(0);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,-1,1);mp.setMargins(dp(8),0,0,0);root.addView(main,mp);
    LinearLayout left=card();left.addView(label("AZ Drive  •  الملاحة",17,white,true));TextView source=label("مصدر الخرائط: "+providerName()+"     ⚙",12,accent,true);left.addView(source,lp(-1,34));source.setOnClickListener(v->showMapOptions());
    mapHolder=new LinearLayout(this);mapHolder.setOrientation(1);mapHolder.setBackground(shape(panel2,12));left.addView(mapHolder,new LinearLayout.LayoutParams(-1,0,1));renderMap();
    Button nav=btn("ابدأ الملاحة إلى وجهة",true);nav.setOnClickListener(v->navigation());left.addView(nav,lp(-1,43));
    main.addView(left,new LinearLayout.LayoutParams(0,-1,1.35f));
    LinearLayout center=new LinearLayout(this);center.setOrientation(1);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);cp.setMargins(dp(7),0,dp(7),0);main.addView(center,cp);
    LinearLayout time=card();clock=label("--:--",42,white,true);clock.setGravity(Gravity.CENTER);date=label("",12,muted,false);date.setGravity(Gravity.CENTER);time.addView(clock,lp(-1,60));time.addView(date,lp(-1,25));center.addView(time,lp(-1,108));
    LinearLayout wc=card();wc.addView(label("الرياض • الطقس",15,white,true));temp=label("--°",32,white,true);weather=label("جاري تحديث الطقس",12,muted,false);wc.addView(temp);wc.addView(weather);LinearLayout.LayoutParams wp=lp(-1,105);wp.topMargin=dp(7);center.addView(wc,wp);
    LinearLayout apps=card();apps.addView(label("التطبيقات المفضلة",15,white,true));String[][] fav={{"خرائط","com.google.android.apps.maps"},{"YouTube","com.google.android.youtube"},{"Brave","com.brave.browser"},{"ثمانية","com.thmanyah.app"},{"Spotify","com.spotify.music"}};for(String[] a:fav){Button x=btn(a[0],false);x.setOnClickListener(v->launch(a[1]));apps.addView(x,lp(-1,35));}LinearLayout.LayoutParams ap=lp(-1,0);ap.weight=1;ap.topMargin=dp(7);center.addView(apps,ap);
    LinearLayout right=new LinearLayout(this);right.setOrientation(1);main.addView(right,new LinearLayout.LayoutParams(0,-1,1.0f));
    LinearLayout pr=card();pr.addView(label("مواقيت الصلاة • الرياض",17,white,true));prayerRows=label("جاري تحميل المواقيت...",13,white,false);pr.addView(prayerRows,new LinearLayout.LayoutParams(-1,0,1));LinearLayout.LayoutParams prp=lp(-1,0);prp.weight=1.2f;right.addView(pr,prp);
    LinearLayout nc=card();nc.addView(label("الصلاة القادمة: "+nextName,14,white,true));next=label(nextName,18,accent,true);countdown=label("--:--:--",28,white,true);nc.addView(next);nc.addView(countdown);LinearLayout.LayoutParams ncp=lp(-1,105);ncp.topMargin=dp(7);right.addView(nc,ncp);
    LinearLayout media=card();media.addView(label("الوسائط",16,white,true));media.addView(label("افتح تطبيق الوسائط المثبت على جهازك",11,muted,false));Button y=btn("YouTube",false);y.setOnClickListener(v->launch("com.google.android.youtube"));media.addView(y,lp(-1,40));Button t=btn("ثمانية / التطبيقات",true);t.setOnClickListener(v->showApps());media.addView(t,lp(-1,40));LinearLayout.LayoutParams medp=lp(-1,0);medp.weight=1;medp.topMargin=dp(7);right.addView(media,medp);
    setContentView(root);
  }
  String providerName(){return provider.equals("google")?"Google Maps":provider.equals("hudhud")?"هدهد":"OpenStreetMap";}
  void renderMap(){mapHolder.removeAllViews();if(provider.equals("google")){TextView info=label("Google Maps مختار\nافتح التطبيق لعرض الخريطة والملاحة",14,white,true);info.setGravity(Gravity.CENTER);mapHolder.addView(info,new LinearLayout.LayoutParams(-1,0,1));Button b=btn("فتح Google Maps",true);b.setOnClickListener(v->launch("com.google.android.apps.maps"));mapHolder.addView(b,lp(-1,44));}else if(provider.equals("hudhud")){TextView info=label("مصدر هدهد محدد\nيلزم تطبيق هدهد أو واجهة SDK رسمية لعرض الخريطة داخليًا",14,white,true);info.setGravity(Gravity.CENTER);mapHolder.addView(info,new LinearLayout.LayoutParams(-1,0,1));Button b=btn("البحث عن تطبيق هدهد",true);b.setOnClickListener(v->showApps());mapHolder.addView(b,lp(-1,44));}else{map=new WebView(this);map.setBackgroundColor(panel);map.getSettings().setJavaScriptEnabled(true);map.getSettings().setDomStorageEnabled(true);map.setWebViewClient(new WebViewClient());String html="<html><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><body style='margin:0'><iframe style='width:100%;height:100%;border:0' src='https://www.openstreetmap.org/export/embed.html?bbox=46.58%2C24.62%2C46.78%2C24.81&layer=mapnik&marker=24.7136%2C46.6753'></iframe></body></html>";map.loadDataWithBaseURL("https://www.openstreetmap.org",html,"text/html","UTF-8",null);mapHolder.addView(map,new LinearLayout.LayoutParams(-1,-1));}}
  void showMapOptions(){String[] opts={"OpenStreetMap (خريطة داخلية)","Google Maps (تطبيق وملاحة)","هدهد (يتطلب تكاملًا رسميًا)"};new AlertDialog.Builder(this).setTitle("مصدر الخرائط").setSingleChoiceItems(opts,provider.equals("osm")?0:provider.equals("google")?1:2,(d,w)->{provider=w==0?"osm":w==1?"google":"hudhud";getPreferences(0).edit().putString("provider",provider).apply();d.dismiss();build();}).setNegativeButton("إلغاء",null).show();}
  void navigation(){if(provider.equals("google")){Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("google.navigation:q=24.7136,46.6753"));i.setPackage("com.google.android.apps.maps");try{startActivity(i);}catch(Exception e){launch("com.google.android.apps.maps");}}else if(provider.equals("osm")){new AlertDialog.Builder(this).setTitle("بدء الملاحة").setMessage("سيتم فتح تطبيق ملاحة مثبت يدعم وجهة الخريطة. اختر تطبيقًا من القائمة التالية.").setPositiveButton("اختيار تطبيق",(d,w)->{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:24.7136,46.6753?q=24.7136,46.6753"));startActivity(Intent.createChooser(i,"الملاحة باستخدام"));}).setNegativeButton("إلغاء",null).show();}else showApps();}
  void refresh(){io.execute(()->{weatherFetch();prayerFetch();});h.postDelayed(this::refresh,30*60*1000L);}
  String get(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);in.close();c.disconnect();return o.toString("UTF-8");}
  void weatherFetch(){try{JSONObject j=new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=24.7136&longitude=46.6753&current=temperature_2m,weather_code&timezone=Asia%2FRiyadh")).getJSONObject("current");String t=Math.round(j.getDouble("temperature_2m"))+"°";String w=weatherText(j.optInt("weather_code"));runOnUiThread(()->{temp.setText(t);weather.setText(w);});}catch(Exception e){runOnUiThread(()->weather.setText("الطقس غير متاح"));}}
  String weatherText(int c){if(c==0)return"صحو";if(c<=3)return"غائم جزئيًا";if(c<=48)return"ضباب";if(c<=67)return"أمطار";if(c<=82)return"زخات مطر";if(c>=95)return"عواصف رعدية";return"غائم";}
  void prayerFetch(){try{String day=new SimpleDateFormat("dd-MM-yyyy",Locale.US).format(new Date());JSONObject data=new JSONObject(get("https://api.aladhan.com/v1/timingsByCity/"+day+"?city=Riyadh&country=Saudi%20Arabia&method=4")).getJSONObject("data");JSONObject tm=data.getJSONObject("timings");String[] vals=new String[6];for(int i=0;i<6;i++)vals[i]=tm.getString(prayerKeys[i]).substring(0,5);Calendar now=Calendar.getInstance();long chosen=-1;String nm="";for(int i=0;i<6;i++){if(i==1)continue;String[] a=vals[i].split(":");Calendar c=(Calendar)now.clone();c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(a[0]));c.set(Calendar.MINUTE,Integer.parseInt(a[1]));c.set(Calendar.SECOND,0);if(c.getTimeInMillis()>now.getTimeInMillis()){chosen=c.getTimeInMillis();nm=prayerNames[i];break;}}if(chosen<0){String[] a=vals[0].split(":");Calendar c=(Calendar)now.clone();c.add(Calendar.DATE,1);c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(a[0]));c.set(Calendar.MINUTE,Integer.parseInt(a[1]));chosen=c.getTimeInMillis();nm="الفجر";}final String rows=rows(vals);final long at=chosen;final String name=nm;runOnUiThread(()->{prayerTimes=vals;prayerRows.setText(rows);nextName=name;next.setText("الصلاة القادمة: "+name);nextAt=at;});}catch(Exception e){runOnUiThread(()->prayerRows.setText("تعذر تحميل المواقيت\nتحقق من اتصال الإنترنت"));}}
  String rows(String[] v){StringBuilder s=new StringBuilder();for(int i=0;i<6;i++)s.append(prayerNames[i]).append("     ").append(v[i]).append(i<5?"\n":"");return s.toString();}
  void launch(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null)startActivity(i);else showApps();}catch(Exception e){showApps();}}
  void showApps(){List<android.content.pm.ResolveInfo> list=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=list.get(i).loadLabel(getPackageManager()).toString();new AlertDialog.Builder(this).setTitle("التطبيقات المثبتة").setItems(names,(d,w)->{Intent i=getPackageManager().getLaunchIntentForPackage(list.get(w).activityInfo.packageName);if(i!=null)startActivity(i);}).setNegativeButton("إغلاق",null).show();}
  void customize(){String[] opts={"تبديل فاتح / داكن","لون أزرق سماوي","لون أخضر","لون بنفسجي","الخلفية الافتراضية","إعدادات النظام"};new AlertDialog.Builder(this).setTitle("تخصيص المظهر").setItems(opts,(d,w)->{if(w==0){light=!light;getPreferences(0).edit().putBoolean("light",light).apply();}else if(w==1||w==2||w==3){accent=w==1?Color.rgb(0,153,255):w==2?Color.rgb(0,190,120):Color.rgb(150,80,230);getPreferences(0).edit().putInt("accent",accent).apply();}else if(w==5){startActivity(new Intent(Settings.ACTION_SETTINGS));return;}build();}).setNegativeButton("إغلاق",null).show();}
  @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);}
  @Override protected void onDestroy(){super.onDestroy();h.removeCallbacksAndMessages(null);io.shutdownNow();if(map!=null)map.destroy();}
}
