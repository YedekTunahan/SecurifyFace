package com.example.ergdeneme;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ergdeneme.Frame.FrameOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import org.jmrtd.lds.MRZInfo;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends AppCompatActivity {

    CameraView camera;
    FrameOverlay viewFinder;

    private static String TAG = "MainActivity";
    private AtomicBoolean processing = new AtomicBoolean(false);
    private Bitmap originalBitmap = null;
    private Bitmap scannable = null;
    private ProcessOCR processOCR;
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private Intent intent;

   /* private  String documentNumber = "";
    private  String dateOfBirth = "";
    private  String getDateOfExpiry = "";*/

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this); // Yaşam Döngüsü Sahibini Ayarla

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                viewFinder = new FrameOverlay(CameraActivity.this);
                camera.addView(viewFinder);
                camera.addFrameProcessor(frameProcessor); // çerçeve işlemcisi = frame processor Bununla birlikte Sürekli okuma gerçekleştiriliyor...
            }
        });
    }

    //Kamera ya eklediğimiz Frame
    private FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {
            if (frame.getData() != null && !processing.get()) {
                processing.set(true);

                YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 100, os);
                byte[] jpegByteArray = os.toByteArray();

                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);

                if(bitmap != null) {
                    bitmap = rotateImage(bitmap, frame.getRotation());

                    bitmap = getViewFinderArea(bitmap);

                    originalBitmap = bitmap;

                    scannable = getScannableArea(bitmap); // Taranacak Alanın Sınırlarını Belirttiğimiz yer Tarama Alanı -- processOCR 'a veriliyor.

                    processOCR = new ProcessOCR();
                    processOCR.setBitmap(scannable); // New Value döndürüyor.

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processOCR.execute();  // execute ile thread olayını çalıştırıyoruz.

                        }
                    });

                }
            }
        }
    };



    private Bitmap rotateImage(Bitmap bitmap, int rotate){

        Log.e(TAG, "rotateImage() methodu çalıştı: " + rotate);

        if (rotate != 0) {

            // Getting width & height of the given image.
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            // Setting pre rotate
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);

            // Rotating Bitmap
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        }

        // Convert to ARGB_8888, required by tess
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        return bitmap;
    }


    private Bitmap getViewFinderArea(Bitmap bitmap) {  // bulucu alanını görüntüle()
        int sizeInPixel = getResources().getDimensionPixelSize(R.dimen.frame_margin);
        int center = bitmap.getHeight() / 2;

        int left = sizeInPixel;
        int right = bitmap.getWidth() - sizeInPixel;
        int width = right - left;
        int frameHeight = (int) (width / 1.42f); // Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm

        int top = center - (int)(frameHeight / 1.7f); // Başlangıç koordinatı ?

        bitmap = Bitmap.createBitmap(bitmap, left, (int) (top*1.7f),
                width, frameHeight);

        return bitmap;
    }

    private Bitmap getScannableArea(Bitmap bitmap){  // Taranabilir alan elde etme()
        int top = bitmap.getHeight() * 4 / 10;

        bitmap = Bitmap.createBitmap(bitmap, 0, top,
                bitmap.getWidth(), bitmap.getHeight() - top);

        return bitmap;
    }

    /// okuma işlemi ,,,
    private class ProcessOCR extends AsyncTask {
        Bitmap bitmap = null;
        boolean restart = false;

        @Override
        protected Object doInBackground(Object[] objects) {
            if (bitmap != null) {

                // ML KİT - MRZ READ
                restart = processImage(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {  // yürütme sonrası ()
            if(restart == true)
            {
                processing.set(true);  // işleme
            }
            else
            {
                processing.set(false);
            }

            // Sayfa geçiş işlemi
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }


    //ML KİT İmage işleme
    private boolean processImage(Bitmap bitmap) {
        final boolean[] tempRestart = {false};
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {

                                //textView.setText((CharSequence) visionText); // Buradaki karaktere ayırmayla şart oluşturabilirim
                               /* for(Text.TextBlock block : visionText.getTextBlocks()){
                                    String blockText = block.getText();
                                    Point[] blockCornerPoints = block.getCornerPoints();
                                    Rect blockFrame = block.getBoundingBox();
                                }*/
                                try
                                {
                                    String value = visionText.getText().replace(" ","");
                                    String resultMrz = value.replace("«","<");
                                    if(resultMrz.length() == 92) //Uzunlugu saydım 90 gözüküyordu. Bu if kaldırılabilir zorunlu değil.
                                    {
                                        Log.e("Dogru UZUNLUK ", String.valueOf(visionText.getText().length()));

                                        ReadMerz(resultMrz);

                                        tempRestart[0] = false;
                                        camera.removeFrameProcessor(frameProcessor); // Camerayı sonlandırması lazım.

                                        intent = new Intent(CameraActivity.this,NfcActivity.class);
                                        intent.putExtra("getDateOfExpiry",ReadMerz(resultMrz).getDateOfExpiry());
                                        intent.putExtra("dateOfBirth",ReadMerz(resultMrz).getDateOfBirth());
                                        intent.putExtra("documentNumber",ReadMerz(resultMrz).getDocumentNumber());
                                        intent.putExtra("name",ReadMerz(resultMrz).getSecondaryIdentifier().replace("<"," "));
                                        intent.putExtra("surname",ReadMerz(resultMrz).getPrimaryIdentifier());
                                        startActivity(intent);
                                    }
                                    else
                                    {
                                        Log.e("HATALI UZUNLUK ", String.valueOf(resultMrz.length()));
                                        tempRestart[0] = true;
                                    }
                                }
                                catch(Exception e)
                                {

                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        return tempRestart[0];
    }

    //MRZ INFO
    public  MRZInfo ReadMerz(String text){
        try
        {
            String mrz = "I<TURA40U475006<23741396140<<<9701034M330127TUR<<<<<<<<<<<0USTUNTEPE<<SELAHATTIN<TUNAHAN<";
            String mrzString = "I<TURA40U475006<23741396140<<<\n9701034M3301270TUR<<<<<<<<<<<0\nUSTUNTEPE<<SELAHATTIN<TUNAHAN<";
            MRZInfo mrzInfoz = new MRZInfo(text);

            String issuingState = mrzInfoz.getIssuingState();
            String primaryIdentifier =  mrzInfoz.getPrimaryIdentifier();
            String secondaryIdentifier = mrzInfoz.getSecondaryIdentifier();
            String documentNumber2 = mrzInfoz.getDocumentNumber();
            String nationality = mrzInfoz.getNationality();
            String dateOfBirth2 = mrzInfoz.getDateOfBirth();
            String personalNumber = mrzInfoz.getPersonalNumber();
            String getOptionalData1 = mrzInfoz.getOptionalData1();
            String Gender = String.valueOf(mrzInfoz.getGender());
            String getDateOfExpiry2 = mrzInfoz.getDateOfExpiry();

		 /*  documentNumber= mrzInfoz.getDocumentNumber();
		   dateOfBirth = mrzInfoz.getDateOfBirth();
		   getDateOfExpiry = mrzInfoz.getDateOfExpiry();*/

            Log.e("issuingState ( Ulke )",issuingState);
            Log.e("primaryIdentifier(soyad",primaryIdentifier);
            Log.e("secondaryIdentifier(ad)",secondaryIdentifier);
            Log.e("documentNumbe(Seri no)",documentNumber2);
            Log.e("dateOfBirth",dateOfBirth2);
            Log.e("personalNumber(TC)",personalNumber.replace("<",""));
            Log.e("getOptionalData1",getOptionalData1);
            Log.e("Gender -Cinsiyet",Gender);
            Log.e(" Son kullanma tarihi",getDateOfExpiry2);
            Log.w("test","dedaw");
            return mrzInfoz;
        }
        catch(Exception e)
        {
            Log.e("MRZ INFOZ hatası ","MRZ INFOZ hatası ");
        }
    return null;
    }


}