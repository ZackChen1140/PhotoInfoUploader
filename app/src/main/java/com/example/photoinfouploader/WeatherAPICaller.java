//3.0 天氣資訊取得程式碼重構(搬來這裡)
package com.example.photoinfouploader;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class WeatherAPICaller {

    private String api = "https://opendata.cwa.gov.tw/historyapi/v1/getMetadata/O-A0001-001?Authorization=rdec-key-123-45678-011121314";
    private String allDayInfo;
    private String url_hour;
    private String time;
    //private boolean mutex;
    private boolean getUrlDayCompleted;
    private boolean getDataCompleted;
    private Double latitude;
    private Double longitude;
    private HashMap<String,String> weatherInfo;


    public WeatherAPICaller(Double sLatitude,Double sLongitude, String sTime)
    {
        getUrlDayCompleted = false;
        getDataCompleted = false;

        weatherInfo = new HashMap<>();
        latitude = sLatitude;
        longitude = sLongitude;
        time = sTime;

        new Thread(runnable_getUrlDay).start();
        new Thread(runnable_getData).start();
    }

    public boolean isGetUrlDayCompleted()
    {
        return getUrlDayCompleted;
    }

    public boolean isGetDataCompleted()
    {
        return getDataCompleted;
    }
    public String getUrl_hour()
    {
        return url_hour;
    }

    public HashMap<String, String> getWeatherInfo(/*Double sLatitude,Double sLongitude, String sTime*/)
    {
//        latitude = sLatitude;
//        longitude = sLongitude;
//        time = sTime;
//        url_hour = getUrlHour(time);
//        mutex = true;
//        new Thread(runnable_getData).start();
//        while(mutex);

        return weatherInfo;
    }

    private String getUrlHour(String time)
    {
        String rExifDate = time.substring(0,10).replace(':','-'); //2.2 Add
        String rExifTime = rExifDate + " " + time.substring(11,14)+"00:00"; //2.2 Modify
        try {
            JSONObject JSONob = new JSONObject(allDayInfo);
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
            return "None";
        }
    }

    private Runnable runnable_getUrlDay = new Runnable() {
        @Override
        public void run() {
            try {
                String NetUrl = api;
                InputStream is = new URL(NetUrl).openStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line = rd.readLine();
                while (line != null)
                {
                    sb.append(line);
                    line = rd.readLine();
                }
                if(sb.length()!=0) allDayInfo = sb.toString();
                url_hour = getUrlHour(time);
                getUrlDayCompleted = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable runnable_getData = new Runnable() {
        @Override
        public void run() {
            try {
                while(!getUrlDayCompleted);
                if(url_hour == "None")
                {
                    getDataCompleted = true;
                    return;
                }
                Connection conn = Jsoup.connect(url_hour);
                conn.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/    20100101 Firefox/32.0");
                final Document docs = conn.get();

                int closestIndex = 0;
                Double minDistance=Double.MAX_VALUE;

                /*Elements locElements = docs.select("location");
                for (int i = 0; i < locElements.size(); ++i)
                {
                    Element locElement = locElements.get(i);
                    Double lat = Double.parseDouble(locElement.select("lat").get(0).text());
                    Double lon = Double.parseDouble(locElement.select("lon").get(0).text());
                    Double distance = computeDistance(lat, lon, latitude, longitude);

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

                weatherInfo.put("locationName", targetElement.select("locationName").get(0).text());
                weatherInfo.put("windDirection", getWindDirection(weatherElements.get(1).select("elementValue").get(0).text()));
                weatherInfo.put("windSpeed", weatherElements.get(2).select("elementValue").get(0).text());
                weatherInfo.put("temperature", weatherElements.get(3).select("elementValue").get(0).text());
                weatherInfo.put("humidity", weatherElements.get(4).select("elementValue").get(0).text());
                weatherInfo.put("pressure", weatherElements.get(5).select("elementValue").get(0).text());
                weatherInfo.put("dayRain", weatherElements.get(6).select("elementValue").get(0).text());
                weatherInfo.put("gustSpeed", H_FX);
                weatherInfo.put("gustDirection", H_XD);
                weatherInfo.put("weather", weatherElements.get(14).select("elementValue").get(0).text());*/
                Elements locElements = docs.select("dataset").get(0).select("Station");
                for (int i = 0; i < locElements.size(); ++i)
                {
                    Element locElement = locElements.get(i);
                    Element coordinateElement = locElement.select("GeoInfo").get(0).select("Coordinates").get(1);
                    Double lat = Double.parseDouble(coordinateElement.select("StationLatitude").get(0).text());
                    Double lon = Double.parseDouble(coordinateElement.select("StationLongitude").get(0).text());
                    Double distance = computeDistance(lat, lon, latitude, longitude);

                    if(distance<minDistance)
                    {
                        closestIndex = i;
                        minDistance = distance;
                    }
                }
                Element targetElement = locElements.get(closestIndex);
                Element weatherElement = targetElement.select("weatherElement").get(0);
                Element gustInfoElement = weatherElement.select("GustInfo").get(0);
                String peakGustSpeed = gustInfoElement.select("PeakGustSpeed").get(0).text();
                String gustDirection = gustInfoElement.select("Occurred_at").get(0).select("WindDirection").get(0).text();
                //if(peakGustSpeed.equals("-99")) peakGustSpeed = "-"; //5.0 配合資料庫改動
                //if(gustDirection.equals("-99")) gustDirection = "-"; //5.0 配合資料庫改動

                weatherInfo.put("locationName", targetElement.select("StationName").get(0).text());
                weatherInfo.put("windDirection", weatherElement.select("WindDirection").get(0).text());
                weatherInfo.put("windSpeed", weatherElement.select("WindSpeed").get(0).text());
                weatherInfo.put("temperature", weatherElement.select("AirTemperature").get(0).text());
                weatherInfo.put("humidity", weatherElement.select("RelativeHumidity").get(0).text());
                weatherInfo.put("pressure", weatherElement.select("AirPressure").get(0).text());
                weatherInfo.put("dayRain", weatherElement.select("Now").get(0).select("Precipitation").get(0).text());
                weatherInfo.put("gustSpeed", peakGustSpeed);
                weatherInfo.put("gustDirection", gustDirection);
                weatherInfo.put("weather", weatherElement.select("Weather").get(0).text());

                getDataCompleted = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private Double computeDistance(Double cLat, Double cLon, Double cExifLat, Double cExifLon)
    {
        Double disSquare = (cLat-cExifLat)*(cLat-cExifLat)+(cLon-cExifLon)*(cLon-cExifLon);
        Double distance = Math.sqrt(disSquare);
        return distance;
    }
}
