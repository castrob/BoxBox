package br.pucminas.castro.boxbox;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import static android.app.Activity.RESULT_OK;

public class GalleryFragment extends Fragment{
    @Nullable
    ImageView imageView;
    Bitmap bitmap;
    FloatingActionButton fab;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.gallery_fragment, container, false);
        //Pegando bundle com a Imagem em bytes[]
        Bundle bundle =  this.getArguments();
        final byte[] imgBytes;
        //instanciando elementos do fragment
        imageView = v.findViewById(R.id.image_ViewGallery);
        fab = getActivity().findViewById(R.id.fab);

        if (bundle != null) {
            imgBytes = bundle.getByteArray("IMAGE");
            if (imgBytes != null) {
                bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                //Clearing memory
                bundle.clear();
                bundle = null;
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Fragment fragment = new BoxDetectionFragment();
                        Bundle b = new Bundle();
                        b.putByteArray("IMAGE", imgBytes);
                        fragment.setArguments(b);
                        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.content_main, fragment);
                        ft.commit();
                    }
                });
            }
        }
        return v;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.gallery);

        if(imageView != null && bitmap != null){
            imageView.setImageBitmap(bitmap);
            bitmap = null;
        }
    }
}
