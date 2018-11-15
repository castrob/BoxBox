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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BoxDetectionFragment extends Fragment{
    ImageView imageView;
    Bitmap bitmap;
    ArrayList<LinhasParalelas> linhasParalelas;
    boolean flag;
    Mat cdstP;
    double tamanho;
    ArrayList<Linhas> TodasLinhas;

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
                imageView.setImageBitmap(bitmap);
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
        //Realizar o procedimento de detecção de bordas após o fragment ter carregado.
        try{
            detectEdges();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void detectEdges() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoft = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrong = sharedPreferences.getInt("cannyStrong", 100);

        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);
        Size size = rgba.size();
        tamanho = ((size.height + size.width) / 100)*6;
        double tamanho2 = ((size.height + size.width) / 100)*1;
        System.out.println(tamanho);
        Mat edges = new Mat();//(rgba.size(), CvType.CV_8UC3);
        //bilateralFilter.
        Imgproc.cvtColor(rgba,rgba,Imgproc.COLOR_BGRA2BGR);
        Imgproc.bilateralFilter(rgba,edges,6,75,75);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        //GaussianBlur (3,3)
        //Imgproc.GaussianBlur(edges,edges,new Size(3,3),0);

        //Method canny to find edges.
        Imgproc.Canny(edges, edges, cannySoft, cannyStrong,3,false);
        Toast.makeText(getActivity(), "Running Canny with Values (" + cannySoft + ", " + cannyStrong + ") ", Toast.LENGTH_SHORT).show();

        //Dilate
        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        //Imgproc.erode(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

        //Copy the image for hough transform
        cdstP = new Mat();
        cdstP = rgba.clone();


        // Now using Hough Probabilistic Line Transform.
        // Probabilistic Line Transform.
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(edges, linesP, 1, Math.PI/180, 50,tamanho,(int)tamanho2); // runs the actual detection
        ArrayList<Linhas> linhas = new ArrayList<Linhas>();
        Linhas linha;
        // Draw the lines
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            //Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 255, 0), 1, Imgproc.LINE_AA, 0);
            linha = new Linhas(new Point(l[0], l[1]), new Point(l[2], l[3]));
            linhas.add(linha);
        }
        System.out.println("Size total:" + linhas.size() );
        linhasParalelas = findGroups(linhas,0.2);
        if(linhasParalelas.size() >= 3) {
            System.out.println(linhasParalelas.size());
            int[] x = new int[linhasParalelas.size()];
            flag = false;
            /*for(int i = 0; i < linhasParalelas.size(); i++){
                Imgproc.line(cdstP, linhasParalelas.get(i).linhas.get(0).primeiro, linhasParalelas.get(i).linhas.get(0).ultimo, new Scalar(255, 0, 0), 1, Imgproc.LINE_AA, 0);
            }*/
            //combinationTodasRetas(3, 3, x, 0, 0);
            TodasLinhas = new ArrayList<Linhas>();
            outroMetodo();

        }
        int cont = 0;
       /* for(int i = 0; i < linhasParalelas.size();i++){
            for(int j = 0; j < linhasParalelas.get(i).linhas.size();j++){
                //Imgproc.line(cdstP,linhasParalelas.get(i).linhas.get(j).primeiro, linhasParalelas.get(i).linhas.get(j).ultimo, new Scalar(255, 0, 0), 1, Imgproc.LINE_AA, 0);
                cont++;
            }
        }*/
        System.out.println("Deixou: " + cont);
        Bitmap resultBitmap = Bitmap.createBitmap(cdstP.cols(), cdstP.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cdstP, resultBitmap);
        imageView.setImageBitmap(resultBitmap);
    }

    private void outroMetodo() {
        int posicao = 0;
        for(int i = 0; i < linhasParalelas.size(); i++) {
            if(linhasParalelas.get(i).linhas.get(0).tipoReta == 2) {
                posicao = i;
            }
        }
        for(int i = 0; i < linhasParalelas.size(); i++) {
            if (i != posicao) {
                for (int j = 0; j < linhasParalelas.get(i).linhas.size(); j++) {
                    TodasLinhas.add(linhasParalelas.get(i).linhas.get(j));
                }
            }
        }
        for(int i = 0; i < linhasParalelas.get(posicao).linhas.size(); i++) {
            System.out.println(linhasParalelas.get(posicao).linhas.get(i).primeiro.x + "TipoReta: " + linhasParalelas.get(posicao).linhas.get(i).tipoReta );
        }
        int[] x = new int[linhasParalelas.get(posicao).linhas.size()];
        combinationNoventaGraus(linhasParalelas.get(posicao).linhas.size(),3,x,0,0,posicao);
    }

    private void combinationNoventaGraus(int n, int r, int x [], int next, int k,int posicao) {
        int i;
        if(k == r && flag != true) {
            int tmp = acharCaixa(x,posicao);
            if(tmp == 0) {
                System.out.println("Achou a caixita");
            }
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinationNoventaGraus(n,r,x,i+1,k+1,posicao);
            }
        }
    }

    private int acharCaixa(int[] x,int posicao) {
        int [] retas = new int[6];
        for(int i = 0; i < 6; i++) {
            retas[i] = -1;
        }
        int tmp1 = acharRetas(linhasParalelas.get(0).linhas.get(x[0]).primeiro,linhasParalelas.get(0).linhas.get(x[1]).primeiro,linhasParalelas.get(0).linhas.get(x[1]).ultimo);
        int tmp2 = acharRetas(linhasParalelas.get(0).linhas.get(x[0]).ultimo,linhasParalelas.get(0).linhas.get(x[1]).primeiro,linhasParalelas.get(0).linhas.get(x[1]).ultimo);
        if(tmp1 == -1 || tmp2 == -1) {
            return 1;
        }
        retas[0] = tmp1;
        retas[1] = tmp2;
        tmp1 = acharRetas(linhasParalelas.get(0).linhas.get(x[1]).primeiro,linhasParalelas.get(0).linhas.get(x[2]).primeiro,linhasParalelas.get(0).linhas.get(x[2]).ultimo);
        tmp2 = acharRetas(linhasParalelas.get(0).linhas.get(x[1]).ultimo,linhasParalelas.get(0).linhas.get(x[2]).primeiro,linhasParalelas.get(0).linhas.get(x[2]).ultimo);
        if(tmp1 == -1 || tmp2 == -1) {
            return 1;
        }
        retas[2] = tmp1;
        retas[3] = tmp2;
        boolean test = acharRetasFinais(linhasParalelas.get(0).linhas.get(x[0]).primeiro,linhasParalelas.get(0).linhas.get(x[0]).ultimo,linhasParalelas.get(0).linhas.get(x[2]).primeiro,linhasParalelas.get(0).linhas.get(x[2]).ultimo,retas);

        if(test) {
            flag = true;

            for(int i = 0; i < 3; i++){
                Imgproc.line(cdstP, linhasParalelas.get(posicao).linhas.get(x[i]).primeiro, linhasParalelas.get(posicao).linhas.get(x[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
            for(int i = 0; i < 6; i++){
                Imgproc.line(cdstP, TodasLinhas.get(retas[i]).primeiro,TodasLinhas.get(retas[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
        }

        return 0;
    }

    private boolean acharRetasFinais(Point primeiro, Point ultimo, Point primeiro1, Point ultimo1, int[] retas) {
        boolean resp = false;
        int [] tmp = new int [2];
        tmp[0] = -1;
        tmp[1] = -1;
        ArrayList<Integer> retasPrimeiro = new ArrayList<>();
        ArrayList<Integer> retasUltimo = new ArrayList<>();

        for(int i = 0; i < TodasLinhas.size(); i++) {
            if(!retasEncontrada(i,retas,retasPrimeiro,retasUltimo)) {
                if(distanciaEuclidiana(primeiro,TodasLinhas.get(i).primeiro) <= tamanho || distanciaEuclidiana(primeiro,TodasLinhas.get(i).ultimo) <= tamanho) {
                    retasPrimeiro.add(i);
                }
                if(distanciaEuclidiana(ultimo,TodasLinhas.get(i).primeiro) <= tamanho || distanciaEuclidiana(ultimo,TodasLinhas.get(i).ultimo) <= tamanho) {
                    retasPrimeiro.add(i);
                }

                if(distanciaEuclidiana(primeiro1,TodasLinhas.get(i).primeiro) <= tamanho || distanciaEuclidiana(primeiro1,TodasLinhas.get(i).ultimo) <= tamanho) {
                    retasUltimo.add(i);
                }
                if(distanciaEuclidiana(ultimo1,TodasLinhas.get(i).primeiro) <= tamanho || distanciaEuclidiana(ultimo1,TodasLinhas.get(i).ultimo) <= tamanho) {
                    retasUltimo.add(i);
                }
            }
        }

        for(int i = 0; i < retasPrimeiro.size(); i++) {
            for(int j = 0; j < retasUltimo.size(); j++) {
                if(distanciaEuclidiana(TodasLinhas.get(retasPrimeiro.get(i)).primeiro,TodasLinhas.get(retasUltimo.get(j)).primeiro) <= tamanho) {
                   retas[4] = retasPrimeiro.get(i);
                   retas[5] = retasUltimo.get(j);
                   resp = true;
                   break;
                }
                if(distanciaEuclidiana(TodasLinhas.get(retasPrimeiro.get(i)).primeiro,TodasLinhas.get(retasUltimo.get(j)).ultimo) <= tamanho) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
                if(distanciaEuclidiana(TodasLinhas.get(retasPrimeiro.get(i)).ultimo,TodasLinhas.get(retasUltimo.get(j)).primeiro) <= tamanho) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
                if(distanciaEuclidiana(TodasLinhas.get(retasPrimeiro.get(i)).ultimo,TodasLinhas.get(retasUltimo.get(j)).ultimo) <= tamanho) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
            }
            if(resp) {
                break;
            }
        }
        return resp;
    }

    private boolean retasEncontrada(int x, int[] retas, ArrayList<Integer> retasPrimeiro, ArrayList<Integer> retasUltimo) {
        for(int i = 0; i < retas.length; i++) {
            if(x == retas[i]) {
                return true;
            }
        }
        for(int i = 0; i < retasPrimeiro.size(); i++) {
            if(x == retasPrimeiro.get(i)) {
                return true;
            }
        }
        for(int i = 0; i < retasUltimo.size(); i++) {
            if(x == retasUltimo.get(i)) {
                return true;
            }
        }
        return false;
    }

    private  int acharRetas(Point primeiro, Point segundo1, Point segundo2) {
        int resp = -1;
        for(int i = 0; i < TodasLinhas.size(); i++) {
            if(distanciaEuclidiana(primeiro,TodasLinhas.get(i).primeiro) <= tamanho && distanciaEuclidiana(segundo1,TodasLinhas.get(i).ultimo) <= tamanho) {
                resp = i;
                break;
            }else if(distanciaEuclidiana(primeiro,TodasLinhas.get(i).ultimo) <= tamanho && distanciaEuclidiana(segundo1,TodasLinhas.get(i).primeiro) <= tamanho) {
                resp = i;
                break;
            }

        }
        if(resp != -1) {
            return resp;
        }
        for(int i = 0; i < TodasLinhas.size(); i++) {
            if(distanciaEuclidiana(primeiro,TodasLinhas.get(i).primeiro) <= tamanho && distanciaEuclidiana(segundo2,TodasLinhas.get(i).ultimo) <= tamanho) {
                resp = i;
                break;
            }else if(distanciaEuclidiana(primeiro,TodasLinhas.get(i).ultimo) <= tamanho && distanciaEuclidiana(segundo2,TodasLinhas.get(i).primeiro) <= tamanho) {
                resp = i;
                break;
            }
        }
        return resp;
    }


    private void combinationTodasRetas(int n, int r, int x [],int next,int k) {
        int i;
        if(k == r && flag != true) {
            //System.out.println("x 0 : " + x[0] );
            int size = linhasParalelas.get(x[0]).linhas.size();
            int [] tmp = new int [size];
            combination1(size,3,tmp,0,0,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinationTodasRetas(n,r,x,i+1,k+1);
            }
        }
    }

    private void combination1(int n, int r, int x [], int next, int k, int [] vetorRetasParalelas) {
        int i;
        if(k == r && flag != true) {
            //System.out.println("x 1 : " + x[1] );
            int size = linhasParalelas.get(vetorRetasParalelas[1]).linhas.size();
            int [] tmp = new int [size];
            combination2(size,3,tmp,0,0,vetorRetasParalelas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combination1(n,r,x,i+1,k+1,vetorRetasParalelas);
            }
        }
    }

    private void combination2(int n , int r, int x [] , int next, int k, int [] vetorRetasParalelas, int [] primeirasRetas) {
        int i;
        if(k == r && flag != true) {
            //System.out.println("x 2 : " + x[2] );
            int size = linhasParalelas.get(vetorRetasParalelas[2]).linhas.size();
            int [] tmp = new int [size];
            combination3(size,3,tmp,0,0,vetorRetasParalelas,primeirasRetas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combination2(n,r,x,i+1,k+1,vetorRetasParalelas,primeirasRetas);
            }
        }
    }

    private void combination3(int n, int r, int x [], int next, int k, int [] vetorRetasParalelas, int [] primeirasRetas, int [] segundasRetas) {
        int i;
        if(k == r && flag != true) {
            findBoxes(vetorRetasParalelas,primeirasRetas,segundasRetas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combination3(n,r,x,i+1,k+1,vetorRetasParalelas,primeirasRetas,segundasRetas);
            }
        }
    }

    private void findBoxes(int [] vetorRetasParalelas, int [] primeirasRetas, int [] segundasRetas, int [] terceirasRetas) {
        ArrayList<Point> pontos = new ArrayList<Point>();
        //ArrayList<Boolean> passoTodos = new ArrayList<Boolean>();
        ArrayList<Integer> passoTodos = new ArrayList<Integer>();
        //boolean tmp = false;
        int tmp = 0;
        for(int i = 0; i < 3; i++){
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).primeiro,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).primeiro);
                passoTodos.add(tmp);

            }
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).ultimo,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).ultimo);
                passoTodos.add(tmp);
            }
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).primeiro,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).primeiro);
                passoTodos.add(tmp);
            }
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).ultimo,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).ultimo);
                passoTodos.add(tmp);
            }
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).primeiro,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).primeiro);
                passoTodos.add(tmp);
            }
            if(!acharPontos(linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).ultimo,pontos,passoTodos)) {
                pontos.add(linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).ultimo);
                passoTodos.add(tmp);
            }
        }
        //System.out.println("Contador : " + pontos.size() + "True :" + passoTodos.size());
        if(pontos.size() == 7 && allTrue(passoTodos)) { // Para testar, mudar esses valores.
            flag = true; // Para testar comente isso <<.

            for(int i = 0; i < 3; i++){
                Imgproc.line(cdstP, linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).primeiro, linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
            for(int i = 0; i < 3; i++){
                Imgproc.line(cdstP, linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).primeiro, linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
            for(int i = 0; i < 3; i++){
                Imgproc.line(cdstP, linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).primeiro, linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
        }
        //System.out.println("Começou aqui");
        /*for(int i = 0; i < 7; i++){
            //System.out.println("Pontos: " + passoTodos.get(i));
        }*/
        //System.out.println("Terminou aqui");
    }

    private boolean allTrue(ArrayList<Integer> passoTodos) {
        int contador = 0;
        for(int i = 0; i < passoTodos.size();i++) {

            if(passoTodos.get(i) == 1) {
                //System.out.println("Numeros: "+passoTodos.get(i));
                contador++;
            }
        }
        if(contador != 3) {
            return false;
        }
        for(int i = 0; i < passoTodos.size();i++) {

            if(passoTodos.get(i) == 2) {
                //System.out.println("Numeros: "+passoTodos.get(i));
                contador++;
            }
        }
        if(contador != 7) {
            return false;
        }
        return true;
    }

    private boolean acharPontos(Point ponto, ArrayList<Point> points, ArrayList<Integer> tmp) {
        boolean resp = false;
        double distancia = tamanho;
        int tmp1;
        for(int i = 0; i < points.size();i++) {
            if(distanciaEuclidiana(points.get(i),ponto) <= distancia) {
                resp = true;
                tmp1 = tmp.get(i);
                tmp.set(i,tmp1+1);
            }
        }
        return resp;
    }


    private double distanciaEuclidiana(Point um, Point dois) {

        double distancia = Math.sqrt(Math.pow(dois.x - um.x, 2) + Math.pow(dois.y - um.y, 2));
        return distancia;
    }
    private ArrayList<LinhasParalelas> findGroups(ArrayList<Linhas> linhas,double diferencaCoeficiente) {

        ArrayList<LinhasParalelas> todasLinhasParalelas = new ArrayList<LinhasParalelas>();
        LinhasParalelas linhasParalelas;
        int cont = 0;
        for(int i = 0; i < linhas.size(); i++){
            linhasParalelas = new LinhasParalelas();
            linhasParalelas.linhas.add(linhas.get(i));
            //System.out.println("A Teste: " + linhas.get(i).a);
            linhas.remove(i);
            i--;
            for(int j = i+1;  j < linhas.size();j++){
                /*double coeficienteAngular1 = linhasParalelas.linhas.get(0).a;
                double coeficienteAngular2 = linhas.get(j).a;
                if((coeficienteAngular1 - diferencaCoeficiente) <= coeficienteAngular2 && (coeficienteAngular1 + diferencaCoeficiente) >= coeficienteAngular2 ) {
                    linhasParalelas.linhas.add(linhas.get(j));
                    linhas.remove(j);
                    j--;
                }*/
                if(linhasParalelas.linhas.get(0).tipoReta == linhas.get(j).tipoReta) {
                    //cont++;
                    //System.out.println("Entro aqui");
                    /*if (saoIguais(linhasParalelas.linhas.get(0), linhas.get(j))) {
                        //System.out.println("Entro aqui1");
                        linhas.remove(j);
                        j--;
                    } else {*/
                        //System.out.println("Entro aqui2");
                    linhasParalelas.linhas.add(linhas.get(j));
                    linhas.remove(j);
                    j--;
                    //}

                }
            }
            for(int j = 0; j < linhasParalelas.linhas.size(); j++) {
                Linhas linha1 = linhasParalelas.linhas.get(j);
                for(int z = j+1; z < linhasParalelas.linhas.size();z++ ){
                    Linhas linha2 = linhasParalelas.linhas.get(z);
                    if (saoIguais(linha1, linha2)) {
                        //System.out.println("Entro aqui1");
                        linhasParalelas.linhas.remove(z);
                        z--;
                    }
                }
            }
            if(linhasParalelas.linhas.size() >= 3) {
                //System.out.println("Passo aqui");
                Collections.sort(linhasParalelas.linhas, new Comparator<Linhas>() {
                    public int compare(Linhas v1, Linhas v2) {
                        double tmp = v1.primeiro.x - v2.primeiro.x;
                        return (int)tmp;
                    }
                });
                todasLinhasParalelas.add(linhasParalelas);
                //System.out.println("Valor dentro: "  + ": " + linhasParalelas.linhas.get(0).tipoReta + " a: " + linhasParalelas.linhas.get(0).a );
                /*for(int p = 0; p < linhasParalelas.linhas.size();p++){

                    //Imgproc.line(cdstP,linhasParalelas.linhas.get(p).primeiro, linhasParalelas.linhas.get(p).ultimo, new Scalar(0, 255, 0), 1, Imgproc.LINE_AA, 0);
                }*/
            }/*else{
                for(int p = 0; p < linhasParalelas.linhas.size();p++){
                    System.out.println("Valor fora: "  + ": " + linhasParalelas.linhas.get(0).tipoReta + " a: " + linhasParalelas.linhas.get(0).a);
                    //Imgproc.line(cdstP,linhasParalelas.linhas.get(p).primeiro, linhasParalelas.linhas.get(p).ultimo, new Scalar(0, 255, 0), 1, Imgproc.LINE_AA, 0);
                }
            }*/
        }
        System.out.println("Contador de retas: " + cont );
        return todasLinhasParalelas;
    }

    private boolean saoIguais(Linhas linhas1, Linhas linhas2) {
        double tmp1 = distanciaEuclidiana(linhas1.primeiro,linhas2.primeiro);
        double tmp2 = distanciaEuclidiana(linhas1.ultimo,linhas2.ultimo);
        double tmp3 = distanciaEuclidiana(linhas1.primeiro,linhas2.ultimo);
        double tmp4 = distanciaEuclidiana(linhas1.ultimo,linhas2.primeiro);
        //System.out.println("Distancia tmp1: " + tmp1 + " Distancia tmp2: " + tmp2 + " Distancia tmp3: " + tmp3 + " Distancia tmp4: " + tmp4);
        if((tmp1 <= tamanho && tmp2 <= tamanho) || (tmp3 <= tamanho && tmp4 <= tamanho)) {
            //System.out.println("Distancia tmp1: " + tmp1 + " Distancia tmp2: " + tmp2 + " Distancia tmp3: " + tmp3 + " Distancia tmp4: " + tmp4);
            //Imgproc.line(cdstP, linhas1.primeiro, linhas1.ultimo, new Scalar(255, 0, 0), 1, Imgproc.LINE_AA, 0);
            //Imgproc.line(cdstP, linhas2.primeiro, linhas2.ultimo, new Scalar(0, 255, 0), 1, Imgproc.LINE_AA, 0);
            //System.out.println("Excluiu");
            return true;
        }
        return false;
    }

    private boolean tamanhoIgual(Linhas linhas1, Linhas linhas2) {

        double tmp1 = distanciaEuclidiana(linhas1.primeiro,linhas1.ultimo);
        double tmp2 = distanciaEuclidiana(linhas2.primeiro,linhas2.ultimo);
        if(tmp1+20 >= tmp2 && tmp2 >= tmp1-20) {
            System.out.println("Dentro Tamanho tmp1: " + tmp1 + " A: " + linhas1.a +  " Tamanho tmp2: " + tmp2 + " A: " + linhas2.a);
            return true;
        }else{
            System.out.println("Tamanho tmp1: " + tmp1 + " A: " + linhas1.a + " Tamanho tmp2: " + tmp2 + " A: " + linhas2.a);
            return false;
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

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoftPreviousPref = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrongPreviousPref = sharedPreferences.getInt("cannyStrong", 100);

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
        cannySoftSeekBar.setProgress(cannySoftPreviousPref);
        cannyStrongSeekBar.setProgress(cannyStrongPreviousPref);

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