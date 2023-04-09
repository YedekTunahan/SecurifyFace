package com.example.ergdeneme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class FirstPageFragment extends Fragment {


   private Button SelectImage;
   private Button Camera;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View design = inflater.inflate(R.layout.fragment_first_page, container, false);

        //Tasarımın yapılacak yeri

        SelectImage = design.findViewById(R.id.SelectImage);
        Camera = design.findViewById(R.id.Camera);

        SelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Navigation.findNavController(v).navigate(R.id.toGalery);
            }
        });

        Camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.toCamera);
            }
        });


        return design;
    }
}