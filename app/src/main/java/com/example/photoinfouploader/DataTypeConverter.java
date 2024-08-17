package com.example.photoinfouploader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataTypeConverter {

    private EXIFReader exifReader;
    private HashMap<String, String> exifInfo;

    public DataTypeConverter()
    {
        exifReader = new EXIFReader();
    }

    public String photoToBase64(String photoPath)
    {
        try {
            FileInputStream fileInputStream = new FileInputStream(photoPath);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byte[] photoBytes = byteArrayOutputStream.toByteArray();

            //photo_base64 = Base64.encodeToString(photoBytes, Base64.DEFAULT);
            return Base64.encodeToString(photoBytes, Base64.DEFAULT);
        } catch (IOException e) {
            return "None";
        }
    }
    public Map<String, Object> getUploadMap4Firebase(String photoPath, JSONObject weatherObject, JSONObject orientationObject, String locationProvider)
    {
        Map<String, Object> uploadMap = new HashMap<String, Object>();
        try {
            exifReader.setPhotoPath(photoPath);
            exifInfo = exifReader.getEXIF();
            int orientation = Integer.parseInt(exifInfo.get("exifOrient"));
            String datetime = formatDateTime(exifInfo.get("exifDatetime"));
            String maker = exifInfo.get("exifMaker");
            String model = exifInfo.get("exifModel");
            int flash = Integer.parseInt(exifInfo.get("exifFlash"));
            int image_length = Integer.parseInt(exifInfo.get("exifImgLen"));
            int image_width = Integer.parseInt(exifInfo.get("exifImgWid"));
            float exposure_time = Float.parseFloat(exifInfo.get("exifExposure"));
            float aperture = Float.parseFloat(exifInfo.get("exifAperture"));
            int iso = Integer.parseInt(exifInfo.get("exifISO"));
            int white_balance = Integer.parseInt(exifInfo.get("exifWB"));
            float focal_length = getFocalLenFlt(exifInfo.get("exifFocalLen"));
            float longitude = Float.parseFloat(exifInfo.get("exifGPSLONG"));
            String longitude_round = String.valueOf(Math.round(longitude*1000)*0.001);
            float latitude = Float.parseFloat(exifInfo.get("exifGPSLAT"));

            uploadMap.put("orientation", orientation);
            uploadMap.put("datetime", datetime);
            uploadMap.put("maker", maker);
            uploadMap.put("model", model);
            uploadMap.put("flash", flash);
            uploadMap.put("image_length", image_length);
            uploadMap.put("image_width", image_width);
            uploadMap.put("exposure_time", exposure_time);
            uploadMap.put("aperture", aperture);
            uploadMap.put("iso", iso);
            uploadMap.put("white_balance", white_balance);
            uploadMap.put("focal_length", focal_length);
            uploadMap.put("longitude", longitude);
            uploadMap.put("longitude_round", longitude_round);
            uploadMap.put("latitude", latitude);
            uploadMap.put("location_provider", locationProvider);

            float wind_direction = Float.parseFloat(weatherObject.getString("windDirection"));
            float wind_speed = Float.parseFloat(weatherObject.getString("windSpeed"));
            float temperature = Float.parseFloat(weatherObject.getString("temperature"));
            int humidity = Integer.parseInt(weatherObject.getString("humidity"));
            float pressure = Float.parseFloat(weatherObject.getString("pressure"));
            float precipitation = Float.parseFloat(weatherObject.getString("dayRain"));
            float gust_speed = Float.parseFloat(weatherObject.getString("gustSpeed"));
            float gust_direction = Float.parseFloat(weatherObject.getString("gustDirection"));

            uploadMap.put("station_name", weatherObject.getString("locationName"));
            uploadMap.put("wind_direction", wind_direction);
            uploadMap.put("wind_speed", wind_speed);
            uploadMap.put("temperature", temperature);
            uploadMap.put("humidity", humidity);
            uploadMap.put("pressure", pressure);
            uploadMap.put("precipitation", precipitation);
            uploadMap.put("gust_speed", gust_speed);
            uploadMap.put("gust_direction", gust_direction);
            uploadMap.put("weather", weatherObject.getString("weather"));

            uploadMap.put("pitch", orientationObject.getDouble("pitch"));
            uploadMap.put("roll", orientationObject.getDouble("roll"));
            uploadMap.put("yaw", orientationObject.getDouble("yaw"));

            return uploadMap;
        }catch (JSONException e)
        {
            return uploadMap;
        }
    }
    public JSONObject getUploadJsonObjectForMySQL(String photoPath, JSONObject weatherObject, JSONObject orientationObject, String locationProvider)
    {
        JSONObject uploadObject = new JSONObject();
        try {
            String photo_base64 = photoToBase64(photoPath);
            uploadObject.put("photo_base64", photo_base64);

            exifReader.setPhotoPath(photoPath);
            exifInfo = exifReader.getEXIF();
            int orientation = Integer.parseInt(exifInfo.get("exifOrient"));
            String datetime = formatDateTime(exifInfo.get("exifDatetime"));
            String maker = exifInfo.get("exifMaker");
            String model = exifInfo.get("exifModel");
            int flash = Integer.parseInt(exifInfo.get("exifFlash"));
            int image_length = Integer.parseInt(exifInfo.get("exifImgLen"));
            int image_width = Integer.parseInt(exifInfo.get("exifImgWid"));
            float exposure_time = Float.parseFloat(exifInfo.get("exifExposure"));
            float aperture = Float.parseFloat(exifInfo.get("exifAperture"));
            int iso = Integer.parseInt(exifInfo.get("exifISO"));
            int white_balance = Integer.parseInt(exifInfo.get("exifWB"));
            float focal_length = getFocalLenFlt(exifInfo.get("exifFocalLen"));
            float longitude = Float.parseFloat(exifInfo.get("exifGPSLONG"));
            float latitude = Float.parseFloat(exifInfo.get("exifGPSLAT"));

            uploadObject.put("orientation", orientation);
            uploadObject.put("datetime", datetime);
            uploadObject.put("maker", maker);
            uploadObject.put("model", model);
            uploadObject.put("flash", flash);
            uploadObject.put("image_length", image_length);
            uploadObject.put("image_width", image_width);
            uploadObject.put("exposure_time", exposure_time);
            uploadObject.put("aperture", aperture);
            uploadObject.put("iso", iso);
            uploadObject.put("white_balance", white_balance);
            uploadObject.put("focal_length", focal_length);
            uploadObject.put("longitude", longitude);
            uploadObject.put("latitude", latitude);
            uploadObject.put("location_provider", locationProvider);

            float wind_direction = Float.parseFloat(weatherObject.getString("windDirection"));
            float wind_speed = Float.parseFloat(weatherObject.getString("windSpeed"));
            float temperature = Float.parseFloat(weatherObject.getString("temperature"));
            int humidity = Integer.parseInt(weatherObject.getString("humidity"));
            float pressure = Float.parseFloat(weatherObject.getString("pressure"));
            float precipitation = Float.parseFloat(weatherObject.getString("dayRain"));
            float gust_speed = Float.parseFloat(weatherObject.getString("gustSpeed"));
            float gust_direction = Float.parseFloat(weatherObject.getString("gustDirection"));

            uploadObject.put("station_name", weatherObject.getString("locationName"));
            uploadObject.put("wind_direction", wind_direction);
            uploadObject.put("wind_speed", wind_speed);
            uploadObject.put("temperature", temperature);
            uploadObject.put("humidity", humidity);
            uploadObject.put("pressure", pressure);
            uploadObject.put("precipitation", precipitation);
            uploadObject.put("gust_speed", gust_speed);
            uploadObject.put("gust_direction", gust_direction);
            uploadObject.put("weather", weatherObject.getString("weather"));

            uploadObject.put("pitch", orientationObject.getDouble("pitch"));
            uploadObject.put("roll", orientationObject.getDouble("roll"));
            uploadObject.put("yaw", orientationObject.getDouble("yaw"));

            return uploadObject;
        }catch (JSONException e)
        {
            return uploadObject;
        }
    }

    public void photoCompress(String photoPath)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);

        int targetHeight = 1080;
        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        int targetWidth = (int)(bitmapWidth / bitmapHeight * 1080.0);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        File file = new File(photoPath);
        if(file.exists()) file.delete();

        saveBitmapToFile(resizedBitmap, photoPath);
    }

    private void saveBitmapToFile(Bitmap bitmap, String filePath) {
        File file = new File(filePath);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private float getFocalLenFlt(String focalLenStr)
    {
        int idx_slash = focalLenStr.indexOf("/");
        float up = Float.parseFloat(focalLenStr.substring(0, idx_slash));
        float btm = Float.parseFloat(focalLenStr.substring(idx_slash+1, focalLenStr.length()));
        float focalLenFlt = up/btm;
        return focalLenFlt;
    }

    private String formatDateTime(String dateTime)
    {
        try {
            DateFormat inputFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            Date date = inputFormat.parse(dateTime);

            DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
