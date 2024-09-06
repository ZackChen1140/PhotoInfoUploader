package com.example.photoinfouploader;

import android.content.Context;
import android.location.Location;
import android.media.ExifInterface;

import java.io.IOException;
import java.util.HashMap;

public class EXIFReader {

    private ExifInterface exifInterface;
    private String photoPath;
    private HashMap<String, String> exifInfo;
    public EXIFReader()
    {
        exifInfo = new HashMap<>();
    }

    public HashMap<String, String> getEXIF()
    {
        try
        {
            exifInterface = new ExifInterface(photoPath);
            String exifLAT = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String exifLATR = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String exifLONG = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String exifLONGR = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            if(exifLAT==null||exifLATR==null||exifLONG==null||exifLONGR==null) //經緯度
            {
                exifInfo.put("exifGPSLAT", null);
                exifInfo.put("exifGPSLONG", null);
            }
            else
            {
                exifInfo.put("exifGPSLAT", String.valueOf(transferLocation(exifLAT, exifLATR)));
                exifInfo.put("exifGPSLONG", String.valueOf(transferLocation(exifLONG, exifLONGR)));
            }
            //5.0 Modify 配合資料庫上傳
            //exifInfo.put("exifOrient", getShotDirection(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION))); //旋轉角度
            exifInfo.put("exifOrient", exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)); //旋轉角度
            //5.0 Modify 配合資料庫上傳
            exifInfo.put("exifDatetime", exifInterface.getAttribute(ExifInterface.TAG_DATETIME)); //拍攝時間
            exifInfo.put("exifMaker", exifInterface.getAttribute(ExifInterface.TAG_MAKE)); //設備品牌
            exifInfo.put("exifModel", exifInterface.getAttribute(ExifInterface.TAG_MODEL)); //設備型號
            exifInfo.put("exifFlash", exifInterface.getAttribute(ExifInterface.TAG_FLASH)); //閃光燈
            exifInfo.put("exifImgLen", exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)); //圖片長
            exifInfo.put("exifImgWid", exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)); //圖片寬
            exifInfo.put("exifExposure", exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)); //曝光時間
            exifInfo.put("exifAperture", exifInterface.getAttribute(ExifInterface.TAG_APERTURE)); //光圈值
            exifInfo.put("exifISO", exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)); //感光度
            exifInfo.put("exifWB", exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE)); //白平衡
            exifInfo.put("exifFocalLen", exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)); //焦距
            //String exifWB = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE); //白平衡  //5.0 delete 配合資料庫上傳
            //if(exifInfo.get("exifExposure").length()>6) exifInfo.put("exifExposure", exifInfo.get("exifExposure").substring(0,6));
            //if(exifInfo.get("exifWB").equals("0")) exifInfo.put("exifWB", "Auto"); //5.0 delete 配合資料庫上傳
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return exifInfo;
    }

    public void addGPSinfo(Location location) {
        try {
            exifInterface = new ExifInterface(photoPath);

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            // 設置經度和緯度
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPSConvertBack(latitude));
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPSConvertBack(longitude));

            // 設置經度和緯度方向
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude >= 0 ? "N" : "S");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude >= 0 ? "E" : "W");


            exifInterface.saveAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPhotoOrientation(String photoPath)
    {
        try
        {
            exifInterface = new ExifInterface(photoPath);
            String orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            return orientation;
        }
        catch (IOException e)
        {
            return "";
        }
    }

    private static String GPSConvertBack(double value) {
        value = Math.abs(value);
        int degrees = (int) value;
        value = (value - degrees) * 60;
        int minutes = (int) value;
        value = (value - minutes) * 60;
        int seconds = (int) value;

        return degrees + "/1," + minutes + "/1," + seconds + "/1";
    }

    public void setPhotoPath(String path)
    {
        photoPath = path;
    }

    private float transferLocation(String relationalStr, String ref)
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

    public String getShotDirection(String exifOrient)
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
}
