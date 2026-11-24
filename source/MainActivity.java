package com.azdrive.launcher;

import android.app.*;
import android.content.*;
import android.graphics.*;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  int BG=Color.rgb(8,15,27), PANEL=Color.rgb(17,30,48), PANEL2=Color.rgb(23,42,64), CYAN=Color.rgb(0,174,220), WHITE=Color.rgb(238,245,251), MUTED=Color.rgb(151,174,196);
  Handler h=new Handler(Looper.getMainLooper()); ExecutorService io=Executors.newSingleThreadExecutor();
  TextView clock,date,temp,weather,nextName,countdown; LinearLayout prayers,appRow,mapHost; WebView map;
  String provider="osm"; String[] prayerNames={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
  String[] prayerKeys={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"}; String[] prayerTimes=new String[6];
  long nextAt=-1; int accent=0; boolean dark=true;
  Runnable ticker=new Runnable(){public void run(){Calendar c=Calendar.getInstance();clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(c.getTime()));date.setText(new SimpleDateFormat("EEEE، d MMMM yyyy",new Locale("ar")).format(c.getTime()));if(nextAt>0){long d=Math.max(0,nextAt-System.currentTimeMillis());countdown.setText(String.format(Locale.getDefault(),"%02d:%02d:%02d",d/3600000,(d/60000)%60,(d/1000)%60));}h.postDelayed(this,1000);}};
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);provider=getPreferences(0).getString("provider","osm");accent=getPreferences(0).getInt("accent",0);dark=getPreferences(0).getBoolean("dark",true);render();h.post(ticker);loadLive();}
  int accentColor(){int[] a={Color.rgb(0,174,220),Color.rgb(30,136,229),Color.rgb(0,190,140),Color.rgb(155,92,230),Color.rgb(255,140,50),Color.rgb(230,70,100)};return a[Math.max(0,Math.min(accent,a.length-1))];}
  int panelColor(){return dark?PANEL:Color.rgb(245,249,253);} int bgColor(){return dark?BG:Color.rgb(225,237,248);} int fg(){return dark?WHITE:Color.rgb(22,38,55);} int muted(){return dark?MUTED:Color.rgb(91,112,132);}
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),dark?Color.rgb(43,66,91):Color.rgb(195,215,234));return d;}
  TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(fg());t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,1);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  LinearLayout card(){LinearLayout l=col();l.setPadding(dp(12),dp(9),dp(12),dp(9));l.setBackground(shape(panelColor(),17));return l;}
  void gap(LinearLayout p,int w,int height){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(dp(w),dp(height)));}
  Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:fg());b.setBackground(shape(primary?accentColor():PANEL2,12));return b;}
  void render(){
    LinearLayout root=row();root.setPadding(dp(10),dp(8),dp(10),dp(8));root.setBackgroundColor(bgColor());
    LinearLayout rail=col();rail.setGravity(Gravity.CENTER);rail.setPadding(dp(4),dp(8),dp(4),dp(8));rail.setBackground(shape(panelColor(),18));
    rail.addView(txt("AZ",23,true));rail.addView(txt("Drive",11,true));
    String[][] nav={{"⌂","الرئيسية"},{"⌖","الملاحة"},{"♫","الوسائط"},{"▦","التطبيقات"},{"⚙","الإعدادات"}};
    for(int i=0;i<nav.length;i++){final int n=i;TextView x=txt(nav[i][0]+"\n"+nav[i][1],12,true);x.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(68),0,1);lp.setMargins(0,dp(4),0,dp(4));rail.addView(x,lp);x.setOnClickListener(v->{if(n==3)showApps();else if(n==4)customize();else if(n==1)destination();else if(n==2)launch("com.google.android.youtube.music");else render();});}
    root.addView(rail,new LinearLayout.LayoutParams(dp(76),-1));gap(root,8,1);
    LinearLayout center=col();root.addView(center,new LinearLayout.LayoutParams(0,-1,1));
    LinearLayout status=row();status.addView(txt("AZ Drive  •  5G  •  Wi-Fi  •  Bluetooth",12,true),new LinearLayout.LayoutParams(0,dp(30),1));TextView gear=txt("⚙",23,true);gear.setGravity(Gravity.CENTER);status.addView(gear,new LinearLayout.LayoutParams(dp(40),dp(30)));gear.setOnClickListener(v->customize());center.addView(status);
    LinearLayout body=row();center.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout mapCard=card();mapCard.addView(txt("الملاحة  •  مصدر الخرائط: "+providerName(),16,true));map=new WebView(this);map.setBackgroundColor(panelColor());map.getSettings().setJavaScriptEnabled(true);map.getSettings().setDomStorageEnabled(true);map.setWebViewClient(new WebViewClient());String html="<html><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><body style='margin:0'><iframe style='width:100%;height:100%;border:0' src='https://www.openstreetmap.org/export/embed.html?bbox=46.58%2C24.62%2C46.76%2C24.80&layer=mapnik&marker=24.7136%2C46.6753'></iframe></body></html>";map.loadDataWithBaseURL("https://www.openstreetmap.org",html,"text/html","UTF-8",null);mapCard.addView(map,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout mapActions=row();Button source=btn("تغيير المصدر",false);source.setOnClickListener(v->selectProvider());Button navButton=btn("ابدأ الملاحة",true);navButton.setOnClickListener(v->destination());mapActions.addView(source,new LinearLayout.LayoutParams(0,dp(44),1));gap(mapActions,6,1);mapActions.addView(navButton,new LinearLayout.LayoutParams(0,dp(44),1));mapCard.addView(mapActions);body.addView(mapCard,new LinearLayout.LayoutParams(0,-1,1.25f));gap(body,8,1);
    LinearLayout middle=col();body.addView(middle,new LinearLayout.LayoutParams(0,-1,1));
    LinearLayout time=card();time.setGravity(Gravity.CENTER);clock=txt("--:--",43,true);clock.setGravity(Gravity.CENTER);date=txt("",13,false);date.setGravity(Gravity.CENTER);time.addView(date);time.addView(clock);TextView analog=txt("◷  AZ",22,true);analog.setGravity(Gravity.CENTER);time.addView(analog);middle.addView(time,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout wx=card();wx.addView(txt("الرياض  •  الطقس",15,true));LinearLayout wr=row();temp=txt("--°",34,true);weather=txt("جاري تحديث الطقس",13,false);wr.addView(temp);gap(wr,12,1);wr.addView(weather,new LinearLayout.LayoutParams(0,-2,1));wx.addView(wr);middle.addView(wx,new LinearLayout.LayoutParams(-1,0,1));
    gap(body,8,1);
    LinearLayout right=col();body.addView(right,new LinearLayout.LayoutParams(0,-1,1));
    LinearLayout prayer=card();prayer.addView(txt("مواقيت الصلاة - الرياض",17,true));prayers=col();prayer.addView(prayers,new LinearLayout.LayoutParams(-1,0,1));LinearLayout next=row();nextName=txt("الصلاة القادمة",14,true);countdown=txt("--:--:--",22,true);countdown.setTextColor(accentColor());next.addView(nextName,new LinearLayout.LayoutParams(0,-2,1));next.addView(countdown);prayer.addView(next);right.addView(prayer,new LinearLayout.LayoutParams(-1,0,1.4f));
    LinearLayout media=card();media.addView(txt("الوسائط والتطبيقات",16,true));media.addView(txt("افتح تطبيقاتك المفضلة",12,false));LinearLayout mr=row();String[][] apps={{"Maps","com.google.android.apps.maps"},{"YouTube","com.google.android.youtube"},{"Brave","com.brave.browser"},{"Spotify","com.spotify.music"},{"ثمانية","com.thmanyah.android"}};for(String[] a:apps){Button b=btn(a[0],false);b.setOnClickListener(v->launch(a[1]));mr.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));}media.addView(mr);right.addView(media,new LinearLayout.LayoutParams(-1,0,1));
    center.addView(txt("التطبيقات",14,true));appRow=row();String[][] shortcuts={{"Google Maps","com.google.android.apps.maps"},{"YouTube","com.google.android.youtube"},{"Brave","com.brave.browser"},{"Spotify","com.spotify.music"},{"ثمانية","com.thmanyah.android"},{"Play Store","com.android.vending"},{"الملفات","com.google.android.documentsui"}};for(String[] a:shortcuts){Button b=btn(a[0],false);b.setOnClickListener(v->launch(a[1]));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);lp.setMargins(dp(2),0,dp(2),0);appRow.addView(b,lp);}Button plus=btn("+",true);plus.setOnClickListener(v->showApps());appRow.addView(plus,new LinearLayout.LayoutParams(dp(48),dp(44)));center.addView(appRow,new LinearLayout.LayoutParams(-1,dp(52)));
    setContentView(root);
  }
  String providerName(){return provider.equals("google")?"Google Maps":provider.equals("hudhud")?"هدهد":"OpenStreetMap";}
  void selectProvider(){String[] a={"OpenStreetMap (داخل الواجهة)","Google Maps (تطبيق خارجي)","هدهد (يتطلب تطبيقًا/SDK مدعومًا)"};new AlertDialog.Builder(this).setTitle("مصدر الخرائط").setItems(a,(d,n)->{provider=n==1?"google":n==2?"hudhud":"osm";getPreferences(0).edit().putString("provider",provider).apply();render();}).show();}
  void destination(){EditText e=new EditText(this);e.setHint("اكتب الوجهة أو العنوان");new AlertDialog.Builder(this).setTitle("إلى أين تريد الذهاب؟").setView(e).setPositiveButton("ابدأ الملاحة",(d,n)->{String q=e.getText().toString().trim();if(q.isEmpty())return;try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("google.navigation:q="+Uri.encode(q)));i.setPackage("com.google.android.apps.maps");startActivity(i);}catch(Exception ex){try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(q)));startActivity(i);}catch(Exception ignored){toast("تعذر فتح تطبيق ملاحة");}}}).setNegativeButton("إلغاء",null).show();}
  void loadLive(){io.execute(()->{loadWeather();loadPrayer();});}
  String get(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);in.close();c.disconnect();return o.toString("UTF-8");}
  void loadWeather(){try{JSONObject j=new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=24.7136&longitude=46.6753&current=temperature_2m,weather_code&timezone=Asia%2FRiyadh")).getJSONObject("current");String t=Math.round(j.getDouble("temperature_2m"))+"°";String w=weatherText(j.optInt("weather_code"));runOnUiThread(()->{temp.setText(t);weather.setText(w);});}catch(Exception e){runOnUiThread(()->weather.setText("الطقس غير متاح"));}}
  String weatherText(int c){if(c==0)return"صحو";if(c<=3)return"غائم جزئيًا";if(c<=48)return"ضباب";if(c<=67)return"أمطار";if(c<=82)return"زخات مطر";if(c>=95)return"عواصف رعدية";return"غائم";}
  void loadPrayer(){try{String today=new SimpleDateFormat("dd-MM-yyyy",Locale.US).format(new Date());JSONObject t=new JSONObject(get("https://api.aladhan.com/v1/timingsByCity/"+today+"?city=Riyadh&country=Saudi%20Arabia&method=4")).getJSONObject("data").getJSONObject("timings");String[] values=new String[6];for(int i=0;i<6;i++)values[i]=t.getString(prayerKeys[i]).substring(0,5);runOnUiThread(()->{for(int i=0;i<6;i++){prayerTimes[i]=values[i];}showPrayerRows();});}catch(Exception e){runOnUiThread(()->{prayers.removeAllViews();prayers.addView(txt("تعذر تحميل المواقيت - تحقق من الاتصال",12,false));});}}
  void showPrayerRows(){prayers.removeAllViews();Calendar now=Calendar.getInstance();nextAt=-1;for(int i=0;i<6;i++){LinearLayout r=row();TextView n=txt(prayerNames[i],14,true);TextView t=txt(prayerTimes[i],14,true);r.addView(n,new LinearLayout.LayoutParams(0,dp(29),1));r.addView(t);prayers.addView(r);View line=new View(this);line.setBackgroundColor(dark?Color.rgb(45,64,83):Color.rgb(205,219,232));prayers.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));if(i!=1){String[] p=prayerTimes[i].split(":");Calendar x=(Calendar)now.clone();x.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p[0]));x.set(Calendar.MINUTE,Integer.parseInt(p[1]));x.set(Calendar.SECOND,0);if(x.getTimeInMillis()>now.getTimeInMillis()&&nextAt<0){nextAt=x.getTimeInMillis();nextName.setText("الصلاة القادمة: "+prayerNames[i]);}}}if(nextAt<0){String[] p=prayerTimes[0].split(":");Calendar x=(Calendar)now.clone();x.add(Calendar.DAY_OF_MONTH,1);x.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p[0]));x.set(Calendar.MINUTE,Integer.parseInt(p[1]));nextAt=x.getTimeInMillis();nextName.setText("الصلاة القادمة: الفجر");}}
  void customize(){String[] choices={"لون أزرق سماوي","أزرق","أخضر","بنفسجي","برتقالي","وردي","الوضع الداكن/الفاتح"};new AlertDialog.Builder(this).setTitle("تخصيص المظهر").setItems(choices,(d,n)->{if(n==6)dark=!dark;else accent=n;getPreferences(0).edit().putInt("accent",accent).putBoolean("dark",dark).apply();render();loadLive();}).setNeutralButton("إعادة الافتراضي",(d,n)->{accent=0;dark=true;getPreferences(0).edit().clear().apply();render();loadLive();}).show();}
  void showApps(){List<android.content.pm.ResolveInfo> list=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));String[] names=new String[list.size()];for(int i=0;i<list.size();i++)names[i]=list.get(i).loadLabel(getPackageManager()).toString();new AlertDialog.Builder(this).setTitle("التطبيقات المثبتة").setItems(names,(d,n)->{Intent i=getPackageManager().getLaunchIntentForPackage(list.get(n).activityInfo.packageName);if(i!=null)startActivity(i);}).show();}
  void launch(String p){try{Intent i=getPackageManager().getLaunchIntentForPackage(p);if(i!=null)startActivity(i);else showApps();}catch(Exception e){showApps();}}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override protected void onDestroy(){super.onDestroy();h.removeCallbacksAndMessages(null);io.shutdownNow();}
}
