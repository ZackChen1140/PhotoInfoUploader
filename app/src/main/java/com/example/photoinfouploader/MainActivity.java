//2.1 直接拍攝的情況，天氣資訊就抓最新的資料。(超過24小時資料待處理)
//2.2 新增抓取感測器資訊功能&新增reset function&修正超出24小時的照片誤抓資料的問題。
//3.0 天氣資訊取得程式碼重構
//3.1 EXIF取得程式碼重構&刪除無用function
//3.2 新增照片儲存
package com.example.photoinfouploader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

//3.1 Remark
/*import android.os.Environment;
import android.media.ExifInterface;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.lang.Math;*/
//3.1 Remark

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_OTHER_ACTIVITY = 1;
    private TextView albumListTV;
    private TextView mainHintTV;
    private RecyclerView rcv;
    private FloatingActionButton createAlbumFAB;
    private StorageTools storageTools;
    private AlbumAdapter albumAdapter;

    private EXIFReader exifReader;
    private DisplayMetrics displayMetrics;
    private int screenHeight;

    //2.2 Add

    //private String hisWeatherInfo; //3.0 Remark
    //private boolean mutex=true; //3.0 Remark
    //private ExifInterface exifInfo; //3.1 Remark
    /*private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView PhotoIV;
    private Button selectPhotoBTN;
    private Button shotPhotoBTN;
    private Button analyzePhotoBTN;
    private Button storagePhotoBTN; //3.2 Add
    private TextView exifInfoTV;
    private TextView weatherInfoTV;
    private Uri mImageUri;
    private String photoPath; //3.1 Remark //3.2 Add
    private String showExifInfo;
    private String showWeatherInfo;
    private float[] accelerometerValue;
    private float[] gyroscopeValue;
    private float[] megnetometerValue;
    private EXIFReader exifReader;

    private WeatherAPICaller weatherAPICaller; //3.0 Add
    //2.2 Add
    private SensorDataReader sensorDataReader;*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        StringBuffer word = new StringBuffer();
        switch (permissions.length) {
            case 1:
                if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) word.append("儲存權限");
                else word.append("讀取權限");
                if (grantResults[0] == 0) word.append("已取得");
                else word.append("未取得");
                word.append("\n");
                if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) word.append("讀取權限");
                else word.append("儲存權限");
                word.append("已取得");

                break;
            case 2:
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) word.append("儲存權限");
                    else word.append("讀取權限");
                    if (grantResults[i] == 0) word.append("已取得");
                    else word.append("未取得");
                    if (i < permissions.length - 1) word.append("\n");
                }
                break;
        }
    }
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OTHER_ACTIVITY) {
            // resultCode為RESULT_OK表示操作成功
            if (resultCode == RESULT_OK) {
                // 這是從其他Activity返回的
                Bundle bundle = data.getExtras();
                if(bundle.containsKey("back mode"))
                {
                    if(bundle.getInt("back mode")==1) storageTools.deleteAlbum(this, bundle.getInt("back album ID"));
                    albumAdapter = new AlbumAdapter(this, getAlbumIdList(), getAlbumImageMap(), getAlbumNameList());
                    rcv.setAdapter(albumAdapter);
                    rcv.setLayoutManager(new LinearLayoutManager(this));
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }*/
    private void init()
    {
        requestPermission();
        albumListTV = findViewById(R.id.albumListTextView);
        mainHintTV = findViewById(R.id.mainHintTextView);
        rcv = findViewById(R.id.albumsRCV);
        /*selectPhotoBTN=(Button)findViewById(R.id.selectPhotoButton);
        shotPhotoBTN=(Button)findViewById(R.id.shotPhotoButton);
        analyzePhotoBTN=(Button)findViewById(R.id.analyzePhotoButton);
        storagePhotoBTN=(Button)findViewById(R.id.storagePhotoButton); //3.2 Add
        PhotoIV=(ImageView)findViewById(R.id.photoInfoImageView);
        exifInfoTV=(TextView)findViewById(R.id.exifInfoTextView);
        weatherInfoTV=(TextView)findViewById(R.id.weatherInfoTextView);*/
        createAlbumFAB = findViewById(R.id.createAlbumFloatingActionButton);

        /*sensorDataReader = new SensorDataReader(this); //2.2 add
        weatherAPICaller = new WeatherAPICaller(); //3.0 add
        exifReader = new EXIFReader();*/
        storageTools = new StorageTools();
        exifReader = new EXIFReader();


        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float density = displayMetrics.density;
        screenHeight = (int)(displayMetrics.heightPixels/density);

        List<Integer> albumIdList = getAlbumIdList();
        if(!albumIdList.isEmpty()) mainHintTV.setVisibility(View.GONE);

        albumAdapter = new AlbumAdapter(this, albumIdList, getAlbumImageMap(), getAlbumNameList());
        rcv.setAdapter(albumAdapter);
        rcv.setLayoutManager(new LinearLayoutManager(this));

        //callAPI("https://opendata.cwb.gov.tw/historyapi/v1/getMetadata/O-A0001-001?Authorization=rdec-key-123-45678-011121314",0); 3.0 Remark

        listeners();
    }
    private void listeners()
    {
        //3.2 Add
        createAlbumFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                //intent.putExtra("photo", photoData);
                Bundle bundle = new Bundle();
                bundle.putInt("album ID", 0);
                bundle.putBoolean("save", false);
                intent.putExtras(bundle);
                startActivity(intent);*/
                showInputDialog();
            }
        });
        rcv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) rcv.getLayoutManager();
                int itemCount = layoutManager.getItemCount();
                if (itemCount*110>screenHeight&&!recyclerView.canScrollVertically(1)) {
                    createAlbumFAB.setVisibility(View.GONE);
                } else {
                    createAlbumFAB.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請輸入相簿名稱");

        // 設置輸入框
        final EditText input = new EditText(this);
        builder.setView(input);

        // 設置確認按鈕
        builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 在這裡處理使用者輸入的資訊
                String newAlbumName = input.getText().toString();
                int newAlbumId = storageTools.createAlbum(getBaseContext(), newAlbumName);
                // 創建Intent來啟動另一個Activity並傳遞資訊
                Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("album ID", newAlbumId);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }
        });

        // 設置取消按鈕
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // 顯示AlertDialog
        builder.show();
    }
    private List<Integer> getAlbumIdList()
    {
        List<Integer> albumIdList = new ArrayList<Integer>();
        File crtDir = this.getFilesDir();
        File[] fileList = crtDir.listFiles();
        for(File file : fileList)
        {
            if (!file.isDirectory()) continue;
            String fileName = file.getName();
            int dirId = Integer.parseInt(fileName.substring(6));
            albumIdList.add(dirId);
        }
        return albumIdList;
    }
    private List<String> getAlbumNameList()
    {
        try {
            List<String> albumNameList = new ArrayList<String>();
            File crtDir = this.getFilesDir();
            File[] fileList = crtDir.listFiles();
            for(File file : fileList)
            {
                if (!file.isDirectory()) continue;
                String photoPathesFilePath = file.getAbsolutePath() + "/photoPathes.json";
                File photoPathesFile = new File(photoPathesFilePath);
                if(!photoPathesFile.exists()) return (new ArrayList<String>());
                String fileContent = storageTools.readFile(photoPathesFilePath);
                JSONObject mainObject = new JSONObject(fileContent);
                albumNameList.add(mainObject.getString("album name"));
            }
            return albumNameList;
        }
        catch (IOException | JSONException e){
            return (new ArrayList<String>());
        }

    }
    private HashMap<Integer, Bitmap> getAlbumImageMap()
    {
        try {
            HashMap<Integer, Bitmap> albumImageMap = new HashMap<Integer, Bitmap>();
            File crtDir = this.getFilesDir();
            File[] fileList = crtDir.listFiles();
            for(File file : fileList)
            {
                if (!file.isDirectory()) continue;
                String fileName = file.getName();
                int dirId = Integer.parseInt(fileName.substring(6));
                String photoPathesFilePath = file.getAbsolutePath() + "/photoPathes.json";
                File photoPathesFile = new File(photoPathesFilePath);
                if(!photoPathesFile.exists()) return (new HashMap<Integer, Bitmap>());
                String fileContent = storageTools.readFile(photoPathesFilePath);
                JSONObject mainObject = new JSONObject(fileContent);
                if(mainObject.has("photos"))
                {
                    JSONArray photosArray = mainObject.getJSONArray("photos");
                    JSONObject firstObject = photosArray.getJSONObject(0);
                    String photoPath = firstObject.getString("photo path");

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 16;
                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);

                    if(exifReader.getPhotoOrientation(photoPath).equals("6"))
                    {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }

                    albumImageMap.put(dirId, bitmap);
                }
            }
            return albumImageMap;
        }
        catch (IOException | JSONException e){
            return (new HashMap<Integer, Bitmap>());
        }
    }
    private void requestPermission()
    {
        boolean[] permissionHasGone = new boolean[7];
        permissionHasGone[0] = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[1] = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[2] = checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[3] = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[4] = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[5] = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionHasGone[6] = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        String[] permissionStr = new String[7];
        permissionStr[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        permissionStr[1] = Manifest.permission.READ_EXTERNAL_STORAGE;
        permissionStr[2] = Manifest.permission.ACCESS_MEDIA_LOCATION;
        permissionStr[3] = Manifest.permission.ACCESS_FINE_LOCATION;
        permissionStr[4] = Manifest.permission.ACCESS_COARSE_LOCATION;
        permissionStr[5] = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
        permissionStr[6] = Manifest.permission.CAMERA;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int numOfPermissions = 0;
            for(int i=0;i<permissionHasGone.length;++i)
                if(!permissionHasGone[i])
                    ++numOfPermissions;
            if(numOfPermissions==0) return;
            String[] permissions = new String[numOfPermissions];

            int index=0;
            for(int i=0;i<permissionHasGone.length;++i)
            {
                if(!permissionHasGone[i])
                {
                    permissions[index] = permissionStr[i];
                    ++index;
                }
            }
            requestPermissions(permissions, 100);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if(!albumAdapter.getDeleteMode())
            {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("離開").setMessage("是否要離開?")
                        .setPositiveButton("離開", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                finishAffinity();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        }).show();
            }
            else
            {
                albumAdapter.exitDeleteMode();
            }
        }
        return true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解除位置監聽器的註冊
    }
}



//3.1 Remark
    /*private float transferLocation(String relationalStr, String ref)
    {
        String[] parts = relationalStr.split(",");
        String[] pair;

        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim())/Double.parseDouble(pair[1].trim());
        pair = parts[1].split("/");
        double minutes = Double.parseDouble(pair[0].trim())/Double.parseDouble(pair[1].trim());
        pair = parts[2].split("/");
        double seconds = Double.parseDouble(pair[0].trim())/Double.parseDouble(pair[1].trim());

        double result = degrees + (minutes/60.0)+(seconds/3600.0);
        if(ref.equals("S")||ref.equals("W"))
            return (float)-result;
         return (float)result;
    }



    private String getShotDirection(String exifOrient)
    {
        String shotDir = "None";
        switch(exifOrient)
        {
            case "1":
                shotDir = "水平";
                break;
            case "2":
                shotDir = "水平左右鏡像";
                break;
            case "3":
                shotDir = "水平轉置";
                break;
            case "4":
                shotDir = "水平上下鏡像";
                break;
            case "5":
                shotDir = "垂直左右鏡像";
                break;
            case "6":
                shotDir = "垂直";
                break;
            case "7":
                shotDir = "垂直轉置左右鏡像";
                break;
            case "8":
                shotDir = "垂直轉置";
                break;
        }

        return shotDir;
    }
    private Uri getImageUriFromBitmap(Bitmap imageBitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "Title", null);
        return Uri.parse(path);
    }

    private String saveImageToExternalStorage(Uri imageUri)
    {
        // 指定儲存目錄
        //File storageDir = getFilesDir(); // 內部儲存空間
        // 或者使用下面的程式碼儲存到外部儲存空間
        String storagePath = "/storage/emulated/0/DCIM/PhotoInfoUploader/";
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // 建立檔案名稱
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = timeStamp + ".jpg";

        File imageFile = new File(storageDir, imageFileName);
        try {
            FileInputStream fis = new FileInputStream(new File(imageUri.getPath()));
            FileOutputStream fos = new FileOutputStream(imageFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.flush();
            fos.close();
            fis.close();

            ExifInterface originalExif = new ExifInterface(imageFile.getAbsolutePath());
            ExifInterface newExif = new ExifInterface(imageFile.getAbsolutePath());

            // 獲取Exif屬性名稱
            String[] allAttributes = {
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_IMAGE_LENGTH,
                    ExifInterface.TAG_IMAGE_WIDTH,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_APERTURE,
                    ExifInterface.TAG_ISO_SPEED_RATINGS,
                    ExifInterface.TAG_WHITE_BALANCE,
                    ExifInterface.TAG_FOCAL_LENGTH
            };
            for (String attribute : allAttributes) {
                String value = originalExif.getAttribute(attribute);
                newExif.setAttribute(attribute, value);
            }

            // 保存Exif資訊
            newExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageFile.getAbsolutePath();
    }*/
//3.1 Remark

//3.0 Remark (移至WeatherAPICaller)
    /*private void callAPI(String url, int mode)
    {
        Runnable runable0 = new Runnable() {
            @Override
            public void run() {
                try {
                    String NetUrl = url;
                    InputStream is = new URL(NetUrl).openStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line = rd.readLine();
                    while (line != null)
                    {
                        sb.append(line);
                        line = rd.readLine();
                    }
                    if(sb.length()!=0) hisWeatherInfo = sb.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable runable1 = new Runnable() {
            @Override
            public void run() {
                try {
                    Double exifLat = Double.parseDouble(String.valueOf(transferLocation(exifInfo.getAttribute(ExifInterface.TAG_GPS_LATITUDE),exifInfo.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))));
                    Double exifLon = Double.parseDouble(String.valueOf(transferLocation(exifInfo.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),exifInfo.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))));

                    Connection conn = Jsoup.connect(url);
                    conn.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/    20100101 Firefox/32.0");
                    final Document docs = conn.get();

                    int closestIndex = 0;
                    Double minDistance=Double.MAX_VALUE;

                    Elements locElements = docs.select("location");
                    for (int i = 0; i < locElements.size(); ++i)
                    {
                        Element locElement = locElements.get(i);
                        Double lat = Double.parseDouble(locElement.select("lat").get(0).text());
                        Double lon = Double.parseDouble(locElement.select("lon").get(0).text());
                        Double distance = computeDistance(lat, lon, exifLat, exifLon);

                        if(distance<minDistance)
                        {
                            closestIndex = i;
                            minDistance = distance;
                        }
                    }
                    Element targetElement = locElements.get(closestIndex);
                    Elements weatherElements = targetElement.select("weatherElement");
                    String H_FX = weatherElements.get(7).select("elementValue").get(0).text();
                    String H_XD = weatherElements.get(8).select("elementValue").get(0).text();
                    if(H_FX.equals("-99")) H_FX = "-";
                    if(H_XD.equals("-99")) H_XD = "-";

                    showWeatherInfo = "測站名稱: " + targetElement.select("locationName").get(0).text() + '\n' +
                            "風向: " + getWindDirection(weatherElements.get(1).select("elementValue").get(0).text()) + '\n' +
                            "風速: " + weatherElements.get(2).select("elementValue").get(0).text() + '\n' +
                            "溫度: " + weatherElements.get(3).select("elementValue").get(0).text() + '\n' +
                            "濕度: " + weatherElements.get(4).select("elementValue").get(0).text() + '\n' +
                            "氣壓: " + weatherElements.get(5).select("elementValue").get(0).text() + '\n' +
                            "日累積雨量: " + weatherElements.get(6).select("elementValue").get(0).text() + '\n' +
                            "陣風最大風速: " + H_FX + '\n' +
                            "陣風最大風向: " + H_XD + '\n' +
                            "天氣: " + weatherElements.get(14).select("elementValue").get(0).text() + '\n' + //2.2 Modify
                            //2.2 Add
                            "加速規: " + accelerometerValue[0] + ", " + accelerometerValue[1] + ", " + accelerometerValue[2] + '\n' +
                            "陀螺儀: " + gyroscopeValue[0] + ", " + gyroscopeValue[1] + ", " + gyroscopeValue[2] + '\n' +
                            "磁強器: " + megnetometerValue[0] + ", " + megnetometerValue[1] + ", " + megnetometerValue[2];
                            //2.2 Add

                    mutex=false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        if(mode == 0)
            new Thread(runable0).start();
        else if(mode == 1)
            new Thread(runable1).start();
    }

    private String readWeatherAPI(String hisWeatherResult, String exifTime)
    {
        String rExifDate = exifTime.substring(0,10).replace(':','-'); //2.2 Add
        String rExifTime = rExifDate + " " + exifTime.substring(11,14)+"00:00"; //2.2 Modify
        try {
            JSONObject JSONob = new JSONObject(hisWeatherResult);
            JSONObject JSONobData = JSONob.getJSONObject("dataset").getJSONObject("resources").getJSONObject("resource").getJSONObject("data");
            JSONArray JSONarrTime = JSONobData.getJSONArray("time");
            for(int i = 0; i<JSONarrTime.length();++i)
            {
                JSONObject JSONobTime = JSONarrTime.getJSONObject(i);
                String dataTime = JSONobTime.getString("dataTime");
                if(!dataTime.equals(rExifTime)) continue;

                String url = JSONobTime.getString("url");
                return url;
            }
            //return "None"; 2.1 Remark
            //2.1 Add
            JSONObject lastJSONobTime = JSONarrTime.getJSONObject(JSONarrTime.length()-1);
            //String url = JSONobTime.getString("url"); //2.2 Remark
            //return url; //2.2 Remark
            //2.2 Add
            String date = lastJSONobTime.getString("dataTime").substring(0,10);
            if(rExifDate.equals(date))
            {
                String url = lastJSONobTime.getString("url");
                return url;
            }
            return "None";
            //2.2 Add
            //2.1 Add
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private Double computeDistance(Double cLat, Double cLon, Double cExifLat, Double cExifLon)
    {
        Double disSquare = (cLat-cExifLat)*(cLat-cExifLat)+(cLon-cExifLon)*(cLon-cExifLon);
        Double distance = Math.sqrt(disSquare);
        return distance;
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
    }*/
//3.0 Remark