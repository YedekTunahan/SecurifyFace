package com.example.ergdeneme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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

public class CameraFragment extends Fragment {
    CameraView camera;
    FrameOverlay viewFinder;

    private static String TAG = "MainActivity";
    private AtomicBoolean processing = new AtomicBoolean(false);
    private Bitmap originalBitmap = null;
    private Bitmap scannable = null;
    private ProcessOCR processOCR;
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View design = inflater.inflate(R.layout.fragment_camera, container, false);

        camera = design.findViewById(R.id.camera);

        camera.setLifecycleOwner(this); // Yaşam Döngüsü Sahibini Ayarla
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {

                viewFinder = new FrameOverlay(getActivity());
                camera.addView(viewFinder);
                camera.addFrameProcessor(frameProcessor); // çerçeve işlemcisi = frame processor
            }
        });
        return design;
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

                    getActivity().runOnUiThread(new Runnable() {
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

        bitmap = Bitmap.createBitmap(bitmap, left, (int) (top*1.9f),
                width, frameHeight);



        return bitmap;
    }

    private Bitmap getScannableArea(Bitmap bitmap){  // Taranabilir alan elde etme()
        int top = bitmap.getHeight() * 1 / 10;

        bitmap = Bitmap.createBitmap(bitmap, 0, top,
                bitmap.getWidth(), bitmap.getHeight() - top);



        return bitmap;
    }

    /// okuma işlemi ,,,
    private class ProcessOCR extends AsyncTask {
        Bitmap bitmap = null;

        @Override
        protected Object doInBackground(Object[] objects) {
            if (bitmap != null) {

                // ML KİT - MRZ READ
                 processImage(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {  // yürütme sonrası ()
            processing.set(false);  // işleme

            // Sayfa geçiş işlemi
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }


    //ML KİT İmage işleme
    private Void processImage(Bitmap bitmap) {

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                // ...
                                //textView.setText((CharSequence) visionText);
                                for(Text.TextBlock block : visionText.getTextBlocks()){
                                    String blockText = block.getText();
                                    Point[] blockCornerPoints = block.getCornerPoints();
                                    Rect blockFrame = block.getBoundingBox();
                                   /* Log.e("symbol",blockText);
                                    Log.e("symbolFrame", String.valueOf(blockFrame));
                                    Log.d("-","---------");*/
                                }
                                String value = visionText.getText().replace(" ","");
                                String result = value.replace("«","<");

                                if (result.length() == 92) {
                                    Log.e("visionText", result); // Bütün taramayı veriyor...

                                    Log.e("UZUNLUK", String.valueOf(result.length()));
                                    camera.removeFrameProcessor(frameProcessor);
                                    // JMRTD MRZ OKUMA
                                   ReadMerz(result);
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


        return null;
    }

    //MRZ INFO
    public  void ReadMerz(String textMRZ){
        MRZInfo mrzInfo = new MRZInfo(textMRZ);

        String issuingState = mrzInfo.getIssuingState();
        String primaryIdentifier =  mrzInfo.getPrimaryIdentifier();
        String secondaryIdentifier = mrzInfo.getSecondaryIdentifier();
        String documentNumbe = mrzInfo.getDocumentNumber();
        String nationality = mrzInfo.getNationality();
        String dateOfBirth = mrzInfo.getDateOfBirth();
        String personalNumber = mrzInfo.getPersonalNumber();
        String getOptionalData1 = mrzInfo.getOptionalData1();
        String Gender = String.valueOf(mrzInfo.getGender());
        String getDateOfExpiry = mrzInfo.getDateOfExpiry();



        Log.e("issuingState ( Ulke )",issuingState);
        Log.e("primaryIdentifier(soyad",primaryIdentifier);
        Log.e("secondaryIdentifier(ad)",secondaryIdentifier);
        Log.e("documentNumbe(Seri no)",documentNumbe);
        Log.e("nationality(UYRUK)",nationality);
        Log.e("dateOfBirth",dateOfBirth);
        Log.e("personalNumber(TC)",personalNumber.replace("<",""));
        Log.e("getOptionalData1",getOptionalData1);
        Log.e("Gender -Cinsiyet",Gender);
        Log.e(" Son kullanma tarihi",getDateOfExpiry);
        Log.w("test","dedaw");
    }
}