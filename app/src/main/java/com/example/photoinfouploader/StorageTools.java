package com.example.photoinfouploader;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class StorageTools {

    private String photo_base64;
    private HashMap<String, String> exifInfo;
    private HashMap<String, String> weatherInfo;
    private float[] orientationArray;
    private EXIFReader exifReader;
    private String locationProvider;
    //private WeatherAPICaller weatherAPICaller;

    public StorageTools()
    {
        photo_base64 = "";
        exifInfo = new HashMap<>();
        weatherInfo = new HashMap<>();

        orientationArray = new float[3];

        exifReader = new EXIFReader();
        //weatherAPICaller = new WeatherAPICaller();
    }
    private void reset()
    {
        photo_base64 = "";
        exifInfo.clear();
        weatherInfo.clear();

        orientationArray = new float[3];
    }
    private boolean createJsonFile(String dirPath, String fileName, String jsonContent)
    {
        try {
            File file = new File(dirPath,fileName + ".json");
            FileWriter fileWriter = new FileWriter(file.getPath());
            fileWriter.write(jsonContent);
            fileWriter.close();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
    public int createAlbum(Context context, String albumName)
    {
        try {
            int newAlbumId = -1;

            File crtDir = context.getFilesDir();
            File[] fileList = crtDir.listFiles();
            if(fileList!=null)
            {
                for(File file:fileList)
                {
                    if(!file.isDirectory()) continue;
                    String fileName = file.getName();
                    int dirId = Integer.parseInt(fileName.substring(6));
                    if(dirId>newAlbumId)
                    {
                        newAlbumId = dirId;
                    }
                }
                newAlbumId += 1;
                String newDirPath = crtDir.getAbsolutePath() + "/album_" + String.valueOf(newAlbumId);
                File newDir = new File(newDirPath);
                if(!newDir.exists())
                {
                    if(newDir.mkdir())
                    {
                        if(createPhotoPathesFile(newDirPath, albumName))
                        {
                            return newAlbumId;
                        }
                    }
                }
            }
            return -1;
        }catch (Exception e)
        {
            return -1;
        }
    }
    private boolean deleteFiles(File directory)
    {
        try {
            File[] fileList = directory.listFiles();
            for(File file:fileList)
            {
                if(file.isFile()) file.delete();
                if(file.isDirectory())
                {
                    if(!deleteFiles(file))
                    {
                        return false;
                    }
                }
            }
            directory.delete();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
    public boolean deleteAlbum(Context context, int albumId)
    {
        try {
            File crtDir = context.getFilesDir();
            File[] fileList = crtDir.listFiles();
            for(File file:fileList)
            {
                if(!file.isDirectory()) continue;
                String fileName = file.getName();
                if(Integer.parseInt(fileName.substring(6))!=albumId) continue;
                JSONObject mainObject = new JSONObject(readFile(file.getAbsolutePath() + "/photoPathes.json"));
                if(mainObject.has("photos"))
                {
                    JSONArray photoArray = mainObject.getJSONArray("photos");
                    for(int i = 0; i < photoArray.length(); ++i)
                    {
                        JSONObject photoObject = photoArray.getJSONObject(i);
                        String photoPath = photoObject.getString("photo path");
                        File photoFile = new File(photoPath);
                        if(photoFile.exists()) photoFile.delete();
                    }
                }
                return deleteFiles(file);
            }
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }
    public void deletePhoto(Context context, int albumId, String photoPath) throws IOException, JSONException
    {
        File crtDir = context.getFilesDir();
        File[] fileList = crtDir.listFiles();
        String photoPathesFilePath = null;
        File albumDir = null;
        for(File file:fileList)
        {
            if(!file.isDirectory()) continue;
            String fileName = file.getName();
            if(Integer.parseInt(fileName.substring(6))!=albumId) continue;
            albumDir = file;
            photoPathesFilePath = file.getAbsolutePath() + "/photoPathes.json";
            break;
        }
        JSONObject mainObject = new JSONObject(readFile(photoPathesFilePath));
        String albumName = mainObject.getString("album name");
        JSONArray photoArray;
        photoArray = mainObject.getJSONArray("photos");
        mainObject = new JSONObject();
        mainObject.put("album name", albumName);

        for(int i = 0; i < photoArray.length(); ++i)
        {
            JSONObject photoObject = photoArray.getJSONObject(i);
            String jsonPhotoPath = photoObject.getString("photo path");
            if(jsonPhotoPath.equals(photoPath))
            {
                photoArray.remove(i);
                break;
            }
        }

        mainObject.put("photos", photoArray);
        createJsonFile(albumDir.getAbsolutePath(), "photoPathes", mainObject.toString());
        File photoFile = new File(photoPath);
        if(photoFile.exists()) photoFile.delete();
    }
    private boolean createPhotoPathesFile(String albumPath, String albumName)
    {
        try {
            JSONObject mainObject = new JSONObject();

            mainObject.put("album name", albumName);

            return createJsonFile(albumPath, "photoPathes", mainObject.toString());
        }
        catch (JSONException e) {
            return false;
        }
    }
//    public String photoToBase64(String photoPath)
//    {
//        try {
//            FileInputStream fileInputStream = new FileInputStream(photoPath);
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//            int bufferSize = 1024;
//            byte[] buffer = new byte[bufferSize];
//            int bytesRead;
//
//            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
//                byteArrayOutputStream.write(buffer, 0, bytesRead);
//            }
//
//            byte[] photoBytes = byteArrayOutputStream.toByteArray();
//
//            //photo_base64 = Base64.encodeToString(photoBytes, Base64.DEFAULT);
//            return Base64.encodeToString(photoBytes, Base64.DEFAULT);
//        } catch (IOException e) {
//            return "None";
//        }
//    }
    public String readFile(String filePath) throws IOException
    {
        FileInputStream inputStream = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
    }
//    private String createJsonString()
//    {
//        try {
//            JSONObject mainObject = new JSONObject();
//
//            mainObject.put("photo", photo_base64);
//
//            JSONObject exifObject = new JSONObject();
//            for (Map.Entry<String, String> entry : exifInfo.entrySet())
//            {
//                exifObject.put(entry.getKey(), entry.getValue());
//            }
//            mainObject.put("exifInfo", exifObject);
//
//            JSONObject weatherObject = new JSONObject();
//            for (Map.Entry<String, String> entry : weatherInfo.entrySet())
//            {
//                weatherObject.put(entry.getKey(), entry.getValue());
//            }
//            mainObject.put("weatherInfo", weatherObject);
//
//            /*JSONObject sensorObject = new JSONObject();
//            JSONArray accelerometerArray = new JSONArray();
//            JSONArray gyroscopeArray = new JSONArray();
//            JSONArray megnetometerArray = new JSONArray();
//            for(int i = 0; i < 3; ++i)
//            {
//                accelerometerArray.put(accelerometerValue[i]);
//                gyroscopeArray.put(gyroscopeValue[i]);
//                megnetometerArray.put(megnetometerValue[i]);
//            }
//            sensorObject.put("accelerometer", accelerometerArray);
//            sensorObject.put("gyroscope", gyroscopeArray);
//            sensorObject.put("megnetometer", megnetometerArray);
//            mainObject.put("sensorInfo", sensorObject);*/
//            JSONObject orientationObject = new JSONObject();
//            orientationObject.put("pitch", orientationArray[1]);
//            orientationObject.put("roll", orientationArray[2]);
//            orientationObject.put("yaw", orientationArray[0]);
//            mainObject.put("orientation info", orientationObject);
//
//            return mainObject.toString();
//        } catch (JSONException e) {
//            return "None";
//        }
//    }

    private String storePhotoPath(String albumPath, String photoPath) throws IOException, JSONException
    {
        //讀取原始JSON資料
        String photoPathesFilePath = albumPath + "/photoPathes.json";
        String jsonStr = readFile(photoPathesFilePath);
        JSONObject mainObject = new JSONObject(jsonStr);
        String albumName = mainObject.getString("album name");
        JSONArray photoArray;
        if(mainObject.has("photos"))
        {
            photoArray = mainObject.getJSONArray("photos");
        }
        else
        {
            photoArray = new JSONArray();
        }

        //更新JSON資料
        mainObject = new JSONObject();
        mainObject.put("album name", albumName);

        JSONObject photoObject = new JSONObject();
        photoObject.put("photo path", photoPath);

            /*JSONObject exifObject = new JSONObject();
            for (Map.Entry<String, String> entry : exifInfo.entrySet())
            {
                exifObject.put(entry.getKey(), entry.getValue());
            }
            photoObject.put("exifInfo", exifObject);*/ //5.0 delete 改為上傳時再讀取EXIF

        JSONObject weatherObject = new JSONObject();
        for (Map.Entry<String, String> entry : weatherInfo.entrySet())
        {
            weatherObject.put(entry.getKey(), entry.getValue());
        }
        photoObject.put("weatherInfo", weatherObject);

        JSONObject orientationObject = new JSONObject();
        orientationObject.put("pitch", orientationArray[1]);
        orientationObject.put("roll", orientationArray[2]);
        orientationObject.put("yaw", orientationArray[0]);
        photoObject.put("orientation info", orientationObject);

        photoObject.put("location_provider", locationProvider);

        photoArray.put(photoObject);
        mainObject.put("photos", photoArray);

        return mainObject.toString();
    }
    private String getAlbumPath(File dir, int albumId)
    {
        File[] fileList = dir.listFiles();
        if(fileList!=null)
        {
            for(File file:fileList)
            {
                if(!file.isDirectory()) continue;
                String fileName = file.getName();
                int dirId = Integer.parseInt(fileName.substring(6));
                if(dirId==albumId)
                {
                    return file.getAbsolutePath();
                }
            }
        }
        String newDirPath = dir.getAbsolutePath() + "/album_" + String.valueOf(albumId);
        File newDir = new File(newDirPath);
        if(!newDir.exists())
        {
            if(newDir.mkdir())
            {
                return newDirPath;
            }
        }
        return "None";
    }
    public int save(Context context,String photoPath, int albumId, float[] orientationArray, HashMap<String, String> weatherInfo, String locationProvider) throws JSONException, IOException //5.0 modify string to int
    {
        if(photoPath != "")
        {
                /*photo_base64 = photoToBase64(photoPath);
                if(photo_base64=="None") return "儲存失敗";*/ //5.0 delete
            exifReader.setPhotoPath(photoPath);
            exifInfo = exifReader.getEXIF();
            if(exifInfo.get("exifGPSLAT")==null||exifInfo.get("exifGPSLONG")==null) return 1; //這張照片沒有儲存地點 5.0 modify string to int
            Double latitude = Double.parseDouble(exifInfo.get("exifGPSLAT"));
            Double longitude = Double.parseDouble(exifInfo.get("exifGPSLONG"));
            //weatherInfo = weatherAPICaller.getWeatherInfo(latitude, longitude, exifInfo.get("exifDatetime"));
            this.weatherInfo = weatherInfo;
            this.orientationArray = orientationArray;
            this.locationProvider = locationProvider;
        }

            /*if(photo_base64 == "")
            {
                reset();
                return "儲存失敗";
            }*///5.0 delete
        if(exifInfo.isEmpty()) //5.0 modify
        {
            reset();
            return 2; //這張照片沒有EXIF資訊 5.0 modify string to int
        }
        else if(weatherInfo.isEmpty())
        {
            reset();
            return 3; //天氣資訊已刪除 5.0 modify string to int
        }
        else
        {
            String albumPath = getAlbumPath(context.getFilesDir(), albumId);
            if(albumPath == "None") return 4; //找不到相簿路徑;
            //String jsonStr = createJsonString(); //5.0 delete
            String jsonStr = storePhotoPath(albumPath, photoPath); //5.0 modify

                /*String fileName = "album_" + String.valueOf(albumId) + "_" + exifInfo.get("exifDatetime").split(" ")[0].replace(':','-')+'_'+exifInfo.get("exifDatetime").split(" ")[1].replace(":","");
                String fileName2 = "photoPathes";
                File file = new File(albumPath,fileName + ".json");
                File file2 = new File(albumPath, fileName2 + ".json");
                FileWriter fileWriter = new FileWriter(file.getPath());
                fileWriter.write(jsonStr);
                fileWriter.close();
                fileWriter = new FileWriter(file2.getPath());
                fileWriter.write(jsonStr2);
                fileWriter.close();*/ //5.0 delete

            String fileName = "photoPathes";
            File file = new File(albumPath, fileName + ".json");
            FileWriter fileWriter = new FileWriter(file.getPath());
            fileWriter.write(jsonStr);
            fileWriter.close();

            reset();
            return 0; //儲存完成 5.0 modify string to int
        }
    }
}
