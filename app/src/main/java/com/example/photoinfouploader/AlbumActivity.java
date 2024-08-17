package com.example.photoinfouploader;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.base.MoreObjects;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class AlbumActivity extends Activity {
    private TextView albumTitleNameTV;
    private TextView albumHintTV;
    private boolean isNoPhoto;
    private RecyclerView rcv;
    private FloatingActionButton plusFAB;
    private FloatingActionButton selectFAB;
    private FloatingActionButton shotFAB;
    private FloatingActionButton uploadFAB;
    private ProgressBar pb;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Intent takePictureIntent;
    private Bundle bd;
    private int albumID;
    private String photoPath;
    private float[] accelerometerValue;
    private float[] gyroscopeValue;
    private float[] megnetometerValue;

    private LocationManager locationManager;
    private LocationListener locationListener;
    boolean provider_exist = false;

    private PhotoAdapter adapter;
    private EXIFReader exifReader;
    private SensorDataReader sensorDataReader;
    private StorageTools storageTools;
    private Uploader uploader;

    private DisplayMetrics displayMetrics;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorDataReader.startListening();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 3000 && resultCode == RESULT_OK) {
            Uri uri = data.getData();//取得相片路徑
            ContentResolver resolver = this.getContentResolver();
            photoPath = getFilePathFromContentUri(uri, resolver); //3.1 Remark  //3.2 Add
            exifReader.setPhotoPath(photoPath);
            HashMap<String, String> exifInfo = exifReader.getEXIF();
            if (exifInfo.get("exifGPSLAT") == null || exifInfo.get("exifGPSLONG") == null)
            {
                super.onActivityResult(requestCode, resultCode, data);
                Toast.makeText(this, "該照片沒有位置資訊!", Toast.LENGTH_SHORT).show();
            }
            else
            {
                bd.putBoolean("isShoted", false);
                bd.putString("location_provider", "unknown");
                toPreviewActivity();
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(takePictureIntent!=null) takePictureIntent=null;

            accelerometerValue = sensorDataReader.getAccelerometerValue();
            gyroscopeValue = sensorDataReader.getGyroscopeValue();
            megnetometerValue = sensorDataReader.getMegnetometerValue();
            exifReader.setPhotoPath(photoPath);
            HashMap<String, String> exifInfo = exifReader.getEXIF();
            if (exifInfo.get("exifGPSLAT") == null || exifInfo.get("exifGPSLONG") == null)
            {
                try
                {
                    Location location = getLocation();
                    exifReader.addGPSinfo(location);
                    bd.putBoolean("isShoted", true);
                    bd.putString("location_provider", location.getProvider());
                    toPreviewActivity();
                }
                catch (Exception e) {
                    File photoFile = new File(photoPath);
                    if (photoFile.exists()) photoFile.delete();
                    if (provider_exist) {
                        super.onActivityResult(requestCode, resultCode, data);
                        Toast.makeText(this, "請在訊號良好的地點拍攝!", Toast.LENGTH_SHORT);
                    } else {
                        super.onActivityResult(requestCode, resultCode, data);
                        Toast.makeText(this, "請給予定位權限並開啟定位服務!", Toast.LENGTH_SHORT);
                    }
                }
            }
            else
            {
                bd.putBoolean("isShoted", true);
                bd.putString("location_provider", "gps");
                toPreviewActivity();
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void init() {
        albumTitleNameTV = findViewById(R.id.albumTitleTextView);
        albumHintTV = findViewById(R.id.albumHintTextView);
        rcv = findViewById(R.id.photosRCV);
        plusFAB = findViewById(R.id.plusFloatingActionButton);
        selectFAB = findViewById(R.id.selectFloatingActionButton);
        shotFAB = findViewById(R.id.shotFloatingActionButton);
        uploadFAB = findViewById(R.id.uploadAlbumFloatingActionButton);
        pb = findViewById(R.id.albumProgressBar);

        sensorDataReader = new SensorDataReader(this); //2.2 add
        exifReader = new EXIFReader();
        storageTools = new StorageTools();
        uploader = new Uploader();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }
        };

        requestLocationUpdate();
        if (!provider_exist)
        {
            Toast.makeText(this, "請給予定位權限並開啟定位服務!", Toast.LENGTH_SHORT).show();
        }

        sensorReset();

        bd = getIntent().getExtras();
        albumID = bd.getInt("album ID");
        albumTitleNameTV.setText(readAlbumName());

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float density = displayMetrics.density;
        screenHeight = (int)(displayMetrics.heightPixels/density);

        JSONArray photos = readAlbum();
        if(photos.length()==0)
        {
            albumHintTV.setVisibility(View.VISIBLE);
            isNoPhoto = true;
        }

        int columnLen = 4;
        adapter = new PhotoAdapter(this, readAlbum(), albumID);
        rcv.setAdapter(adapter);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(columnLen, StaggeredGridLayoutManager.VERTICAL);
        rcv.setLayoutManager(layoutManager);

        if(bd.containsKey("back mode"))
        {
            switch (bd.getInt("back mode"))
            {
                case 1:
                    Toast.makeText(this, "儲存完成", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(this, "刪除完成", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(this, "上傳完成", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    Toast.makeText(this, "天氣資訊已刪除，必須是一天內拍攝的照片!", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    Toast.makeText(this, "上傳完成，但本機照片刪除失敗!", Toast.LENGTH_SHORT).show();
                case 6:
                    Toast.makeText(this, "照片刪除失敗，本機可能殘留檔案!", Toast.LENGTH_SHORT).show();
                case 99:
                    Toast.makeText(this, "圖片載入失敗", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        listeners();
//        Toast toast;
//        if(bd.getBoolean("save"))
//        {
//            toast = Toast.makeText(this, "儲存完成", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if(bd.getBoolean("delete"))
//        {
//            toast = Toast.makeText(this, "刪除完成", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if(bd.getBoolean("upload"))
//        {
//            toast = Toast.makeText(this, "上傳完成", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//        else if(bd.getBoolean("init failed"))
//        {
//            toast = Toast.makeText(this, "圖片載入失敗", Toast.LENGTH_SHORT);
//            toast.show();
//        }
    }
    private void sensorReset()
    {
        accelerometerValue = new float[3];
        gyroscopeValue = new float[3];
        megnetometerValue = new float[3];
    }
    private void listeners()
    {
        AnimatorSet animatorSet = new AnimatorSet();
        //ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(selectFAB, "scaleX", 0f, 1f); //6.0公版，暫時關閉此功能
        //ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(selectFAB, "scaleY", 0f, 1f); //6.0公版，暫時關閉此功能
        ObjectAnimator scaleUpX2 = ObjectAnimator.ofFloat(shotFAB, "scaleX", 0f, 1f);
        ObjectAnimator scaleUpY2 = ObjectAnimator.ofFloat(shotFAB, "scaleY", 0f, 1f);
        ObjectAnimator scaleUpX3 = ObjectAnimator.ofFloat(uploadFAB, "scaleX", 0f, 1f);
        ObjectAnimator scaleUpY3 = ObjectAnimator.ofFloat(uploadFAB, "scaleY", 0f, 1f);

        //animatorSet.play(scaleUpX).with(scaleUpY).with(scaleUpX2).with(scaleUpY2).with(scaleUpX3).with(scaleUpY3); //6.0公版，暫時關閉select功能
        animatorSet.play(scaleUpX2).with(scaleUpY2).with(scaleUpX3).with(scaleUpY3);
        animatorSet.setDuration(300);
        plusFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(/*selectFAB.getVisibility() == View.INVISIBLE && */shotFAB.getVisibility() == View.INVISIBLE && uploadFAB.getVisibility() == View.INVISIBLE)
                {
                    //selectFAB.setVisibility(View.VISIBLE); //6.0公版，暫時關閉此功能
                    shotFAB.setVisibility(View.VISIBLE);
                    uploadFAB.setVisibility(View.VISIBLE);
                    animatorSet.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {}
                        @Override
                        public void onAnimationEnd(Animator animation) {}
                        @Override
                        public void onAnimationCancel(Animator animation) {}
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                    });
                    animatorSet.start();
                    if(isNoPhoto) albumHintTV.setText("請點擊相機鍵開始拍照");
                }
                else
                {
                    //selectFAB.setVisibility(View.INVISIBLE); //6.0公版，暫時關閉此功能
                    shotFAB.setVisibility(View.INVISIBLE);
                    uploadFAB.setVisibility(View.INVISIBLE);
                    if(isNoPhoto) albumHintTV.setText("請點選右下角按鈕展開選單");
                }
            }
        });

        /*selectFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorReset(); //2.2 Add

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 3000);
                selectFAB.setVisibility(View.INVISIBLE);
                shotFAB.setVisibility(View.INVISIBLE);
                uploadFAB.setVisibility(View.INVISIBLE);
            }
        });*/  //6.0公版，暫時關閉此功能

        shotFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = timeStamp + ".jpg";
                String storagePath = "/storage/emulated/0/DCIM/PhotoInfoUploader/";
                File storageDir = new File(storagePath);
                if (!storageDir.exists()) storageDir.mkdirs();
                File photoFile = new File(storageDir, imageFileName);
                photoPath = photoFile.getPath(); //3.1 Remark  //3.2 Add
                exifReader.setPhotoPath(photoPath); //3.1 Add  //3.2 Modify
                if (photoFile != null) {
                    Uri photoUri = FileProvider.getUriForFile(AlbumActivity.this, "com.example.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    //selectFAB.setVisibility(View.INVISIBLE); //6.0公版，暫時關閉此功能
                    shotFAB.setVisibility(View.INVISIBLE);
                    uploadFAB.setVisibility(View.INVISIBLE);
                }
                Glide.get(getApplicationContext()).clearMemory();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 在後台線程中清除磁碟緩存
                        Glide.get(getApplicationContext()).clearDiskCache();
                    }
                }).start();
            }
        });

        uploadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //selectFAB.setVisibility(View.INVISIBLE); //6.0公版，暫時關閉此功能
                if(readAlbum().length()==0) return;
                new AlertDialog.Builder(AlbumActivity.this)
                        .setTitle("上傳").setMessage("是否要上傳整個相簿?")
                        .setPositiveButton("確定", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                shotFAB.setVisibility(View.INVISIBLE);
                                uploadFAB.setVisibility(View.INVISIBLE);
                                pb.setVisibility(View.VISIBLE);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try
                                        {
                                            File dirFile = getBaseContext().getFilesDir();
                                            String albumPath = dirFile.getAbsolutePath() + "/album_" + String.valueOf(albumID);
                                            File albumDir = new File(albumPath);
                                            String photoPathesFilePath = albumDir.getAbsolutePath() + "/photoPathes.json";
                                            String fileContent = storageTools.readFile(photoPathesFilePath);
                                            JSONObject mainObject = new JSONObject(fileContent);
                                            JSONArray photos = mainObject.getJSONArray("photos");

                                            final CountDownLatch latch = new CountDownLatch(photos.length());
                                            pb.setMax(photos.length());
                                            pb.setProgress(0);
                                            for(int i = 0; i < photos.length(); ++i)
                                            {
                                                final int progress = i;
                                                JSONObject photoObject = photos.getJSONObject(i);
                                                String uploadPhotoPath = photoObject.getString("photo path");
                                                uploader.uploadPhoto2Firebase(getBaseContext(), albumID, uploadPhotoPath, new Uploader.OnCompleteListener() {
                                                    @Override
                                                    public void onComplete(boolean success) {
                                                        latch.countDown();
                                                    }
                                                });
                                                pb.setProgress(i);
                                            }
                                            latch.await();

                                            storageTools.deleteAlbum(getBaseContext(), albumID);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    pb.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(getBaseContext(), "上傳完成!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            Intent showAct = new Intent(AlbumActivity.this, MainActivity.class);
                                            startActivity(showAct);
                                            finish();
                                        }
                                        catch (IOException|JSONException|InterruptedException e)
                                        {
                                            Toast.makeText(getBaseContext(), "檔案讀取失敗", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).start();
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
        });
        rcv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) rcv.getLayoutManager();
                int itemCount = layoutManager.getItemCount()/4;
                if (itemCount*110>screenHeight&&!recyclerView.canScrollVertically(1)) {
                    plusFAB.setVisibility(View.GONE);
                } else {
                    plusFAB.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void toPreviewActivity()
    {
        Intent intent = new Intent(AlbumActivity.this, PhotoActivity.class);
        bd.putInt("album ID", albumID);
        bd.putString("photo path", photoPath);
        bd.putFloatArray("accelerometer array", accelerometerValue);
        bd.putFloatArray("gyroscope array", gyroscopeValue);
        bd.putFloatArray("megnetometer array", megnetometerValue);
        bd.putBoolean("preview mode", true);
        intent.putExtras(bd);
        startActivity(intent);
        finish();
    }
    private Runnable viewAlbumRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };
    private JSONArray readAlbum()
    {
        JSONArray photos = new JSONArray();
        try
        {
            File dirFile = this.getFilesDir();
            String albumPath = dirFile.getAbsolutePath() + "/album_" + String.valueOf(albumID);
            File albumDir = new File(albumPath);
            if(!albumDir.exists()) return photos;
            String photoPathesFilePath = albumDir.getAbsolutePath() + "/photoPathes.json";
            File photoPathesFile = new File(photoPathesFilePath);
            if(!photoPathesFile.exists()) return photos;

            String fileContent = storageTools.readFile(photoPathesFilePath);
            JSONObject mainObject = new JSONObject(fileContent);
            photos = mainObject.getJSONArray("photos");
            for(int i = 0; i < photos.length(); ++i)
            {
                JSONObject photoObject = photos.getJSONObject(i);
                String photoPath = photoObject.getString("photo path");
                File photoFile = new File(photoPath);
                if(!photoFile.exists())
                {
                    photos.remove(i);
                    --i;
                }
            }
            /*Bitmap bitmap;
            for(int i=0;i<photoPathArray.length();++i)
            {
                String path = photoPathArray.getJSONObject(i).getString("photo path");
                File photoFile = new File(path);
                if(!photoFile.exists()) continue;
                bitmap = BitmapFactory.decodeFile(path);
                photos.add(bitmap);
            }*/
            return photos;
        }
        catch (JSONException | IOException e)
        {
            return photos;
        }
    }
    private String readAlbumName()
    {
        try
        {
            File crtDir = this.getFilesDir();
            File[] fileList = crtDir.listFiles();
            for(File file : fileList)
            {
                if (!file.isDirectory()) continue;
                String fileName = file.getName();
                int dirId = Integer.parseInt(fileName.substring(6));
                if(dirId!=albumID) continue;
                String photoPathesFilePath = file.getAbsolutePath() + "/photoPathes.json";
                File photoPathesFile = new File(photoPathesFilePath);
                if(!photoPathesFile.exists()) return "None";
                String fileContent = storageTools.readFile(photoPathesFilePath);
                JSONObject mainObject = new JSONObject(fileContent);
                return mainObject.getString("album name");
            }
            return "None";
        }
        catch (JSONException|IOException e)
        {
            return "None";
        }
    }
    private void requestLocationUpdate()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0f, locationListener);
            provider_exist = true;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1.0f, locationListener);
            provider_exist = true;
        }
    }
    private Location getLocation()
    {
        if(!provider_exist) requestLocationUpdate();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location==null)
        {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return location;
    }
    public static String getFilePathFromContentUri(Uri fileUri, ContentResolver contentResolver)
    {
        String filePath;

        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(fileUri, filePathColumn, null, null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

        filePath = cursor.getString(columnIndex);

        cursor.close();

        return filePath;
    }
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            Intent showAct = new Intent(AlbumActivity.this, MainActivity.class);
            startActivity(showAct);
            finish();
        }
        return true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解除位置監聽器的註冊
        sensorDataReader.stopListening();
        locationManager.removeUpdates(locationListener);
    }
}