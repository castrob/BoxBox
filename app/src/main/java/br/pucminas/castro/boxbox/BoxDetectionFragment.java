package br.pucminas.castro.boxbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class BoxDetectionFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.box_detection_fragment,container,false);
        ImageView imageView = v.findViewById(R.id.boxImage);

        Bundle bundle = this.getArguments();
        byte[] imgBytes = bundle.getByteArray("IMAGE");
        bundle.putByteArray("IMAGE", null);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes,0,imgBytes.length);

        Bitmap bmpGrayscale = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        int pixel, a, r, g, b;

        for(int x = 0; x < bmpGrayscale.getWidth(); x++)
            for(int y = 0; y < bmpGrayscale.getHeight(); y++){
                pixel = bitmap.getPixel(x, y);

                a = Color.alpha(pixel);
                r = Color.red(pixel);
                g = Color.green(pixel);
                b = Color.blue(pixel);

                r = g = b = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                bmpGrayscale.setPixel(x, y, Color.argb(a,r,g,b));
            }

        imageView.setImageBitmap(bmpGrayscale);

        bitmap = null;
        imgBytes = null;
        bmpGrayscale = null;
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Box Detection");
        FloatingActionButton fab_camera = getActivity().findViewById(R.id.fab_camera);
        fab_camera.hide();
        FloatingActionButton fab_gallery = getActivity().findViewById(R.id.fab_gallery);
        fab_gallery.hide();
    }
}
