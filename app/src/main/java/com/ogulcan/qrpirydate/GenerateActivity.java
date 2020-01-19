package com.ogulcan.qrpirydate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.ogulcan.qrpirydate.Objects.Product;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class GenerateActivity extends AppCompatActivity {

    public final static int QRcodeWidth = 500, QRcodeHeight = 500, PICK_IMAGE = 100, RequestWritePermissionID = 1 ;
    private static final String IMAGE_DIRECTORY = "/QRcodes";

    BluetoothSocket clientSocket;
    // EN: mac address HC-06
    String DEVICE_UID = "00:21:13:01:AE:1F";

    Bitmap bitmap;
    int clicked;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageRef;
    Uri imageUri;
    Button btn, save_btn, upload_image, bluetooth_btn, blueon_btn;
    RelativeLayout loadingPanel;
    private EditText etqr, production_date_text, expiry_date_text;
    DatePickerDialog.OnDateSetListener mDateSetListenerProduction, mDateSetListenerExpiry;
    private ImageView iv;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestWritePermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    //saveqr
                    if(clicked==0) {
                        String path = saveImage(bitmap);
                        Toast.makeText(GenerateActivity.this, "QRCode saved to -> " + path, Toast.LENGTH_SHORT).show();
                    }
                    //loadimage
                    else {
                        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(gallery, PICK_IMAGE);
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        iv = (ImageView) findViewById(R.id.iv);
        etqr = (EditText) findViewById(R.id.etqr);
        production_date_text = (EditText) findViewById(R.id.production_date_text);
        expiry_date_text = (EditText) findViewById(R.id.expiry_date_text);
        btn = (Button) findViewById(R.id.btn);
        save_btn = (Button) findViewById(R.id.save_btn);
        upload_image = (Button) findViewById(R.id.load_image);
        bluetooth_btn = (Button) findViewById(R.id.bluetooth_btn);
        blueon_btn = (Button) findViewById(R.id.blueon_btn);
        loadingPanel = (RelativeLayout) findViewById(R.id.loadingPanel);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingPanel.setVisibility(View.VISIBLE);
                //Parsing dates for comparision
                @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date date1 = null;
                Date date2 = null;
                try {
                    date1 = format.parse(production_date_text.getText().toString());
                    date2 = format.parse(expiry_date_text.getText().toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if(etqr.getText().toString().trim().length() == 0){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(GenerateActivity.this, "Enter name!", Toast.LENGTH_SHORT).show();
                }
                else if(etqr.getText().toString().trim().length() > 40){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(GenerateActivity.this, "Product name's length can't be longer than 40 characters.", Toast.LENGTH_SHORT).show();
                }
                else if(production_date_text.getText().toString().trim().length() == 0){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(GenerateActivity.this, "Enter Production Date!", Toast.LENGTH_SHORT).show();
                }
                else if(expiry_date_text.getText().toString().trim().length() == 0){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(GenerateActivity.this, "Enter Production Date!", Toast.LENGTH_SHORT).show();
                }
                else if(date1!=null & date2!=null){
                    if(date1.compareTo(date2) <= 0){
                        loadingPanel.setVisibility(View.GONE);
                        Toast.makeText(GenerateActivity.this, "Expiry date can't be before than Production date.", Toast.LENGTH_SHORT).show();
                    }
                }
                else if(!upload_image.getText().toString().equals("UPLOADED")){
                    loadingPanel.setVisibility(View.GONE);
                    Toast.makeText(GenerateActivity.this, "Upload an image!", Toast.LENGTH_SHORT).show();
                }
                else {
                    final StorageReference imageRef = storageRef.child("images/"+imageUri.getLastPathSegment());
                    UploadTask uploadTask = imageRef.putFile(imageUri);
                    Task<Uri> task = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                loadingPanel.setVisibility(View.GONE);
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return imageRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                                Date productionDate = null;
                                Date expiryDate = null;
                                try {
                                    productionDate = formatter.parse(production_date_text.getText().toString());
                                    expiryDate = formatter.parse(expiry_date_text.getText().toString());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                assert downloadUri != null;
                                Product data = new Product(etqr.getText().toString(), downloadUri.toString(), productionDate, expiryDate);
                                db.collection("Products").add(data).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        try {
                                            bitmap = TextToImageEncode(documentReference.getId());
                                            iv.setImageBitmap(bitmap);
                                            save_btn.setVisibility(View.VISIBLE);
                                            bluetooth_btn.setVisibility(View.VISIBLE);
                                            loadingPanel.setVisibility(View.GONE);
                                        } catch (WriterException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                loadingPanel.setVisibility(View.GONE);
                                                Toast.makeText(GenerateActivity.this, "Failure", Toast.LENGTH_SHORT ).show();
                                            }
                                        });
                            } else {
                                loadingPanel.setVisibility(View.GONE);
                                Toast.makeText(GenerateActivity.this, "Image task is not successful", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingPanel.setVisibility(View.VISIBLE);
                clicked=0;
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        loadingPanel.setVisibility(View.GONE);
                        ActivityCompat.requestPermissions(GenerateActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestWritePermissionID);
                    }
                    else {
                        String path = saveImage(bitmap);
                        loadingPanel.setVisibility(View.GONE);
                        Toast.makeText(GenerateActivity.this, "QRCode saved to -> " + path, Toast.LENGTH_SHORT).show();
                    }
                } catch(Exception e){
                    loadingPanel.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
        });

        blueon_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingPanel.setVisibility(View.VISIBLE);
                String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                startActivityForResult(new Intent(enableBT), 0);

                BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

                try{
                    BluetoothDevice device = bluetooth.getRemoteDevice(DEVICE_UID);
                    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});

                    clientSocket = (BluetoothSocket)m.invoke(device,1);
                    clientSocket.connect();

                    blueon_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_left, 0, 0, 0);
                    blueon_btn.setText("Matched");
                    loadingPanel.setVisibility(View.GONE);
                } catch (InvocationTargetException | NoSuchMethodException | IOException | IllegalAccessException e) {
                    loadingPanel.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
        });

        bluetooth_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingPanel.setVisibility(View.VISIBLE);
                try{
                    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                    //DateFormat formatternow = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                    Date expiryDate = null;
                    //Date now = new Date();
                    try {
                        expiryDate = formatter.parse(expiry_date_text.getText().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    OutputStream outputStream = clientSocket.getOutputStream();

                    String value = formatter.format(expiryDate);
                    //value += " " + formatternow.format(now);
                    Toast.makeText(GenerateActivity.this, value, Toast.LENGTH_SHORT).show();

                    outputStream.write(value.getBytes());
                    bluetooth_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_left, 0,0,0);
                    loadingPanel.setVisibility(View.GONE);
                } catch (IOException e) {
                    loadingPanel.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
        });

        production_date_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        GenerateActivity.this,
                        android.R.style.Theme_Holo_Dialog_MinWidth,
                        mDateSetListenerProduction,
                        year,month,day);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        mDateSetListenerProduction = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                production_date_text.setText(new StringBuilder().append(day).append("/").append(month+1).append("/").append(year));
            }
        };


        expiry_date_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        GenerateActivity.this,
                        android.R.style.Theme_Holo_Dialog_MinWidth,
                        mDateSetListenerExpiry,
                        year,month,day);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        mDateSetListenerExpiry = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                expiry_date_text.setText(new StringBuilder().append(day).append("/").append(month+1).append("/").append(year));
            }
        };

        upload_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clicked=1;
                try {
                    if (ActivityCompat.checkSelfPermission(GenerateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(GenerateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestWritePermissionID);
                    } else {
                        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(gallery, PICK_IMAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
    //upload image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            upload_image.setText("UPLOADED");
            upload_image.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_left, 0, 0, 0);
        }
    }

    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);

        if (!wallpaperDirectory.exists()) {
            Log.d("dir---->", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            save_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_left, 0,0,0);

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

    }
    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.QR_CODE,
                    QRcodeWidth, QRcodeHeight, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}
