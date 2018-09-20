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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.gallery_fragment, container, false);
        Bundle bundle =  this.getArguments();
        final byte[] imgBytes = bundle.getByteArray("IMAGE");
        bundle.putByteArray("IMAGE", null);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes,0,imgBytes.length);
        imageView = (ImageView) v.findViewById(R.id.image_ViewGallery);
        imageView.setImageBitmap(bitmap);
        bitmap = null;
        FloatingActionButton fab_gallery = getActivity().findViewById(R.id.fab_gallery);
        fab_gallery.setOnClickListener(new View.OnClickListener() {
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

        return v;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.gallery);
    }
}
