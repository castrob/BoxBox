package br.pucminas.castro.boxbox;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class BoxDetectionFragment extends Fragment{
    ImageView imageView;
    Bitmap bitmap;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.box_detection_fragment,container,false);
        imageView = v.findViewById(R.id.boxImage);
        setHasOptionsMenu(true);
        // Assegurando que nada e' null e pegando array de bytes da imagem
        if (getArguments() != null) {
            byte[] imgBytes = getArguments().getByteArray("IMAGE");
            if (imgBytes != null) {
                bitmap = BitmapFactory.decodeByteArray(imgBytes,0,imgBytes.length);
                try{
                    detectEdges();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Box Detection");

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_settings));
        fab.setOnClickListener(null);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doTheDialogThing();
            }
        });
    }

    private void detectEdges() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoft = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrong = sharedPreferences.getInt("cannyStrong", 100);

        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.Canny(edges, edges, cannySoft, cannyStrong);
        Toast.makeText(getActivity(), "Running Canny with Values (" + cannySoft + ", " + cannyStrong + ") ", Toast.LENGTH_SHORT).show();

        // Don't do that at home or work it's for visualization purpose.
        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, resultBitmap);
        imageView.setImageBitmap(resultBitmap);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        doTheDialogThing();
        return true;
    }

    public void doTheDialogThing(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.settings_dialog);
        dialog.setTitle("User Settings");

        final TextView cannySoft = dialog.findViewById(R.id.actualCannyLow);
        final TextView cannyStrong = dialog.findViewById(R.id.actualCannyStrong);

        final SeekBar cannySoftSeekBar = dialog.findViewById(R.id.cannyLowBorders);
        final SeekBar cannyStrongSeekBar = dialog.findViewById(R.id.cannyStrogBorders);
        cannySoftSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cannySoft.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        cannyStrongSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cannyStrong.setText(progress+"");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        cannySoftSeekBar.setProgress(50);
        cannyStrongSeekBar.setProgress(180);

        Button ok = dialog.findViewById(R.id.dialogOk);
        Button cancel = dialog.findViewById(R.id.dialogCancel);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt("cannySoft", cannySoftSeekBar.getProgress());
                sharedPreferencesEditor.putInt("cannyStrong", cannyStrongSeekBar.getProgress());
                sharedPreferencesEditor.commit();
                detectEdges();
                Toast.makeText(getActivity(), "New SharedPreferences stored!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}