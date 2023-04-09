package com.example.ergdeneme;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  /*  TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    Button selectImageBtn;
    ImageView imageView;
    TextView textView;

    static  final int  SELECT_IMAGE = 22;

    Uri imageUri;
    Bitmap bitmap;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

/*        selectImageBtn = findViewById(R.id.imageBtn);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);*/

   /*     selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                *//*Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // dosya secmek ıcın kullanılıyor bu ıntetnt actıon_get_contet
                intent.setType("image/*"); // Tüm dosyaları seçebiliri engellemek için seçilecek nesnenin türününü image olduğunu belirterek sınırladım.

                startActivityForResult(intent,SELECT_IMAGE); // BUNUNLA ben talebimi yollamış oldum dosya seçim talebi , galeri açıldı resim seçildi  ve sistem bir intent meydana getırıyor
*//*
                //bu ıntent secılen dosyanın uri bilgisini saklıyor biz onu yakalıyacağız
*//*
                ImagePicker.with(MainActivity.this)
                        .crop()	    			//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)

                        .start();*//*

            }
        });*/
    }

   /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {  // kullanıcının dosyayı secip secmemesine göre şimdi bir işlem yaptırcaz.
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {  // resultCode == RESULT_OK , kullanıcı gercekten bir resim seçtiyse

            imageUri = data.getData();  // buradan dönen değeri data  seçilen resmin uri sini saklıyor.Bunu değişkene atadım.

            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
                processImage();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            imageView.setImageURI(imageUri); // seçileni ImageView da göstertiyoruz bu vesileyle
        }else if (resultCode == RESULT_CANCELED){
            Toast.makeText(this,"resim sçme iptal",Toast.LENGTH_SHORT);
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

                                textView.setText(visionText.getText());
                                Log.e("visionText", String.valueOf(visionText.getText()));


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


    }*/



}