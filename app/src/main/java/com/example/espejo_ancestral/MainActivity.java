package com.example.espejo_ancestral;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    private ImageView mImageView;
    private TextView txtresults;

    Bitmap mSelectedImage;
    String rutaImagen = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        txtresults = findViewById(R.id.txtresults);
        mImageView = findViewById(R.id.image_view);

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        DatabaseReference db = FirebaseDatabase.getInstance()
                .getReference("test_android");

        db.setValue("Android conectado correctamente");
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }
    public void abrirCamara(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //if (intent.resolveActivity(getPackageManager()) != null) {
            File foto;
            try {
                foto = crearArchivoImagen();
            } catch (IOException e) {
                txtresults.setText("Error creando archivo");
                return;
            }

            Uri fotoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    foto
            );

            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoURI);
            startActivityForResult(intent, REQUEST_CAMERA);
        //}
    }
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dir = getExternalFilesDir(null);
        File image = File.createTempFile("IMG_" + timeStamp + "_", ".jpg", dir);
        rutaImagen = image.getAbsolutePath();
        return image;
    }
    public void AnalizarRostro(View v) {

        //AQUI SE VA A TRABAR LA LOGICA DE ANALIZAR EL
        // ROSTRO DESDE LA CLASE MOTOS RASGO Y MOSTRS LOS DATOS DE LA PANTALLA
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    mSelectedImage = BitmapFactory.decodeFile(rutaImagen);
                    mImageView.setImageBitmap(mSelectedImage);
                    MediaScannerConnection.scanFile(this,
                            new String[]{rutaImagen}, null, null);
                }

                if (requestCode == REQUEST_GALLERY && data != null) {
                    File temp = crearArchivoTempDesdeUri(data.getData());
                    rutaImagen = temp.getAbsolutePath();
                    mSelectedImage = BitmapFactory.decodeFile(rutaImagen);
                    mImageView.setImageBitmap(mSelectedImage);
                }
            } catch (Exception e) {
                txtresults.setText("Error al cargar imagen");
            }
        }

    }

    private File crearArchivoTempDesdeUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = crearArchivoImagen();
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();
        return tempFile;
    }


}