package com.larryliang.arredecoration;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.larryliang.arredecorationunity.UnityPlayerActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
//import com.larryliang.arredecoration.UnityPlayerActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_TAKE_PICTURE = 0;
    private static final int REQUEST_CODE_CHOOSE_ALBUM = 1;
    private static final int REQUEST_CODE_CROP = 2;

    ImageView mImageView;
    Button mCreateModelButton;
    Button mProceedButton;

    String photoName;
    String cropPhotoName;

    private Uri photoUri;
    private Uri cropPhotoUri;

    private boolean modelCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.INTERNET, Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
    }

    void initView() {
        mImageView = findViewById(R.id.imageView);
        mCreateModelButton = findViewById(R.id.create_model_button);
        mProceedButton = findViewById(R.id.proceed_button);

        mCreateModelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] items = {"Take a picture", "Choose from album"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        photoName = Environment.getExternalStorageDirectory().getPath() + "/arredecoration/photo" + System.currentTimeMillis() + ".jpg";
                        cropPhotoName = Environment.getExternalStorageDirectory().getPath() + "/arredecoration/cropPhoto" + System.currentTimeMillis() + ".jpg";

                        if (which == 0) {
                            File picture = new File(photoName);
                            if (!picture.getParentFile().exists()) {
                                picture.getParentFile().mkdirs();
                            }

                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            // Android7.0 URI
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                photoUri = FileProvider.getUriForFile(MainActivity.this,
                                        getApplicationContext().getPackageName() + ".my.package.name.provider", picture);
                            } else {
                                photoUri = Uri.fromFile(picture);
                            }
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
                        } else if (which == 1) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, REQUEST_CODE_CHOOSE_ALBUM);
                        }
                    }
                }).setCancelable(true);

                Dialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });

        mProceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (modelCreated) {
                    startActivity(new Intent(MainActivity.this, UnityPlayerActivity.class));
                } else {
                    Toast.makeText(MainActivity.this, "Please create a hanging model first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            File cropPhoto;
            switch (requestCode) {
                case REQUEST_CODE_TAKE_PICTURE:
                    cropPhoto = new File(cropPhotoName);
                    if (!cropPhoto.getParentFile().exists()) {
                        cropPhoto.getParentFile().mkdirs();
                    }

                    cropPhotoUri = Uri.fromFile(cropPhoto);

                    startPhotoZoom(photoUri, cropPhotoUri);
                    break;

                case REQUEST_CODE_CHOOSE_ALBUM:
                    if (data != null) {
                        cropPhoto = new File(cropPhotoName);
                        if (!cropPhoto.getParentFile().exists()) {
                            cropPhoto.getParentFile().mkdirs();
                        }

                        cropPhotoUri = Uri.fromFile(cropPhoto);

                        Uri newUri = Uri.parse(getPath(data.getData()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            newUri = FileProvider.getUriForFile(MainActivity.this,
                                getApplicationContext().getPackageName() + ".my.package.name.provider", new File(newUri.getPath()));
                        }

                        startPhotoZoom(newUri, cropPhotoUri);
                    }
                    break;

                case REQUEST_CODE_CROP:
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), cropPhotoUri);

                        mImageView.setImageBitmap(bitmap);

                        modelCreated = true;

                        Drawable drawable = getDrawable(R.drawable.round_button);
                        mProceedButton.setBackground(drawable);

                        // /storage/emulated/0/arredecoration/photoDest.jpg
                        String path = Environment.getExternalStorageDirectory().getPath() + "/arredecoration/photoDest.jpg";
                        File photoDest = new File(path);
                        if (photoDest.exists()) {
                            System.out.println("exist, delete it." + photoDest.delete());
                        } else {
                            System.out.println("not exist, " + path);
                        }

                        FileOutputStream fos;
                        try {
                            fos = new FileOutputStream(path);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startPhotoZoom(Uri uri, Uri desUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 1024);
        intent.putExtra("outputY", 1024);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("return-data", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, desUri);

        startActivityForResult(intent, REQUEST_CODE_CROP);
    }

    /**
     * The following codes are borrowed from https://github.com/ChenYXin/TakePicAndGallery
     * Author: ChenYXin
     * Date: 2018/07/28
     */
    @SuppressLint("NewApi")
    public String getPath(Uri uri) {

        String pathHead = "file:///";
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return pathHead + Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);

                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return pathHead + getDataColumn(contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return pathHead + getDataColumn(contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return pathHead + getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return pathHead + uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
