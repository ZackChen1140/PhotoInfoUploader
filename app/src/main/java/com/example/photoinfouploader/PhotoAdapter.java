package com.example.photoinfouploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private Context context;
    //private List<Bitmap> photoList; // PhotoData是用于存储照片信息的数据类
    private JSONArray photoArray;
    private int albumId;
    private EXIFReader exifReader;

    // 构造方法，接受数据源和上下文作为参数
    public PhotoAdapter(Context context, JSONArray photoArray, int albumId) {
        this.context = context;
        this.photoArray = photoArray;
        this.albumId = albumId;
        exifReader = new EXIFReader();
    }

    // 创建ViewHolder，用于绑定布局文件中的视图元素
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }
    // 绑定数据到ViewHolder
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try
        {
            JSONObject photoObject = photoArray.getJSONObject(position);
            String photoPath = photoObject.getString("photo path");
//            Bitmap photoData = BitmapFactory.decodeFile(photoPath);
//            // 在这里，您可以设置ImageView的图片，也可以设置其他视图元素
//            // 例如：holder.imageView.setImageResource(photoData.getPhotoResId());
//            //holder.imageView.setImageBitmap(photoData);
//            RequestOptions options = new RequestOptions()
//                    .override(300, 300) // 設置目標寬度和高度
//                    .format(DecodeFormat.PREFER_RGB_565) // 設置解碼格式，減小內存占用
//                    .centerCrop(); // 如果圖片比例不符合目標比例，進行中央裁剪
//            Glide.with(context).load(photoData).apply(options).into(holder.imageView);

            final int position4thread = position;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 在這裡進行照片加載的耗時操作，例如從本地文件系統加載照片
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 16;
                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);

                    // 將照片設置到ImageView中
                    holder.itemView.post(new Runnable() {
                        @Override
                        public void run() {
                            RequestOptions options = new RequestOptions()
                                    .override(300, 300)
                                    .format(DecodeFormat.PREFER_RGB_565)
                                    .transform(new CenterCrop());
                            if(exifReader.getPhotoOrientation(photoPath).equals("6"))
                                options = options.transform(new CenterCrop(), new Rotate(90));
                            Glide.with(context).load(bitmap).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).placeholder(R.drawable.main_default_album_icon).apply(options).into(holder.imageView);
                        }
                    });
                }
            }).start();
            // 设置点击事件监听器
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 处理点击事件，启动新的Activity等等
                    // 示例：启动目标Activity并传递数据
                    Intent intent = new Intent(context, PhotoActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("album ID", albumId);
                    bundle.putString("photo json object", photoObject.toString());
                    bundle.putBoolean("preview mode", false);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                    ((Activity)context).finish();
                }
            });
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    // 获取数据源的大小
    @Override
    public int getItemCount() {
        return photoArray.length();
    }
    // 自定义ViewHolder类，用于绑定视图元素
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photosImageView); // 根据布局中的ImageView的ID来获取ImageView实例
        }
    }
}
