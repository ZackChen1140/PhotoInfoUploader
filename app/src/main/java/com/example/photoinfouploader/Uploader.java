package com.example.photoinfouploader;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Uploader {

    private StorageTools storageTools;
    private DataTypeConverter dataTypeConverter;
    private String uploadStr;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private String result;
    //private boolean mutex;

    public Uploader()
    {
        storageTools = new StorageTools();
        dataTypeConverter = new DataTypeConverter();
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void uploadPhoto2Firebase(final Context context, final int albumID, final String photoPath, final OnCompleteListener onCompleteListener)
    {
        try {
            //找照片資訊
            File photoFile = new File(photoPath);
            if(!photoFile.exists())
            {
                onCompleteListener.onComplete(false);
                return;
            }

            File dirFile = context.getFilesDir();
            String albumPath = dirFile.getAbsolutePath() + "/album_" + String.valueOf(albumID);
            File albumDir = new File(albumPath);
            if(!albumDir.exists())
            {
                onCompleteListener.onComplete(false);
                return;
            }
            String photoPathesFilePath = albumDir.getAbsolutePath() + "/photoPathes.json";
            File photoPathesFile = new File(photoPathesFilePath);
            if(!photoPathesFile.exists())
            {
                onCompleteListener.onComplete(false);
                return;
            }

            String fileContent = storageTools.readFile(photoPathesFilePath);
            if(fileContent.equals("None"))
            {
                onCompleteListener.onComplete(false);
                return;
            }
            JSONObject mainObject = new JSONObject(fileContent);
            JSONArray photos = mainObject.getJSONArray("photos");
            //JSONObject exifObject = new JSONObject(); //5.0 local JSON 刪除此資料
            JSONObject weatherObject = new JSONObject();
            JSONObject orientationObject = new JSONObject();
            String locationProvider = new String();
            boolean fileExisted = false;

            for(int i = 0; i < photos.length(); ++i)
            {
                JSONObject photoObject = photos.getJSONObject(i);
                String targetPhotoPath = photoObject.getString("photo path");
                if(!targetPhotoPath.equals(photoPath)) continue;
                //exifObject = photoObject.getJSONObject("exifInfo");
                weatherObject = photoObject.getJSONObject("weatherInfo");
                orientationObject = photoObject.getJSONObject("orientation info");
                locationProvider = photoObject.getString("location_provider");
                fileExisted = true;
                break;
            }
            if(!fileExisted)
            {
                onCompleteListener.onComplete(false);
                return;
            }

            //取得上傳Map
            final Map<String, Object> convertMap = dataTypeConverter.getUploadMap4Firebase(photoPath, weatherObject, orientationObject, locationProvider);
            //壓縮原始照片
            dataTypeConverter.photoCompress(photoPath);
            //上傳照片至Firebase Storage
            StorageReference storageRef = storage.getReference();
            Uri file = Uri.fromFile(new File(photoPath));
            StorageReference riversRef = storageRef.child("cameraless_photos/" + file.getLastPathSegment());
            UploadTask uploadTask = riversRef.putFile(file);
            uploadTask.addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful())
                    {
                        storageRef.child("cameraless_photos/" + file.getLastPathSegment()).getDownloadUrl()
                        .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task)
                            {
                                if (task.isSuccessful())
                                {
                                    String downloadUrl = task.getResult().toString();
                                    //取得上傳Map
                                    Map<String, Object> UploadMap = convertMap;
                                    UploadMap.put("photo_url", downloadUrl);
                                    //上傳照片資訊至Firestore
                                    db.collection("cameraless_photography_db").add(UploadMap)
                                    .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful())
                                            {
                                                Log.d("結果:", "資料上傳成功，文件 ID:" + task.getResult().getId());
                                                onCompleteListener.onComplete(true);
                                            }
                                            else
                                            {
                                                storageRef.child("cameraless_photos/" + file.getLastPathSegment()).delete()
                                                .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        onCompleteListener.onComplete(false);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    storageRef.child("cameraless_photos/" + file.getLastPathSegment()).delete()
                                    .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            onCompleteListener.onComplete(false);
                                        }
                                    });
                                }
                            }
                        });
                    }
                    else
                    {
                        onCompleteListener.onComplete(false);
                    }
                }
            });
        }
        catch (IOException|JSONException e) {
            onCompleteListener.onComplete(false);
        }
    }

    public boolean uploadPhoto(Context context, int albumID, String photoPath)
    {
        try {
            File photoFile = new File(photoPath);
            if(!photoFile.exists()) return false;

            File dirFile = context.getFilesDir();
            String albumPath = dirFile.getAbsolutePath() + "/album_" + String.valueOf(albumID);
            File albumDir = new File(albumPath);
            if(!albumDir.exists()) return false;
            String photoPathesFilePath = albumDir.getAbsolutePath() + "/photoPathes.json";
            File photoPathesFile = new File(photoPathesFilePath);
            if(!photoPathesFile.exists()) return false;

            String fileContent = storageTools.readFile(photoPathesFilePath);
            JSONObject mainObject = new JSONObject(fileContent);
            JSONArray photos = mainObject.getJSONArray("photos");
            //JSONObject exifObject = new JSONObject(); //5.0 local JSON 刪除此資料
            JSONObject weatherObject = new JSONObject();
            JSONObject orientationObject = new JSONObject();
            String locationProvider = new String();
            boolean fileExisted = false;

            for(int i = 0; i < photos.length(); ++i)
            {
                JSONObject photoObject = photos.getJSONObject(i);
                String targetPhotoPath = photoObject.getString("photo path");
                if(!targetPhotoPath.equals(photoPath)) continue;
                //exifObject = photoObject.getJSONObject("exifInfo");
                weatherObject = photoObject.getJSONObject("weatherInfo");
                orientationObject = photoObject.getJSONObject("orientation info");
                locationProvider = photoObject.getString("location_provider");
                fileExisted = true;
                break;
            }
            if(!fileExisted) return false;

            JSONObject uploadObject = dataTypeConverter.getUploadJsonObjectForMySQL(photoPath, weatherObject, orientationObject, locationProvider);
            if(uploadObject == (new JSONObject())) return false;

            uploadStr = uploadObject.toString();
            //mutex = true;
            Thread thread = new Thread(uploadThread);
            thread.start();
            //while(mutex);
            return true;
        }
        catch (IOException | JSONException e) {
            return false;
        }
    }

    private Runnable uploadThread = new Runnable(){
        public void run() {
            try {
                String serverUrl = "http://192.168.0.49/UploadData.php";
                //String serverUrl = "http://192.168.0.100/UploadData.php";

                // 建立URL物件
                URL url = new URL(serverUrl);

                // 建立HttpURLConnection物件
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // 設定請求方法為POST
                urlConnection.setRequestMethod("POST");

                // 啟用輸出流，用於上傳資料
                urlConnection.setDoOutput(true);

                // 取得OutputStream
                OutputStream outputStream = urlConnection.getOutputStream();

                // 使用BufferedWriter寫入資料
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(uploadStr);

                // 關閉資料流
                writer.flush();
                writer.close();
                outputStream.close();

                // 取得伺服器回應碼
                int responseCode = urlConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    result = response.toString();
                }
                else {
                    result = "failed";
                }
                //mutex = false;

            } catch(Exception e) {
                result = e.toString(); // 如果出事，回傳錯誤訊息
                //mutex = false;
            }
        }
    };
    public interface OnCompleteListener {
        void onComplete(boolean success);
    }
}
