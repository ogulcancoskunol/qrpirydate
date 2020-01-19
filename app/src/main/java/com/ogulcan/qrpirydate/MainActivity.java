package com.ogulcan.qrpirydate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;;import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button readButton, createButton;
    TextView name, prodate, expdate;
    ImageView image, panel;
    DocumentReference documentReference;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        readButton = (Button) findViewById(R.id.button);
        createButton = (Button) findViewById(R.id.create_button);
        name = (TextView) findViewById(R.id.name);
        prodate = (TextView) findViewById(R.id.prodate);
        expdate = (TextView) findViewById(R.id.expdate);
        image = (ImageView) findViewById(R.id.image);
        panel = (ImageView) findViewById(R.id.panel);

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivity(intent);
            }
        });

        if(getIntent() != null) {
            Intent ComingIntent = getIntent();
            String id = ComingIntent.getStringExtra(ScannerActivity.RESULT_URL);
            if (id != null) {
                documentReference = db.collection("Products").document(id);
                documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        name.setText(documentSnapshot.getString("name"));
                        prodate.setText(Objects.requireNonNull(documentSnapshot.getDate("productionDate")).toString());
                        expdate.setText(Objects.requireNonNull(documentSnapshot.getDate("expiryDate")).toString());
                        new DownloadImageTask(image).execute(documentSnapshot.getString("img"));
                        panel.setVisibility(View.GONE);
                        name.setVisibility(View.VISIBLE);
                        prodate.setVisibility(View.VISIBLE);
                        expdate.setVisibility(View.VISIBLE);
                        image.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GenerateActivity.class);
                startActivity(intent);
            }
        });
    }

    private class DownloadImageTask extends AsyncTask<String,Void,Bitmap> {
        ImageView imageView;

        private DownloadImageTask(ImageView imageView){
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String...urls){
            String urlOfImage = urls[0];
            Bitmap logo = null;
            try{
                InputStream is = new URL(urlOfImage).openStream();
                logo = BitmapFactory.decodeStream(is);
            }catch(Exception e){
                e.printStackTrace();
            }
            return logo;
        }

        protected void onPostExecute(Bitmap result){
            imageView.setImageBitmap(result);
        }
    }
}
