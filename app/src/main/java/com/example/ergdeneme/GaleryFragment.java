package com.example.ergdeneme;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class GaleryFragment extends Fragment {

    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    Button selectImageBtn;
    ImageView imageView;
    TextView MRzText;

    static  final int  SELECT_IMAGE = 22;

    Uri imageUri;
    Bitmap bitmap;
    private GaleryFragment mBase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View design = inflater.inflate(R.layout.fragment_galery, container, false);

        selectImageBtn = design.findViewById(R.id.SelectBtn);
        imageView = design.findViewById(R.id.imageView);
        MRzText = design.findViewById(R.id.MRzText);


        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // dosya secmek ıcın kullanılıyor bu ıntetnt actıon_get_contet
                intent.setType("image/*"); // Tüm dosyaları seçebiliri engellemek için seçilecek nesnenin türününü image olduğunu belirterek sınırladım.

                startActivityForResult(intent,SELECT_IMAGE); // BUNUNLA ben talebimi yollamış oldum dosya seçim talebi , galeri açıldı resim seçildi  ve sistem bir intent meydana getırıyor
*/
                //bu ıntent secılen dosyanın uri bilgisini saklıyor biz onu yakalıyacağız

                ImagePicker.with(GaleryFragment.this)
                        .crop()	    			//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)

                        .start();

            }
        });
        return design;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {  // kullanıcının dosyayı secip secmemesine göre şimdi bir işlem yaptırcaz.
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {  // resultCode == RESULT_OK , kullanıcı gercekten bir resim seçtiyse

            imageUri = data.getData();  // buradan dönen değeri data  seçilen resmin uri sini saklıyor.Bunu değişkene atadım.

            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContext().getContentResolver(), imageUri);
               // bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
                processImage();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            imageView.setImageURI(imageUri); // seçileni ImageView da göstertiyoruz bu vesileyle
        }else if (resultCode == RESULT_CANCELED){
            Log.e("İptal","resim iptal edildi");
        }
    }



    private void processImage() {

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
                                    Log.e("symbol",blockText);
                                    Log.e("Point", String.valueOf(blockCornerPoints));
                                    Log.e("symbolFrame", String.valueOf(blockFrame));
                                    Log.e("-","---------");
                                    for (Text.Line line : block.getLines()) {
                                        String lineText = line.getText();
                                        Point[] lineCornerPoints = line.getCornerPoints();
                                        Rect lineFrame = line.getBoundingBox();
                                        for (Text.Element element : line.getElements()) {
                                            String elementText = element.getText();
                                            Point[] elementCornerPoints = element.getCornerPoints();
                                            Rect elementFrame = element.getBoundingBox();
                                            for (Text.Symbol symbol : element.getSymbols()) {
                                                String symbolText = symbol.getText();
                                                Point[] symbolCornerPoints = symbol.getCornerPoints();
                                                Rect symbolFrame = symbol.getBoundingBox();

                                                Log.e("symbol",symbolText);
                                                Log.e("Point", String.valueOf(symbolCornerPoints));
                                                Log.e("symbolFrame", String.valueOf(symbolFrame));
                                            }
                                        }
                                    }
                                }

                                MRzText.setText(visionText.getText());
                                Log.e("visionText", String.valueOf(visionText.getText()));


                                return null;
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


    }
}