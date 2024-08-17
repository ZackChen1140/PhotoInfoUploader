package com.example.photoinfouploader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PhotoActivity extends Activity {
    private ScrollView sv;
    private ImageView photoIV;
    private Bitmap photoIV_bitmap;
    private Button exifBTN;
    private Button weatherBTN;
    private Button sensorBTN;
    private TextView exifTV;
    private TextView weatherTV;
    private TextView sensorTV;
    private FloatingActionButton saveFAB;
    private FloatingActionButton deleteFAB;
    private FloatingActionButton uploadFAB;
    private ProgressBar pd;
    String showExifInfo;
    String showWeatherInfo;
    String showSensorInfo;

    private Bundle bd;
    int albumId;
    private String photoPath;
    private EXIFReader exifReader;
    private WeatherAPICaller weatherAPICaller;
    private StorageTools storageTools;
    private Uploader uploader;
    private HashMap<String, String> weatherInfo;
    private float[] orientationArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        init();
    }
    private void init()
    {
        sv = findViewById(R.id.SV);
        photoIV = findViewById(R.id.photoImageView);
        exifBTN = findViewById(R.id.exifInfoButton);
        weatherBTN = findViewById(R.id.weatherInfoButton);
        sensorBTN = findViewById(R.id.sensorInfoButton);
        exifTV = findViewById(R.id.exifInfoTextView);
        weatherTV = findViewById(R.id.weatherInfoTextView);
        sensorTV = findViewById(R.id.sensorInfoTextView);
        saveFAB = findViewById(R.id.saveFloatingActionButton);
        deleteFAB = findViewById(R.id.deleteFloatingActionButton);
        uploadFAB = findViewById(R.id.uploadPhotoFloatingActionButton);
        pd = findViewById(R.id.photoProgressBar);

        bd = getIntent().getExtras();
        albumId = bd.getInt("album ID");
        storageTools = new StorageTools();
        exifReader = new EXIFReader();
        uploader = new Uploader();
        if(bd.getBoolean("preview mode"))
        {
            photoPath = bd.getString("photo path");
            exifReader.setPhotoPath(photoPath);

            HashMap<String, String> exifInfo = exifReader.getEXIF();
            Double latitude = Double.parseDouble(exifInfo.get("exifGPSLAT"));
            Double longitude = Double.parseDouble(exifInfo.get("exifGPSLONG"));

            weatherInfo = new HashMap<>();
            weatherAPICaller = new WeatherAPICaller(latitude, longitude, exifInfo.get("exifDatetime"));
            while(!weatherAPICaller.isGetUrlDayCompleted());
            if(weatherAPICaller.getUrl_hour()=="None")
            {
                Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("album ID", albumId);
                bundle.putInt("back mode", 4); //overtime
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }

            float[] accelerometerArray = bd.getFloatArray("accelerometer array");
            //float[] gyroscopeArray = bd.getFloatArray("gyroscope array");
            float[] megnetometerArray = bd.getFloatArray("megnetometer array");
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerArray, megnetometerArray);
            orientationArray = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationArray);
            for(int i = 0; i < 3; ++i)
            {
                orientationArray[i] = (float)Math.toDegrees(orientationArray[i]);
            }

            deleteFAB.setVisibility(View.INVISIBLE);
        }
        else
        {
            orientationArray = new float[3];
            setInfoFromJson(bd.getString("photo json object"));
            saveFAB.setVisibility(View.INVISIBLE);
        }

        photoIV_bitmap = BitmapFactory.decodeFile(photoPath);
        if(exifReader.getPhotoOrientation(photoPath).equals("6"))
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            photoIV_bitmap = Bitmap.createBitmap(photoIV_bitmap, 0, 0, photoIV_bitmap.getWidth(), photoIV_bitmap.getHeight(), matrix, true);
        }
        photoIV.setImageBitmap(photoIV_bitmap);

        listeners();
    }
    private void listeners()
    {
        exifBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(exifTV.getText().length()==0)
                {
                    exifTV.setVisibility(View.VISIBLE);
                    if(showExifInfo==null)
                    {
                        pd.setVisibility(View.VISIBLE);

                        HashMap<String, String> exifInfo = exifReader.getEXIF();
                        showExifInfo = "拍攝方向: " + exifReader.getShotDirection(exifInfo.get("exifOrient")) + '\n' +
                                "拍攝時間: " + exifInfo.get("exifDatetime") + '\n' +
                                "相機品牌: " + exifInfo.get("exifMaker") + '\n' +
                                "相機型號: " + exifInfo.get("exifModel") + '\n' +
                                "閃光燈: " + exifInfo.get("exifFlash") + '\n' +
                                "相片長度: " + exifInfo.get("exifImgLen") + '\n' +
                                "相片寬度: " + exifInfo.get("exifImgWid") + '\n' +
                                "緯度: " + exifInfo.get("exifGPSLAT") + '\n' +
                                "經度: " + exifInfo.get("exifGPSLONG") + '\n' +
                                "曝光時長: " + exifInfo.get("exifExposure") + '\n' +
                                "光圈: " + exifInfo.get("exifAperture") + '\n' +
                                "ISO: " + exifInfo.get("exifISO") + '\n' +
                                "白平衡: " + exifInfo.get("exifWB") + '\n' +
                                "鏡頭焦距: " + exifInfo.get("exifFocalLen");
                        pd.setVisibility(View.INVISIBLE);
                    }
                    exifTV.setText(showExifInfo);
                }
                else
                {
                    exifTV.setVisibility(View.GONE);
                    exifTV.setText("");
                }
            }
        });
        weatherBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(weatherTV.getText().length()==0)
                {
                    weatherTV.setVisibility(View.VISIBLE);
                    if(showWeatherInfo==null)
                    {
                        pd.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                HashMap<String, String> exifInfo = exifReader.getEXIF();
                                Double latitude = Double.parseDouble(exifInfo.get("exifGPSLAT"));
                                Double longitude = Double.parseDouble(exifInfo.get("exifGPSLONG"));
                                while(!weatherAPICaller.isGetDataCompleted());
                                weatherInfo = weatherAPICaller.getWeatherInfo();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!weatherInfo.isEmpty())
                                        {
                                            //5.0 add 配合資料庫改動
                                            String peakGustSpeed = weatherInfo.get("gustSpeed");
                                            String gustDirection = weatherInfo.get("gustDirection");
                                            if(peakGustSpeed.equals("-99")) peakGustSpeed = "-";
                                            if(gustDirection.equals("-99")) gustDirection = "-";
                                            //5.0 add 配合資料庫改動
                                            showWeatherInfo = "測站名稱: " + weatherInfo.get("locationName") + '\n' +
                                                    "風向: " + getWindDirection(weatherInfo.get("windDirection")) + '\n' +
                                                    "風速: " + weatherInfo.get("windSpeed") + '\n' +
                                                    "溫度: " + weatherInfo.get("temperature") + '\n' +
                                                    "濕度: " + weatherInfo.get("humidity") + '\n' +
                                                    "氣壓: " + weatherInfo.get("pressure") + '\n' +
                                                    "日累積雨量: " + weatherInfo.get("dayRain") + '\n' +
                                                    "陣風最大風速: " + peakGustSpeed + '\n' +
                                                    "陣風最大風向: " + gustDirection + '\n' +
                                                    "天氣: " + weatherInfo.get("weather");
                                        }
                                        else
                                        {
                                            showWeatherInfo = "天氣資訊已被刪除";
                                        }
                                        pd.setVisibility(View.INVISIBLE);
                                        weatherTV.setText(showWeatherInfo);
                                    }
                                });
                            }
                        }).start();
                    }
                    weatherTV.setText(showWeatherInfo);
                }
                else
                {
                    weatherTV.setVisibility(View.GONE);
                    weatherTV.setText("");
                }
            }
        });
        sensorBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sensorTV.getText().length()==0)
                {
                    sensorTV.setVisibility(View.VISIBLE);
                    if(showSensorInfo==null)
                    {
                        //HashMap<String, String> exifInfo = exifReader.getEXIF();

                        showSensorInfo = "pitch: " + orientationArray[1] + "°" + '\n' +
                                "roll: " + orientationArray[2] + "°" + '\n' +
                                "yaw: " + orientationArray[0] + "°";
                    }
                    sensorTV.setText(showSensorInfo);
                }
                else
                {
                    sensorTV.setVisibility(View.GONE);
                    sensorTV.setText("");
                }
            }
        });
        saveFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(weatherInfo.isEmpty())
                        {
                            while(!weatherAPICaller.isGetDataCompleted());
                            weatherInfo = weatherAPICaller.getWeatherInfo();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.setVisibility(View.INVISIBLE);
                                int saveResult;
                                try
                                {
                                    saveResult = storageTools.save(getBaseContext(), photoPath, albumId, orientationArray, weatherInfo, bd.getString("location_provider"));
                                }
                                catch (JSONException|IOException e)
                                {
                                    saveResult = 1000;
                                }
                                if(saveResult!=0)
                                {
                                    Toast toast = Toast.makeText(getBaseContext(), "錯誤代碼:" + saveResult, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("album ID", albumId);
                                    bundle.putInt("back mode", 1);
                                    //bundle.putBoolean("init failed", false);
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
                    }
                }).start();
            }
        });
        deleteFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    storageTools.deletePhoto(getBaseContext(), albumId, photoPath);
                    Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("album ID", albumId);
                    bundle.putInt("back mode", 2); //delete
                    intent.putExtras(bundle);
                    startActivity(intent);
                    finish();
                }
                catch (IOException|JSONException e)
                {
                    Toast toast = Toast.makeText(getBaseContext(), "刪除失敗", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        uploadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(bd.getBoolean("preview mode"))
                        {
                            if(weatherInfo.isEmpty())
                            {
                                while(!weatherAPICaller.isGetDataCompleted());
                                weatherInfo = weatherAPICaller.getWeatherInfo();
                            }
                            int saveResult;
                            try
                            {
                                saveResult = storageTools.save(getBaseContext(), photoPath, albumId, orientationArray, weatherInfo, bd.getString("location_provider"));
                            }
                            catch (IOException|JSONException e)
                            {
                                saveResult = 1000;
                            }
                            final int errorCode = saveResult;
                            if(saveResult!=0)
                            {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getBaseContext(), "錯誤代碼:" + errorCode, Toast.LENGTH_SHORT).show();
                                        pd.setVisibility(View.INVISIBLE);
                                    }
                                });
                                return;
                            }
                        }
                        //boolean uploadFinished = uploader.uploadPhoto(getBaseContext(), albumId, photoPath);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uploader.uploadPhoto2Firebase(getBaseContext(), albumId, photoPath, new Uploader.OnCompleteListener() {
                                    @Override
                                    public void onComplete(boolean success) {
                                        if (success) {
                                            pd.setVisibility(View.INVISIBLE);
                                            Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
                                            Bundle bundle = new Bundle();
                                            bundle.putInt("album ID", albumId);
                                            try
                                            {
                                                storageTools.deletePhoto(getBaseContext(), albumId, photoPath);
                                                bundle.putInt("back mode", 3); //upload
                                            }
                                            catch (IOException|JSONException e)
                                            {
                                                bundle.putInt("back mode", 5); //upload&delete_failed
                                            }
                                            intent.putExtras(bundle);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            pd.setVisibility(View.INVISIBLE);
                                            Toast.makeText(PhotoActivity.this, "上傳失敗！", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
                    }
                }).start();
            }
        });
        sv.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int scrollContentHeight = sv.getChildAt(0).getHeight();
                int scrollHeight = sv.getHeight();

                if (scrollY + scrollHeight >= scrollContentHeight)
                {
                    saveFAB.setVisibility(View.INVISIBLE);
                    deleteFAB.setVisibility(View.INVISIBLE);
                    uploadFAB.setVisibility(View.INVISIBLE);
                }
                else
                {
                    if(bd.getBoolean("preview mode"))
                    {
                        saveFAB.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        deleteFAB.setVisibility(View.VISIBLE);
                    }
                    uploadFAB.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    private void setInfoFromJson(String JsonContent)
    {
        try
        {
            JSONObject photoObject = new JSONObject(JsonContent);
            photoPath = photoObject.getString("photo path");

            //5.0 Modify JSON不再儲存EXIF，改為直接透過EXIFReader取得
            exifReader.setPhotoPath(photoPath);
            HashMap<String, String> exifInfo = exifReader.getEXIF();
            showExifInfo = "拍攝方向: " + exifReader.getShotDirection(exifInfo.get("exifOrient")) + '\n' +
                    "拍攝時間: " + exifInfo.get("exifDatetime") + '\n' +
                    "相機品牌: " + exifInfo.get("exifMaker") + '\n' +
                    "相機型號: " + exifInfo.get("exifModel") + '\n' +
                    "閃光燈: " + exifInfo.get("exifFlash") + '\n' +
                    "相片長度: " + exifInfo.get("exifImgLen") + '\n' +
                    "相片寬度: " + exifInfo.get("exifImgWid") + '\n' +
                    "緯度: " + exifInfo.get("exifGPSLAT") + '\n' +
                    "經度: " + exifInfo.get("exifGPSLONG") + '\n' +
                    "曝光時長: " + exifInfo.get("exifExposure") + '\n' +
                    "光圈: " + exifInfo.get("exifAperture") + '\n' +
                    "ISO: " + exifInfo.get("exifISO") + '\n' +
                    "白平衡: " + exifInfo.get("exifWB") + '\n' +
                    "鏡頭焦距: " + exifInfo.get("exifFocalLen");
            /*JSONObject exifObject = photoObject.getJSONObject("exifInfo");
            showExifInfo = "拍攝方向: " + exifObject.getString("exifOrient") + '\n' +
                    "拍攝時間: " + exifObject.getString("exifDatetime") + '\n' +
                    "相機品牌: " + exifObject.getString("exifMaker") + '\n' +
                    "相機型號: " + exifObject.getString("exifModel") + '\n' +
                    "閃光燈: " + exifObject.getString("exifFlash") + '\n' +
                    "相片長度: " + exifObject.getString("exifImgLen") + '\n' +
                    "相片寬度: " + exifObject.getString("exifImgWid") + '\n' +
                    "緯度: " + exifObject.getString("exifGPSLAT") + '\n' +
                    "經度: " + exifObject.getString("exifGPSLONG") + '\n' +
                    "曝光時長: " + exifObject.getString("exifExposure") + '\n' +
                    "光圈: " + exifObject.getString("exifAperture") + '\n' +
                    "ISO: " + exifObject.getString("exifISO") + '\n' +
                    "白平衡: " + exifObject.getString("exifWB") + '\n' +
                    "鏡頭焦距: " + exifObject.getString("exifFocalLen");*/
            //5.0 Modify

            JSONObject weatherObject = photoObject.getJSONObject("weatherInfo");
            //5.0 add 配合資料庫改動
            String peakGustSpeed = weatherObject.getString("gustSpeed");
            String gustDirection = weatherObject.getString("gustDirection");
            if(peakGustSpeed.equals("-99")) peakGustSpeed = "-";
            if(gustDirection.equals("-99")) gustDirection = "-";
            //5.0 add 配合資料庫改動
            showWeatherInfo = "測站名稱: " + weatherObject.getString("locationName") + '\n' +
                    "風向: " + getWindDirection(weatherObject.getString("windDirection")) + '\n' +
                    "風速: " + weatherObject.getString("windSpeed") + '\n' +
                    "溫度: " + weatherObject.getString("temperature") + '\n' +
                    "濕度: " + weatherObject.getString("humidity") + '\n' +
                    "氣壓: " + weatherObject.getString("pressure") + '\n' +
                    "日累積雨量: " + weatherObject.getString("dayRain") + '\n' +
                    "陣風最大風速: " + peakGustSpeed + '\n' +
                    "陣風最大風向: " + gustDirection + '\n' +
                    "天氣: " + weatherObject.getString("weather");

            /*JSONObject sensorObject = photoObject.getJSONObject("sensorInfo");
            JSONArray accArray = sensorObject.getJSONArray("accelerometer");
            JSONArray gyrArray = sensorObject.getJSONArray("gyroscope");
            JSONArray megArray = sensorObject.getJSONArray("megnetometer");
            accelerometerArray = new float[3];
            gyroscopeArray = new float[3];
            megnetometerArray = new float[3];
            for(int i = 0; i<3;++i)
            {
                accelerometerArray[i] = (float)accArray.getDouble(i);
                gyroscopeArray[i] = (float)gyrArray.getDouble(i);
                megnetometerArray[i] = (float)megArray.getDouble(i);
            }
            showSensorInfo = "加速規: " + accelerometerArray[0] + ", " + accelerometerArray[1] + ", " + accelerometerArray[2] + '\n' +
                    "陀螺儀: " + gyroscopeArray[0] + ", " + gyroscopeArray[1] + ", " + gyroscopeArray[2] + '\n' +
                    "磁強器: " + megnetometerArray[0] + ", " + megnetometerArray[1] + ", " + megnetometerArray[2];*/
            JSONObject orientationObject = photoObject.getJSONObject("orientation info");
            orientationArray[0] = (float)orientationObject.getDouble("yaw");
            orientationArray[1] = (float)orientationObject.getDouble("pitch");
            orientationArray[2] = (float)orientationObject.getDouble("roll");
            showSensorInfo = "pitch: " + orientationArray[1] + "°" + '\n' +
                    "roll: " + orientationArray[2] + "°" + '\n' +
                    "yaw: " + orientationArray[0] + "°";
        }
        catch (JSONException e)
        {
            Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("album ID", albumId);
            //bundle.putBoolean("save", false);
            bundle.putInt("back mode", 99); //exception
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        }
    }
    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            layoutParams.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }
    private String getWindDirection(String WDIR)
    {
        Double windDIRv = Double.valueOf(WDIR);
        String windDIR = "";
        if(windDIRv>348.75||windDIRv<11.25)
            windDIR = "北";
        else if(windDIRv>11.25&&windDIRv<33.75)
            windDIR = "北北東";
        else if(windDIRv>33.75&&windDIRv<56.25)
            windDIR = "東北";
        else if(windDIRv>56.25&&windDIRv<78.75)
            windDIR = "東北東";
        else if(windDIRv>78.75&&windDIRv<101.25)
            windDIR = "東";
        else if(windDIRv>101.25&&windDIRv<123.75)
            windDIR = "東南東";
        else if(windDIRv>123.75&&windDIRv<146.25)
            windDIR = "東南";
        else if(windDIRv>146.25&&windDIRv<168.75)
            windDIR = "南南東";
        else if(windDIRv>168.75&&windDIRv<191.25)
            windDIR = "南";
        else if(windDIRv>191.25&&windDIRv<213.75)
            windDIR = "南南西";
        else if(windDIRv>213.75&&windDIRv<236.25)
            windDIR = "西南";
        else if(windDIRv>236.25&&windDIRv<258.75)
            windDIR = "西南西";
        else if(windDIRv>258.75&&windDIRv<281.25)
            windDIR = "西";
        else if(windDIRv>281.25&&windDIRv<303.75)
            windDIR = "西北西";
        else if(windDIRv>303.75&&windDIRv<326.25)
            windDIR = "西北";
        else if(windDIRv>326.25&&windDIRv<348.75)
            windDIR = "北北西";

        return windDIR;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            Intent intent = new Intent(PhotoActivity.this, AlbumActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("album ID", albumId);
            if(bd.getBoolean("preview mode")&&bd.getBoolean("isShoted"))
            {
                try
                {
                    //6.2 根本沒有存，所以不用更新JSON檔
                    //storageTools.deletePhoto(this, albumId, photoPath);
                    File photoFile = new File(photoPath);
                    photoFile.delete();
                    //6.2 根本沒有存，所以不用更新JSON檔

                    bundle.putInt("back mode", 0);
                }
                catch (Exception e)
                {
                    bundle.putInt("back mode", 6); //本機殘留檔案
                }
            }
            else
            {
                bundle.putInt("back mode", 0); //normal back
            }
            //keycode_back
            //bundle.putBoolean("save", false);
            //bundle.putBoolean("init failed", false);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoIV_bitmap.recycle();
    }
}
