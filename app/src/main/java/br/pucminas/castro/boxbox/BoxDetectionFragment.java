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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

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
        //Method canny to find edges.
        Imgproc.Canny(edges, edges, cannySoft, cannyStrong,3,false);
        Toast.makeText(getActivity(), "Running Canny with Values (" + cannySoft + ", " + cannyStrong + ") ", Toast.LENGTH_SHORT).show();
        Mat cdst = new Mat();
        Imgproc.cvtColor(edges, cdst, Imgproc.COLOR_GRAY2BGR);
        Mat cdstP = cdst.clone();
        //Now using Hough Probabilistic Line Transform.
        // Probabilistic Line Transform.
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(edges, linesP, 1, Math.PI/180, 50,20,10); // runs the actual detection
        ArrayList<Linhas> linhas = new ArrayList<Linhas>();
        Linhas linha;
        // Draw the lines
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 1, Imgproc.LINE_AA, 0);
            linha = new Linhas(new Point(l[0], l[1]), new Point(l[2], l[3]));
            linhas.add(linha);
        }
        findBoxes(linhas,0.2);
        // Don't do that at home or work it's for visualization purpose.
        Bitmap resultBitmap = Bitmap.createBitmap(cdstP.cols(), cdstP.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cdstP, resultBitmap);
        imageView.setImageBitmap(resultBitmap);
    }

    private void findBoxes(ArrayList<Linhas> linhas,double diferencaCoeficiente) {

        ArrayList<LinhasParalelas> todasLinhasParalelas = new ArrayList<LinhasParalelas>();
        LinhasParalelas linhasParalelas;

        for(int i = 0; i < linhas.size(); i++){
            linhasParalelas = new LinhasParalelas();
            linhasParalelas.linhas.add(linhas.get(i));
            System.out.println("A Teste: " + linhas.get(i).a);
            linhas.remove(i);
            i--;
            for(int j = i+1;  j < linhas.size();j++){
                double coeficienteAngular1 = linhasParalelas.linhas.get(0).a;
                double coeficienteAngular2 = linhas.get(j).a;
                if((coeficienteAngular1 - diferencaCoeficiente) <= coeficienteAngular2 && (coeficienteAngular1 + diferencaCoeficiente) >= coeficienteAngular2 ) {
                    linhasParalelas.linhas.add(linhas.get(j));
                    linhas.remove(j);
                    j--;
                }
            }

            if(linhasParalelas.linhas.size() >= 3) {
                System.out.println("Passo aqui");
                todasLinhasParalelas.add(linhasParalelas);
                for(int p = 0; p < linhasParalelas.linhas.size();p++){
                    System.out.println("Valor: " + p + ": " + linhasParalelas.linhas.get(p).a);
                }
            }
        }

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