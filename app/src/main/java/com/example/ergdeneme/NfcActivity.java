package com.example.ergdeneme;

import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.DG11File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.DG7File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PACEInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NfcActivity extends AppCompatActivity {

    String documentNumber;
    String dateOfBirth;
    String getDateOfExpiry;
    ImageView imageViewPhoto;

    TextView textViewNfcRead,textViewnameTitle,textViewSurnameTitle,textViewName,textViewSurname,
            textViewDateOfBirthTitle,textViewDateOfBirth,textViewDocumentNoTitle,textViewDocumentNo,textViewMrzTitle,textViewMrz;

    //EF_DG okumaları için gereken Güvenlik
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        documentNumber=  getIntent().getStringExtra("documentNumber"); //"A4OU47500"
        dateOfBirth =  getIntent().getStringExtra("dateOfBirth"); //"970103";
        getDateOfExpiry = getIntent().getStringExtra("getDateOfExpiry"); //"330127";



       enableOrDisable();
        /*textViewNfcRead = findViewById(R.id.textNFC);
        textViewDateOfBirth = findViewById(R.id.textDateOfBirth);
        textViewDocumentNo = findViewById(R.id.textdocumentNumber);
        textViewMrz =findViewById(R.id.textMRZ);
        textViewName = findViewById(R.id.textName);
        textViewSurname = findViewById(R.id.textSurname);*/

    }
    protected void onResume() {
        super.onResume();

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            Intent intent = new Intent(getApplicationContext(), this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String[][] filter = new String[][]{new String[]{"android.nfc.tech.IsoDep"}};
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
        }
        imageViewPhoto =findViewById(R.id.ImageViewPhoto);
    }

    @Override
    protected void onPause() {
        super.onPause();

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.e("OnNewIntetn", "çalıştı");

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            IsoDep nfc = IsoDep.get(tag);
            nfc.setTimeout(10000);
            Log.e("TAG", String.valueOf(tag));
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {

                BACKeySpec bacKey = new BACKey(documentNumber,dateOfBirth,getDateOfExpiry);

                Log.e("Backey", String.valueOf(bacKey));
                new ReadTask(nfc, bacKey).execute();

            }
        }
    }


    ///NFC ile okuma bilgilerin servisten alınması
    private class ReadTask extends AsyncTask<Void, Void, Exception> {

        private IsoDep isoDep;
        private BACKeySpec bacKey;

        public ReadTask(IsoDep isoDep, BACKeySpec bacKey) {
            this.isoDep = isoDep;
            this.bacKey = bacKey;
        }

        private DG1File dg1File;
        private DG2File dg2File; //MRZ içerisindeki kisisel bilgiler alınır . Ad soyad gibi
        private DG11File dg11File; // Adres ve aytıntılı bilgiler

        private DG3File dg3File;
        private DG15File dg15File;
        private DG7File dg7File;
        public Bitmap bitmap;

        @Override
        protected Exception doInBackground(Void... params) {
            try {

                Log.e("doInBackground","doInBackground ÇALIŞTI");

                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                PassportService service = new PassportService(cardService);
                service.open();


                boolean paceSucceeded = false; // pace başarılı oldu. Test ortamıdır

                try {
                    Log.e("try","Try'a girdi");
                    CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS));
                    // CardFileInputStream cardFileInputStream = new CardFileInputStream(service.getInputStream(PassportService.EF_CARD_ACCESS));
                    Collection<PACEInfo> paceInfos = cardAccessFile.getPACEInfos();//hız bilgileri
                    if (paceInfos != null && paceInfos.size() > 0) {
                        PACEInfo paceInfo = paceInfos.iterator().next();
                        service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));
                        paceSucceeded = true;
                    } else {
                        Log.e("paceInfo","BOŞŞ");
                        paceSucceeded = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                Log.e("1","1");
                service.sendSelectApplet(paceSucceeded);
                service.doBAC(bacKey);  // Patladığımız yer
                Log.e("2","2");
                if (!paceSucceeded) {
                    try {
                        Log.e("3","3");
                        service.getInputStream(PassportService.EF_COM); // hata burada
                        Log.e("4","4");
                    } catch (Exception e) {
                        Log.e("5","5");
                        Log.e("G", String.valueOf(bacKey));
                        service.doBAC(bacKey);  // Patladığımız yer
                        Log.e("ç", String.valueOf(bacKey));
                        Log.e("6","6");
                    }
                }

                Log.e("7","7");
                LDS lds = new LDS();
                ///////////// DG1FİLE
                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                lds.add(PassportService.EF_DG1, dg1In, dg1In.getLength());
                dg1File = lds.getDG1File();

                Log.e("imza", String.valueOf(dg1File.getEncoded()));
                Log.e("dg1File", String.valueOf(dg1File));

                ///////////// DG11FİLE
                CardFileInputStream dg11In = service.getInputStream(PassportService.EF_DG11);
                lds.add(PassportService.EF_DG11, dg11In, dg11In.getLength());
                dg11File = lds.getDG11File();


               ///////////// DG2FİLE
                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                lds.add(PassportService.EF_DG2, dg2In, dg2In.getLength());
                dg2File = lds.getDG2File();
                Log.e("dg2File", String.valueOf(dg2File));


                ///////////// DG15FİLE

                Log.e("dg15FileSTART", String.valueOf(dg15File));
                CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15);
                lds.add(PassportService.EF_DG15, dg15In, dg15In.getLength());
                dg15File = lds.getDG15File();
                Log.e("dg15FileGETCLASS", String.valueOf(dg15File.getPublicKey()));

               //IMZA

                // Elektronik imza bilgilerini al
               /* CardVerifiableCertificate cvCert = service.get;
                List<CardVerifiableCertificate> certChain = service.getCertificateChain();
                List<CVCertificateDescription> cvCertificateDescription = service.getCVCertificateDescriptionList();
                Signature signature = service.getSignature();*/
                ///////////// DG7FİLE

              /*  CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7);
                lds.add(PassportService.EF_DG7, dg7In, dg7In.getLength());
                dg7File = lds.getDG7File();
                Log.e("dg7File", String.valueOf(dg7File));
                */

                bitmap = FaceInfoChangem(dg2File,bitmap);
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {

            if (result == null) {

                MRZInfo mrzInfo = dg1File.getMRZInfo();


                Log.e("Alınan MRZ",mrzInfo.getPrimaryIdentifier());
                Log.e("DG11","////////////");
               /* Log.e("Adress", String.valueOf(dg11File.getPermanentAddress()));
                Log.e("Other Name", String.valueOf(dg11File.getOtherNames()));
                Log.e("Personal Number",dg11File.getPersonalNumber());
                Log.e("Place of Birth", String.valueOf(dg11File.getPlaceOfBirth()));
                Log.e("Date of Birth (in full)", String.valueOf(dg11File.getFullDateOfBirth()));
                //Log.e("Telephone Number(s)",dg11File.getTelephone());  //değerlere ulaşılmıyor
                // Log.e("Profession",dg11File.getProfession());  //değerlere ulaşılmıyor
                //Log.e("Title",dg11File.getTitle());   //değerlere ulaşılmıyor
                //Log.e("Personal Summary",dg11File.getPersonalSummary()); //değerlere ulaşılmıyor
                Log.e("Proof of Citizenship ", String.valueOf(dg11File.getProofOfCitizenship()));
                Log.e("Number of OtherValid ", String.valueOf(dg11File.getOtherValidTDNumbers()));
                Log.e("TAG", String.valueOf(dg11File.getTag()));*/
                // Log.e("Custody Information",dg11File.getCustodyInformation()); // değerlere ulaşılmıyor

                /// IMAGE  documentNumber,dateOfBirth,getDateOfExpiry

                imageViewPhoto.setImageBitmap(bitmap);

                textViewNfcRead.setText(dg11File.getPersonalNumber());
                textViewDateOfBirth.setText(dateOfBirth);
                textViewDocumentNo.setText(documentNumber);
                String textMrz = String.valueOf(dg1File).replace("DG1File","");
                textViewMrz.setText(textMrz);
                enable();

               // textViewName.setText("Tunahan");
                /*MRZInfo name = TakeName(textMrz);
                textViewName.setText(name.getPrimaryIdentifier());
                textViewSurname.setText(name.getSecondaryIdentifier());*/

                //textViewName.setText();

            } else {

            }
        }

    }
    public Bitmap  FaceInfoChangem(DG2File dg2File,Bitmap bitmapm){
        Log.e("FaceInfoChangem","FaceInfoChangem methoduna girdi");

        List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
        List<FaceInfo> faceInfos = dg2File.getFaceInfos();
        for (FaceInfo faceInfo : faceInfos) {
            allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
        }
        if (!allFaceImageInfos.isEmpty()) {
            FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();

            int imageLength = faceImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            try {
                dataInputStream.readFully(buffer, 0, imageLength);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

            bitmapm = BitmapFactory.decodeStream(inputStream);


        }
        return bitmapm;
    }

    public void enableOrDisable(){


        textViewNfcRead = findViewById(R.id.textNFC);
        textViewnameTitle= findViewById(R.id.textNameTitle);
        textViewSurnameTitle = findViewById(R.id.textSurnameTitle);
        textViewName = findViewById(R.id.textName);
        textViewSurname = findViewById(R.id.textSurname);
        textViewDateOfBirthTitle = findViewById(R.id.textDateOfBirthTitle);
        textViewDateOfBirth = findViewById(R.id.textDateOfBirth);
        textViewDocumentNoTitle = findViewById(R.id.textdocumentNumberTitle);
        textViewDocumentNo = findViewById(R.id.textdocumentNumber);
        textViewMrzTitle = findViewById(R.id.textMrzTitle);
        textViewMrz = findViewById(R.id.textMRZ);


        textViewnameTitle.setVisibility(View.GONE);
        textViewSurnameTitle.setVisibility(View.GONE);
        textViewName.setVisibility(View.GONE);
        textViewSurname.setVisibility(View.GONE);
        textViewDateOfBirthTitle.setVisibility(View.GONE);
        textViewDateOfBirth.setVisibility(View.GONE);
        textViewDocumentNoTitle.setVisibility(View.GONE);
        textViewDocumentNo.setVisibility(View.GONE);
        textViewMrzTitle.setVisibility(View.GONE);
        textViewMrz.setVisibility(View.GONE);

    }

    public void enable(){

        textViewnameTitle.setVisibility(View.VISIBLE);
        textViewSurnameTitle.setVisibility(View.VISIBLE);
        textViewName.setVisibility(View.VISIBLE);
        textViewSurname.setVisibility(View.VISIBLE);
        textViewDateOfBirthTitle.setVisibility(View.VISIBLE);
        textViewDateOfBirth.setVisibility(View.VISIBLE);
        textViewDocumentNoTitle.setVisibility(View.VISIBLE);
        textViewDocumentNo.setVisibility(View.VISIBLE);
        textViewMrzTitle.setVisibility(View.VISIBLE);
        textViewMrz.setVisibility(View.VISIBLE);

    }

   /* public MRZInfo TakeName(String textMRZ){

            MRZInfo mrzInfoz = new MRZInfo(textMRZ);
            return mrzInfoz;
    }*/
}