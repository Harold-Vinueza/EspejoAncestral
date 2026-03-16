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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.espejo_ancestral.motor.MotorRasgos;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.exifinterface.media.ExifInterface;
import android.graphics.Matrix;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageCapture;
import androidx.core.content.ContextCompat;

import androidx.camera.core.AspectRatio;
import androidx.camera.view.PreviewView;

public class MainActivity extends AppCompatActivity {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    private ImageView mImageView;
    private TextView txtresults;
    private androidx.camera.view.PreviewView previewView;
    Bitmap mSelectedImage;
    String rutaImagen = null;
    private boolean usarCamaraFrontal = false;

    private androidx.camera.core.ImageCapture imageCapture;

    private ImageButton btCapturar;
    private ImageButton btSwitchCamera;
    private ImageButton btCerrarImagen;
//    private ImageCapture imageCapture;
private static final Map<String, MotorRasgos.Resultado> historial = new HashMap<>();
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
        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(androidx.camera.view.PreviewView.ScaleType.FILL_CENTER);
        iniciarCamara();

        btCapturar = findViewById(R.id.btCapturar);
        btSwitchCamera = findViewById(R.id.btSwitchCamera);
        btCerrarImagen = findViewById(R.id.btCerrarImagen);
        btCerrarImagen.setVisibility(View.GONE);

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        DatabaseReference db = FirebaseDatabase.getInstance()
                .getReference("test_android");

        db.setValue("Android conectado correctamente");
    }

    // ================== GALERÍA ==================
    public void abrirGaleria(View view) {

        txtresults.setVisibility(View.GONE);
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }
    // **********************************
    // ================== CÁMARA ==================
    public void cambiarCamara(View view) {
        txtresults.setVisibility(View.GONE);
        usarCamaraFrontal = !usarCamaraFrontal;

        iniciarCamara();
    }
    //}

    // ================== RESULTADO FOTO ==================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (resultCode == RESULT_OK) {
            try {

                if (requestCode == REQUEST_CAMERA) {
                    txtresults.setVisibility(View.GONE);
                    btCapturar.setVisibility(View.GONE);
                    btSwitchCamera.setVisibility(View.GONE);
                    btCerrarImagen.setVisibility(View.VISIBLE);
                    Bitmap bitmap = BitmapFactory.decodeFile(rutaImagen);
                    bitmap = corregirRotacion(rutaImagen, bitmap);

                    // seguridad extra para teléfonos que no guardan EXIF (Samsung)
                    if (bitmap.getWidth() > bitmap.getHeight()) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        bitmap = Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix,
                                true
                        );
                    }

                    mSelectedImage = bitmap;
                    mImageView.setImageBitmap(mSelectedImage);

                    mImageView.setVisibility(View.VISIBLE);
                    previewView.setVisibility(View.GONE);

                    MediaScannerConnection.scanFile(
                            this,
                            new String[]{rutaImagen},
                            null,
                            null
                    );
                }

                if (requestCode == REQUEST_GALLERY && data != null) {

                    btCapturar.setVisibility(View.GONE);
                    btSwitchCamera.setVisibility(View.GONE);
                    btCerrarImagen.setVisibility(View.VISIBLE);

                    File temp = crearArchivoTempDesdeUri(data.getData());
                    rutaImagen = temp.getAbsolutePath();

                    Bitmap bitmap = BitmapFactory.decodeFile(rutaImagen);

                    mSelectedImage=corregirRotacion(rutaImagen, bitmap);


                    //mSelectedImage = bitmap;
                    mImageView.setImageBitmap(mSelectedImage);

                    mImageView.setVisibility(View.VISIBLE);
                    previewView.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                txtresults.setText("Error al cargar imagen");
            }
        }
    }

    // ================== ANALIZAR ==================
    public void AnalizarRostro(View v) {

        txtresults.setVisibility(View.GONE);
        if (mSelectedImage == null) {
            txtresults.setText("Seleccione una imagen primero");
            txtresults.setVisibility(View.VISIBLE);
            return;
        }






        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);





        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        txtresults.setText("No se detectó rostro");
                        txtresults.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (faces.size() > 1) {
                        txtresults.setText("Solo debe aparecer una persona en la imagen");
                        txtresults.setVisibility(View.VISIBLE);
                        return;
                    }

                    Face face = faces.get(0);

                    Float leftEye = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();
                    Float smile = face.getSmilingProbability();
                    if (leftEye == null || rightEye == null || smile == null) {
                        txtresults.setText("El rostro detectado no parece humano");
                        txtresults.setVisibility(View.VISIBLE);
                        return;
                    }
                    String hashImagen = generarHash(mSelectedImage);

                    MotorRasgos.Resultado r;

                    if(historial.containsKey(hashImagen)){

                        // misma foto → usar resultado guardado
                        r = historial.get(hashImagen);

                    }else{

                        // foto nueva → calcular resultado
                        r = MotorRasgos.evaluar(face, hashImagen);

                        historial.put(hashImagen,r);
                    }

                    String imagenBase64 = bitmapABase64(mSelectedImage);
                    guardarPerfilEnFirebase(r, imagenBase64);

                    StringBuilder sb = new StringBuilder();
                    sb.append(r.perfil).append("\n");
                    sb.append("Confianza: ")
                            .append((int)(r.confianza * 100))
                            .append("%\n\n");


                    sb.append("Otros perfiles posibles:\n");

                    for(String p : r.top3Perfiles){
                        sb.append(p).append("\n");
                    }

                    sb.append("\n");

                    for (MotorRasgos.Rasgo rg : r.rasgosUI) {
                        sb.append("- ").append(rg.nombre).append("\n");
                    }
//                    sb.append("\nDEBUG:\n");
//                    sb.append(r.metricas.toString());

                    Intent intent = new Intent(MainActivity.this, ResultadoActivity.class);
                    intent.putExtra("resultado", sb.toString());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    txtresults.setText("Error ML Kit: " + e.getMessage());
                    txtresults.setVisibility(View.VISIBLE);
                });


    }






    // ================== UTILIDADES ==================
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dir = getExternalFilesDir(null);
        File image = File.createTempFile("IMG_" + timeStamp + "_", ".jpg", dir);
        rutaImagen = image.getAbsolutePath();
        return image;
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


    private void guardarPerfilEnFirebase(MotorRasgos.Resultado r, String imagenBase64) {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("perfiles");

        ref.get().addOnSuccessListener(snapshot -> {

            if (snapshot.getChildrenCount() >= 20) {

                // encontrar el más viejo
                long oldest = Long.MAX_VALUE;
                String keyOldest = null;
                for (DataSnapshot child : snapshot.getChildren()) {

                    Long ts = child.child("timestamp").getValue(Long.class);

                    if (ts != null && ts < oldest) {
                        oldest = ts;
                        keyOldest = child.getKey();
                    }

                }

                if (keyOldest != null) {
                    ref.child(keyOldest).removeValue();
                }
            }

            DatabaseReference nuevo = ref.push();

            Map<String,Object> data = new HashMap<>();
            data.put("perfil", r.perfil);
            data.put("confianza", r.confianza);
            data.put("timestamp", System.currentTimeMillis());

            List<Map<String,Object>> rasgos = new ArrayList<>();


            for (MotorRasgos.Rasgo rg : r.rasgosUI) {
                Map<String,Object> obj = new HashMap<>();
                obj.put("nombre", rg.nombre);
                rasgos.add(obj);
            }

            data.put("rasgos", rasgos);
            data.put("imagen", imagenBase64);
            nuevo.setValue(data);
        });
    }



    private String bitmapABase64(Bitmap bitmap) {
        if (bitmap == null) return "";

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        // Puedes cambiar 70 por 60 o 50 si quieres que pese menos
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);

        byte[] imagenBytes = baos.toByteArray();

        return android.util.Base64.encodeToString(imagenBytes, android.util.Base64.DEFAULT);
    }





    private void iniciarCamara() {

        androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
                .addListener(() -> {
                    try {

                        androidx.camera.lifecycle.ProcessCameraProvider cameraProvider =
                                androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this).get();



                        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(metrics);

                        android.util.Size screenSize =
                                new android.util.Size(metrics.widthPixels, metrics.heightPixels);

                        androidx.camera.core.Preview preview =
                                new androidx.camera.core.Preview.Builder()
                                        .setTargetResolution(screenSize)
                                        .build();



                        imageCapture = new androidx.camera.core.ImageCapture.Builder()

                                .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_16_9)
                                .setTargetRotation(previewView.getDisplay().getRotation())
                                .build();

                        preview.setSurfaceProvider(previewView.getSurfaceProvider());

                        CameraSelector cameraSelector;

                        if (usarCamaraFrontal) {
                            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                        } else {
                            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                        }

                        cameraProvider.unbindAll();

                        cameraProvider.bindToLifecycle(
                                this,
                                cameraSelector,
                                preview,
                                imageCapture
                        );

                    } catch (Exception e) {
                        txtresults.setText("Error iniciando cámara");
                        txtresults.setVisibility(View.VISIBLE);
                    }

                }, androidx.core.content.ContextCompat.getMainExecutor(this));
    }


    public void capturarFoto(View view) {

        if (imageCapture == null) return;

        btCapturar.setVisibility(View.GONE);
        btSwitchCamera.setVisibility(View.GONE);
        btCerrarImagen.setVisibility(View.VISIBLE);
        txtresults.setVisibility(View.GONE);
        try {

            File foto = crearArchivoImagen();
            rutaImagen = foto.getAbsolutePath();

            androidx.camera.core.ImageCapture.OutputFileOptions options =
                    new androidx.camera.core.ImageCapture.OutputFileOptions.Builder(foto).build();

            imageCapture.takePicture(
                    options,
                    androidx.core.content.ContextCompat.getMainExecutor(this),
                    new androidx.camera.core.ImageCapture.OnImageSavedCallback() {

                        @Override
                        public void onImageSaved(
                                androidx.camera.core.ImageCapture.OutputFileResults outputFileResults) {

                            Bitmap bitmap = BitmapFactory.decodeFile(rutaImagen);



                           mSelectedImage= corregirRotacion(rutaImagen, bitmap);


//

                            mImageView.setImageBitmap(mSelectedImage);
                            mImageView.setVisibility(View.VISIBLE);
                            previewView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(
                                androidx.camera.core.ImageCaptureException exception) {

                            txtresults.setText("Error al tomar foto");
                            txtresults.setVisibility(View.VISIBLE);

                        }
                    });

        } catch (Exception e) {
            txtresults.setText("Error capturando foto");
            txtresults.setVisibility(View.VISIBLE);
        }
    }






    @Override
    protected void onResume() {
        super.onResume();

        if (mSelectedImage == null) {

            mImageView.setVisibility(View.GONE);
            previewView.setVisibility(View.VISIBLE);

            iniciarCamara();
        }
    }
    public void cerrarImagen(View view) {

        txtresults.setVisibility(View.GONE);

        mImageView.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);

        btCapturar.setVisibility(View.VISIBLE);
        btSwitchCamera.setVisibility(View.VISIBLE);
        btCerrarImagen.setVisibility(View.GONE);

        mSelectedImage = null;

    }
    private Bitmap corregirRotacion(String path, Bitmap bitmap) {
        try {

            androidx.exifinterface.media.ExifInterface exif =
                    new androidx.exifinterface.media.ExifInterface(path);

            int orientacion = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();

            if (orientacion == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientacion == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientacion == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            } else {
                return bitmap; // 👈 si no necesita rotación, se devuelve igual
            }

            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

        } catch (Exception e) {
            return bitmap;
        }
    }
    private String generarHash(Bitmap bitmap){

        try{

            java.io.ByteArrayOutputStream stream =
                    new java.io.ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG,80,stream);

            byte[] bytes = stream.toByteArray();

            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("MD5");

            byte[] hash = digest.digest(bytes);

            StringBuilder hex = new StringBuilder();

            for(byte b : hash){
                hex.append(String.format("%02x",b));
            }

            return hex.toString();

        }catch(Exception e){
            return "error";
        }

    }
}