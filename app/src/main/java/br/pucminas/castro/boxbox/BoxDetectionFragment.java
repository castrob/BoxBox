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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Arrays;

/**
 * Classe BoxDetectionFragment para lidar com o Fragment da tela de Deteccao de Caixas
 * Nesta classe esta presente toda a logica e heuristicas para a deteccao das caixas.
 */

public class BoxDetectionFragment extends Fragment implements View.OnClickListener {
    //Variaveis globais.
    ImageView imageView;
    Bitmap bitmap;
    FloatingActionButton fab;
    Button prev, next;
    boolean flag;
    double tamanhoDistancia;
    //double tamanhoDistancia3;
    int timeLimit, index;
    Mat cdstP, gray;
    ArrayList<LinhasParalelas> linhasParalelas;
    ArrayList<Linhas> todasLinhas;
    ArrayList<Caixas> todasCaixas;


    /**
     * Metodo construtor do fragment android que contem toda a tela de deteccao da caixa.
     * @param inflater Layout que ira inflar o xml com os objetos da tela
     * @param container ViewGroup de elementos da tela.
     * @param savedInstanceState Informacoes que podem ser salvas no ciclo de vida da tela.
     * @return retorna uma View que e um objeto contendo todos os elementos do fragment.
     */
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

                //Inflando objetos
                fab = getActivity().findViewById(R.id.fab);
                prev = v.findViewById(R.id.prev);
                next = v.findViewById(R.id.next);
                //configurando objetos e setando listeners para lidar com os clicks
                prev.setOnClickListener(this);
                next.setOnClickListener(this);
                fab.setOnClickListener(null);
                fab.setOnClickListener(this);
                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_settings));
                index = 0;
            }
        }

        return v;
    }

    /**
     * Este metodo faz parte do ciclo de vida do fragment, e e' chamado logo apos o construtor terminar,
     * com ele temos o estado do fragment pronto para qualquer operacao ser executada no caso onde comecamos a deteccao
     * da caixa.
     * @param view objeto com todos os elementos do fragment.
     * @param savedInstanceState nformacoes que podem ser salvas no ciclo de vida da tela.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Box Detection");
        //Realizar o procedimento de detecção de bordas após o fragment ter carregado.
        try{
            detectBoxes();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Metodo listener para lidar com clicks dos botoes presentes no fragment como,
     * o botao de configuracoes da deteccoes e os botoes de next e previous.
     * @param v objeto do botao que recebeu o click.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.prev:
                index--;
                index = index < 0 ? -1 * index : index;
                index = index % todasCaixas.size();
                showFoundBoxOnScreen(index);
                break;
            case R.id.next:
                index++;
                index = index % todasCaixas.size();
                showFoundBoxOnScreen(index);
                break;
            case R.id.fab:
                doTheDialogThing();
                break;
        }
    }

    /**
     * Metodo para apresentar as caixas que foram detectadas pelo algoritmo separadamente
     * @param index indice do array de caixas a ser mostrado.
     */
    private void showFoundBoxOnScreen(int index) {
        //limpando caixa detectada e pegando nova caixa
        cdstP = gray.clone();
        caixaToMat(todasCaixas.get(index));

        Bitmap resultBitmap = Bitmap.createBitmap(cdstP.cols(), cdstP.rows(), Bitmap.Config.ARGB_8888);//Pega o bitmap do resultado.
        Utils.matToBitmap(cdstP, resultBitmap);
        imageView.setImageBitmap(resultBitmap);//Coloca a imagem.
    }

    /**
     * Metodo para printar as caixas com as arestas vermelhas de largura 3
     * @param caixa Caixa a ser printada
     */
    private void caixaToMat(Caixas caixa) {
        int largura = 3;
        for(int i = 0; i < caixa.linhas.size(); i++) {
            Imgproc.line(cdstP, caixa.linhas.get(i).primeiro, caixa.linhas.get(i).ultimo, new Scalar(255, 0, 0), largura, Imgproc.LINE_AA, 0);
        }
    }

    /**
     * Metodo para Achar somente as linhas, usando canny e a transformada de hough Prababilistica.
     * Este método também é responsavel pelo tratamento da imagem, como filtro bilateral, canny e transformada de hough.
     */

    private void detectBoxes() {
        //Preparando para usar o canny.
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoft = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrong = sharedPreferences.getInt("cannyStrong", 100);
        boolean combinationHeuristic = sharedPreferences.getBoolean("combinationHeuristic", false);


        Mat rgba = new Mat(); //Pegando a imagem.
        Utils.bitmapToMat(bitmap, rgba); //Passando a imagem para bitmap.
        Size size = rgba.size(); // Pegando o tamanhoDistancia da imagem.

        tamanhoDistancia = ((size.height + size.width) / 100)*3; // Pegando o tamanhoDistancia limite para os calculos abaixo.
        double tamanhoDistancia2 = ((size.height + size.width) / 100)*1; // Pegando o tamanhoDistancia limite para o tamanhoDistancia de 2 linhas se encontrarem, para considerar uma linha so.
        //tamanhoDistancia3 =  ((size.height + size.width) / 100)*5;
        Mat edges = new Mat();//(rgba.size(), CvType.CV_8UC3);

        Imgproc.cvtColor(rgba,rgba,Imgproc.COLOR_BGRA2BGR); // Passando a imagem para usar o filtro bilateral
        Imgproc.bilateralFilter(rgba,edges,6,75,75); // Usando filtro bilateral - SigmaColor e SigmaSpace (Vizinhanca de cores)
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4); //Passando para tons de cinza.

        //Clonando a imagem em escala de cinza para ser utilizado na demonstracao da deteccao
        gray = new Mat();
        gray = edges.clone();
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGB); //voltando de escala de cinza para rgb para que a linha possa ser vermelha

        Imgproc.Canny(edges, edges, cannySoft, cannyStrong,3,false);//Aplicando filtro de Canny
        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));//Dilatando as retas encontradas.

        //Copy the image for hough transform
        cdstP = new Mat(); //Criando uma nova imagem.
        cdstP = rgba.clone();//Pegando um clone da imagem original.

        // Utilizando a Transformada de Hough Probabilistica
        Mat linesP = new Mat(); //Mat para o resultado da transformada de hough
        Imgproc.HoughLinesP(edges, linesP, 1, Math.PI/180, 50,tamanhoDistancia+10,(int)tamanhoDistancia2); // Executando a Transformada
        //Array de linhas encontradas pela t. hough
        ArrayList<Linhas> linhas = new ArrayList<Linhas>();
        Linhas linha;
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            linha = new Linhas(new Point(l[0], l[1]), new Point(l[2], l[3]));//Criar uma linha
            linhas.add(linha);//Botar a linha no vetor de linhas
        }

        timeLimit = 0;
        todasCaixas = new ArrayList<Caixas>();
        linhasParalelas = acharGrouposDeGraus(linhas); // Calcular os grupos de retas, se é 2 ou 1 ou 0.
        int tamanhoInicial,tamanhoFinal;
        int posicao = 0;
        todasLinhas = new ArrayList<Linhas>(); // Colocar todos as retas que nao sao do tipo 2.
        if(linhasParalelas.size() >= 3) { // Verificar se achou os 3 grupos.
            int[] x = new int[linhasParalelas.size()];
            flag = false;
            //Primeiro metodo para encontrar as caixas, usando 3 combinacoes.
            if (combinationHeuristic)
                do{
                    tamanhoInicial = tamanhoTotalDeLinhas();
                    flag = false;
                    combinacaoDeTodasRetas(linhasParalelas.size(), 3, x, 0, 0); // Metodo com todas combinações, principal metodo para achar a caixa.
                    tamanhoFinal = tamanhoTotalDeLinhas();
                }while(tamanhoInicial != tamanhoFinal && linhasParalelas.get(0).linhas.size() >= 3 && linhasParalelas.get(1).linhas.size() >= 3 && linhasParalelas.get(2).linhas.size() >= 3);
            else{
                //Segundo metodo para encontrar as caixas, usando uma heuristica.
                //Achar a posicao do tipo de reta = 2.
                for (int i = 0; i < linhasParalelas.size(); i++) {
                    if (linhasParalelas.get(i).linhas.get(0).tipoReta == 2) {
                        posicao = i;
                    }
                }

                for (int i = 0; i < linhasParalelas.size(); i++) {
                    if (i != posicao) {
                        for (int j = 0; j < linhasParalelas.get(i).linhas.size(); j++) {
                            todasLinhas.add(linhasParalelas.get(i).linhas.get(j));
                        }
                    }
                }
                do {
                    flag = false;
                    tamanhoInicial = tamanhoTotalDeLinhasParalelas(posicao);
                    segundaHeuristica(posicao);//Outro metodo para encontrar a caixa.
                    tamanhoFinal = tamanhoTotalDeLinhasParalelas(posicao);
                }
                while ( tamanhoInicial != tamanhoFinal && (linhasParalelas.get(posicao).linhas.size() >= 3 && todasLinhas.size() >= 6));
            }
        }

        Toast.makeText(getActivity(), todasCaixas.size() + " Caixa(s) encontrada(s)", Toast.LENGTH_SHORT).show();//Mensagem de Caixas encontradas.
        //caso possua mais de uma caixa, mostrar botoes de navegacao
        if(todasCaixas.size() > 1){
            prev.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
        }

        //chamando metodo para mostrar a primeira caixa encontrada pelo algoritmo
        showFoundBoxOnScreen(0);
    }

    /**
     * Pegar tamanho do vetor para primeira heuristica
     * @return
     */
    private int tamanhoTotalDeLinhas() {
        int resp = 0;
        for(int i = 0; i < linhasParalelas.size(); i++) {
            resp = resp + linhasParalelas.get(i).linhas.size();
        }
        return resp;
    }

    /**
     * Pegar tamanho do vetor para segunda heuristica
     * @param posicao posicao do vetor de linha paralelas
     * @return
     */
    private int tamanhoTotalDeLinhasParalelas(int posicao) {
        return linhasParalelas.get(posicao).linhas.size() + todasLinhas.size();
    }


    /**
     * Metodo para comecar a segunda heuristica.
     * @param posicao e a posicao da lista que as retas de 90 graus se encontram.
     */
    private void segundaHeuristica(int posicao) {
        int[] x = new int[linhasParalelas.get(posicao).linhas.size()];
        combinacaoDeNoventaGraus(linhasParalelas.get(posicao).linhas.size(),3,x,0,0,posicao);
    }

    /**
     * Metodo para fazer a combinacao das retas de 90 graus para o calculo da segunda heuristica.
     * @param n tamanho da lista.
     * @param r tamanho do grupo.
     * @param x vetor auxiliar.
     * @param next proxima posicao que a combinacao irar comecar.
     * @param k verificador para ver se o grupo esta cheio.
     * @param posicao e a posicao da lista que as retas de 90 graus se encontram.
     */
    private void combinacaoDeNoventaGraus(int n, int r, int x [], int next, int k,int posicao) {
        int i;
        if(k == r && flag != true) {
            int tmp = acharCaixa(x,posicao); // se tmp = 0 entao achou a caixa
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinacaoDeNoventaGraus(n,r,x,i+1,k+1,posicao);
            }
        }
    }

    /**
     * Metodo que comeca a fazer a segunda heuristica.
     * @param x vetor de posicoes , vindo da combinacao.
     * @param posicao e a posicao da lista que as retas de 90 graus se encontram.
     * @return int 0 para achou uma caixa, se nao outra valor para falar que nao achou.
     */
    private int acharCaixa(int[] x,int posicao) {

        int [] retas = new int[6]; //Vetor de inteiros, que acham as posicoes das retas, no vetor de todasLinhas
        //Setar todos como nao encontrados.
        for(int i = 0; i < 6; i++) {
            retas[i] = -1;
        }

        int teste1 = acharRetas(linhasParalelas.get(posicao).linhas.get(x[0]).primeiro,linhasParalelas.get(posicao).linhas.get(x[1]).primeiro,linhasParalelas.get(posicao).linhas.get(x[1]).ultimo,retas);//Busca a primeira Reta.

        int teste2 = acharRetas(linhasParalelas.get(posicao).linhas.get(x[0]).ultimo,linhasParalelas.get(posicao).linhas.get(x[1]).primeiro,linhasParalelas.get(posicao).linhas.get(x[1]).ultimo,retas); //Busca a segunda Reta.

        //Vericiar se encontrou a reta.
        if(teste1 == -1 || teste2 == -1) {
            return 1;
        }
        //Colocando as retas no vetor de posicoes das retas.
        retas[0] = teste1;
        retas[1] = teste2;

        teste1 = acharRetas(linhasParalelas.get(posicao).linhas.get(x[1]).primeiro,linhasParalelas.get(posicao).linhas.get(x[2]).primeiro,linhasParalelas.get(posicao).linhas.get(x[2]).ultimo,retas);//Busca a terceira Reta.

        teste2 = acharRetas(linhasParalelas.get(posicao).linhas.get(x[1]).ultimo,linhasParalelas.get(posicao).linhas.get(x[2]).primeiro,linhasParalelas.get(posicao).linhas.get(x[2]).ultimo,retas);//Busca a quarta Reta.

        if(teste1 == -1 || teste2 == -1) {
            return 1;
        }
        //Colocando as retas no vetor de posicoes das retas.
        retas[2] = teste1;
        retas[3] = teste2;

        //Ultimo teste, verifica se acha as duas ultimas retas.
        boolean test = acharRetasFinais(linhasParalelas.get(posicao).linhas.get(x[0]).primeiro,linhasParalelas.get(posicao).linhas.get(x[0]).ultimo,linhasParalelas.get(posicao).linhas.get(x[2]).primeiro,linhasParalelas.get(posicao).linhas.get(x[2]).ultimo,retas);
        //Caso o teste volto positivo, achou uma caixa.
        if(test) {
            flag = true;
            //Mostrar todas as retas da caixa.
            for(int i = 0; i < 3; i++){
                Imgproc.line(cdstP, linhasParalelas.get(posicao).linhas.get(x[i]).primeiro, linhasParalelas.get(posicao).linhas.get(x[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
            for(int i = 0; i < 6; i++){
                Imgproc.line(cdstP, todasLinhas.get(retas[i]).primeiro,todasLinhas.get(retas[i]).ultimo, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            }
            //Ordenando o vetor de retas, para que nao haja nenhum erro ao retilar elas na lista.
            Arrays.sort(retas);
            Caixas caixa = new Caixas();
            //Retirando as retas na lista de 90 graus
            for(int i = 2; i >= 0; i--) {
                caixa.linhas.add(linhasParalelas.get(posicao).linhas.get(x[i]));
                linhasParalelas.get(posicao).linhas.remove(x[i]);
            }
            //Retirando as retas na lista das retas restantes
            for(int i = 5; i>= 0; i--) {
                caixa.linhas.add(todasLinhas.get(retas[i]));
                todasLinhas.remove((retas[i]));
            }
            //Adicionando a caixa no vetor de caixas.
            todasCaixas.add(caixa);
        }

        return 0;
    }

    /**
     * Metodo para encontrar retas, dado 3 pontos, 1 e da reta principal o outros dois e da reta secundaria, verifica se a uma reta que tem 2 pontos em comum.
     * Nesse metodo ele pega o Primeiro ponto, que serve como um ponto ja encontrada, e depois pega os outros 2 pontos Segundo1 e Segundo2 para verifica se tem um reta do ponto Primeiro a Segundo1 ou Primeiro a Segundo2.
     * @param primeiro primeiro ponto.
     * @param segundo1 segundo ponto inicial.
     * @param segundo2 segundo ponto final.
     * @param retas vetor de retas ja encontradas, para que nao repita as retas.
     * @return int a posicao da reta encontrada.
     */
    private  int acharRetas(Point primeiro, Point segundo1, Point segundo2,int [] retas) {
        int resp = -1;
        for(int i = 0; i < todasLinhas.size(); i++) {
            if(distanciaEuclidiana(primeiro,todasLinhas.get(i).primeiro) <= tamanhoDistancia && distanciaEuclidiana(segundo1,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontradasComeco(i,retas)) {
                resp = i;
                break;
            }else if(distanciaEuclidiana(primeiro,todasLinhas.get(i).ultimo) <= tamanhoDistancia && distanciaEuclidiana(segundo1,todasLinhas.get(i).primeiro) <= tamanhoDistancia && !retasEncontradasComeco(i,retas)) {
                resp = i;
                break;
            }

        }
        if(resp != -1) {
            return resp;
        }
        for(int i = 0; i < todasLinhas.size(); i++) {
            if(distanciaEuclidiana(primeiro,todasLinhas.get(i).primeiro) <= tamanhoDistancia && distanciaEuclidiana(segundo2,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontradasComeco(i,retas)) {
                resp = i;
                break;
            }else if(distanciaEuclidiana(primeiro,todasLinhas.get(i).ultimo) <= tamanhoDistancia && distanciaEuclidiana(segundo2,todasLinhas.get(i).primeiro) <= tamanhoDistancia && !retasEncontradasComeco(i,retas)) {
                resp = i;
                break;
            }
        }
        return resp;
    }

    /**
     * Metodo para verifica se as retas ja foram encontradas.
     * @param x posicao da reta a ser ferificada.
     * @param retas vetor de posicao de retas.
     * @return boolean true se existe a reta, false se nao.
     */
    private boolean retasEncontradasComeco(int x, int[] retas) {
        for(int i = 0; i < retas.length; i++) {
            if(x == retas[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo para verifica se as retas ja foram encontradas. Incluindo as retas das listas.
     * @param x posicao da reta a ser ferificada.
     * @param retas vetor de posicao de retas.
     * @param retasPrimeiro lista de retas ja encontradas, retas da primeira linha de 90 graus.
     * @param retasUltimo lista de retas ja encontradas, retas da ultimo linha de 90 graus.
     * @return boolean true se existe a reta, false se nao.
     */
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

    /**
     * Metodo para encontrar as 2 retas faltantes para forma a caixa.
     * @param primeiro Primeiro ponto, da primeira reta.
     * @param ultimo Ultimo ponto, da primeira reta.
     * @param primeiro1 Primeiro ponto, da segunda reta.
     * @param ultimo1 Ultimo ponto, da segunda reta.
     * @param retas vetor de retas ja encontradas.
     * @return boolean true para achar as retas faltantes(achou a caixa), false para nao achar as retas faltantes(nao achou a caixa).
     */
    private boolean acharRetasFinais(Point primeiro, Point ultimo, Point primeiro1, Point ultimo1, int[] retas) {
        boolean resp = false;
        int [] tmp = new int [2];
        tmp[0] = -1;
        tmp[1] = -1;
        ArrayList<Integer> retasPrimeiro = new ArrayList<>();
        ArrayList<Integer> retasUltimo = new ArrayList<>();

        for(int i = 0; i < todasLinhas.size(); i++) {

            if(distanciaEuclidiana(primeiro,todasLinhas.get(i).primeiro) <= tamanhoDistancia || distanciaEuclidiana(primeiro,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontrada(i,retas,retasPrimeiro,retasUltimo)) {
                retasPrimeiro.add(i);
            }
            if(distanciaEuclidiana(ultimo,todasLinhas.get(i).primeiro) <= tamanhoDistancia || distanciaEuclidiana(ultimo,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontrada(i,retas,retasPrimeiro,retasUltimo)) {
                retasPrimeiro.add(i);
            }

            if(distanciaEuclidiana(primeiro1,todasLinhas.get(i).primeiro) <= tamanhoDistancia || distanciaEuclidiana(primeiro1,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontrada(i,retas,retasPrimeiro,retasUltimo)) {
                retasUltimo.add(i);
            }
            if(distanciaEuclidiana(ultimo1,todasLinhas.get(i).primeiro) <= tamanhoDistancia || distanciaEuclidiana(ultimo1,todasLinhas.get(i).ultimo) <= tamanhoDistancia && !retasEncontrada(i,retas,retasPrimeiro,retasUltimo)) {
                retasUltimo.add(i);
            }

        }

        for(int i = 0; i < retasPrimeiro.size(); i++) {
            for(int j = 0; j < retasUltimo.size(); j++) {
                if(distanciaEuclidiana(todasLinhas.get(retasPrimeiro.get(i)).primeiro,todasLinhas.get(retasUltimo.get(j)).primeiro) <= tamanhoDistancia) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
                if(distanciaEuclidiana(todasLinhas.get(retasPrimeiro.get(i)).primeiro,todasLinhas.get(retasUltimo.get(j)).ultimo) <= tamanhoDistancia) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
                if(distanciaEuclidiana(todasLinhas.get(retasPrimeiro.get(i)).ultimo,todasLinhas.get(retasUltimo.get(j)).primeiro) <= tamanhoDistancia) {
                    retas[4] = retasPrimeiro.get(i);
                    retas[5] = retasUltimo.get(j);
                    resp = true;
                    break;
                }
                if(distanciaEuclidiana(todasLinhas.get(retasPrimeiro.get(i)).ultimo,todasLinhas.get(retasUltimo.get(j)).ultimo) <= tamanhoDistancia) {
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
    
    
/*===================================================================================================================*/
/*===================================================================================================================*/
/*===================================================================================================================*/

    //Comeco da Primeira Heuristica

    /**
     * Primeira combinacao, para comecar a primeira heuristica.
     * @param n tamanho da lista tipos de graus.
     * @param r tamanho do grupo.
     * @param x vetor auxiliar.
     * @param next proxima posicao que a combinacao irar comecar.
     * @param k verificador para ver se o grupo esta cheio.
     */
    private void combinacaoDeTodasRetas(int n, int r, int x [],int next,int k) {
        int i;
        if(k == r && flag != true) {
            int size = linhasParalelas.get(x[0]).linhas.size();
            int [] tmp = new int [size];
            combinacao1(size,3,tmp,0,0,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinacaoDeTodasRetas(n,r,x,i+1,k+1);
            }
        }
    }
    /**
     * Segunda combinacao, para comecar a primeira heuristica.
     * @param n tamanho da lista da primeiras linhas.
     * @param r tamanho do grupo.
     * @param x vetor auxiliar.
     * @param next proxima posicao que a combinacao irar comecar.
     * @param k verificador para ver se o grupo esta cheio.
     * @param vetorRetasParalelas e o vetor de posicoes encontrado pela ultima combinacao
     */
    private void combinacao1(int n, int r, int x [], int next, int k, int [] vetorRetasParalelas) {
        int i;
        if(k == r && flag != true) {
            int size = linhasParalelas.get(vetorRetasParalelas[1]).linhas.size();
            int [] tmp = new int [size];
            combinacao2(size,3,tmp,0,0,vetorRetasParalelas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinacao1(n,r,x,i+1,k+1,vetorRetasParalelas);
            }
        }
    }
    /**
     * Terceira combinacao, para comecar a primeira heuristica.
     * @param n tamanho da lista da segundas linhas.
     * @param r tamanho do grupo.
     * @param x vetor auxiliar.
     * @param next proxima posicao que a combinacao irar comecar.
     * @param k verificador para ver se o grupo esta cheio.
     * @param vetorRetasParalelas e o vetor de posicoes de graus das linhas.
     * @param primeirasRetas e o vetor de posicoes da primeiro tipo de grau das linhas.
     */
    private void combinacao2(int n , int r, int x [] , int next, int k, int [] vetorRetasParalelas, int [] primeirasRetas) {
        int i;
        if(k == r && flag != true) {
            int size = linhasParalelas.get(vetorRetasParalelas[2]).linhas.size();
            int [] tmp = new int [size];
            combinacao3(size,3,tmp,0,0,vetorRetasParalelas,primeirasRetas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinacao2(n,r,x,i+1,k+1,vetorRetasParalelas,primeirasRetas);
            }
        }
    }
    /**
     * Terceira combinacao, para comecar a primeira heuristica.
     * @param n tamanho da lista da segundas linhas.
     * @param r tamanho do grupo.
     * @param x vetor auxiliar.
     * @param next proxima posicao que a combinacao irar comecar.
     * @param k verificador para ver se o grupo esta cheio.
     * @param vetorRetasParalelas e o vetor de posicoes de graus das linhas.
     * @param primeirasRetas e o vetor de posicoes do primeiro tipo de grau das linhas.
     * @param segundasRetas e o vetor de posicoes do segunda tipo de grau das linhas.
     */
    private void combinacao3(int n, int r, int x [], int next, int k, int [] vetorRetasParalelas, int [] primeirasRetas, int [] segundasRetas) {
        int i;
        if(k == r && flag != true) {
            acharCaixaPrimeiraHeuristica(vetorRetasParalelas,primeirasRetas,segundasRetas,x);
        }else{
            for(i = next; i < n; i++) {
                x[k] = i;
                if(flag != true)
                    combinacao3(n,r,x,i+1,k+1,vetorRetasParalelas,primeirasRetas,segundasRetas);
            }
        }
    }

    /**
     * Metodo para achar as caixas, usando a primeira heuristica.
     * @param vetorRetasParalelas Vetor de graus encontrados.
     * @param primeirasRetas Vetor de posicoes das retas do primeiro grau.
     * @param segundasRetas Vetor de posicoes das retas do segundo grau.
     * @param terceirasRetas Vetor de posicoes das retas do terceiro grau.
     */
    private void acharCaixaPrimeiraHeuristica(int [] vetorRetasParalelas, int [] primeirasRetas, int [] segundasRetas, int [] terceirasRetas) {
        ArrayList<Point> pontos = new ArrayList<Point>();
        ArrayList<Integer> passoTodos = new ArrayList<Integer>();
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
        if(pontos.size() == 7 && verificarVetor(passoTodos)) { // Para testar, mudar esses valores.
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

            Caixas caixa = new Caixas();
            //Retirando as retas na lista de 90 graus
            for(int i = 2; i >= 0; i--) {
                caixa.linhas.add(linhasParalelas.get(vetorRetasParalelas[0]).linhas.get(primeirasRetas[i]));
                linhasParalelas.get(vetorRetasParalelas[0]).linhas.remove(primeirasRetas[i]);
            }
            for(int i = 2; i >= 0; i--) {
                caixa.linhas.add(linhasParalelas.get(vetorRetasParalelas[1]).linhas.get(segundasRetas[i]));
                linhasParalelas.get(vetorRetasParalelas[1]).linhas.remove(segundasRetas[i]);
            }
            //Retirando as retas na lista das retas restantes
            for(int i = 2; i>= 0; i--) {
                caixa.linhas.add(linhasParalelas.get(vetorRetasParalelas[2]).linhas.get(terceirasRetas[i]));
                linhasParalelas.get(vetorRetasParalelas[2]).linhas.remove(terceirasRetas[i]);
            }
            //Adicionando a caixa no vetor de caixas.
            todasCaixas.add(caixa);
        }
    }

    /**
     * Verifica se as vertices foram encontradas corretamentes.
     * @param passoTodos Vertices encontradas.
     * @return boolean true se as vertices foram certas, e false se nao e vertices corretas.
     */
    private boolean verificarVetor(ArrayList<Integer> passoTodos) {
        int contador = 0;
        for(int i = 0; i < passoTodos.size();i++) {

            if(passoTodos.get(i) == 1) {
                contador++;
            }
        }
        if(contador != 3) {
            return false;
        }
        for(int i = 0; i < passoTodos.size();i++) {

            if(passoTodos.get(i) == 2) {
                contador++;
            }
        }
        if(contador != 7) {
            return false;
        }
        return true;
    }

    /**
     * Metodo para verificar se a vertice ja foi encontrada.
     * @param ponto Ponto para verificar.
     * @param points Pontos ja encontrados.
     * @param tmp verificar se as verticies ja foram revidicadas ou nao. Se sim adiciona +1 na posicao da vertice.
     * @return boolean vertice ainda nao foi colocada volta true, se nao false.
     */
    private boolean acharPontos(Point ponto, ArrayList<Point> points, ArrayList<Integer> tmp) {
        boolean resp = false;
        double distancia = tamanhoDistancia;
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

    /**
     * Metodo para o calculo de distancia euclidiana.
     * @param um Ponto um.
     * @param dois Ponto dois.
     * @return distancia encontrada.
     */
    private double distanciaEuclidiana(Point um, Point dois) {

        double distancia = Math.sqrt(Math.pow(dois.x - um.x, 2) + Math.pow(dois.y - um.y, 2));
        return distancia;
    }

    /**
     * Metodo para separar as retas em 3 grupos de graus.
     * @param linhas Todas as linhas encontrada pela transformada de Hough.
     * @return ArrayList<LinhasParalelas> Vetor ja separando todas as retas pelos seus respectivos graus.
     */
    private ArrayList<LinhasParalelas> acharGrouposDeGraus(ArrayList<Linhas> linhas) {

        ArrayList<LinhasParalelas> todasLinhasParalelas = new ArrayList<LinhasParalelas>();
        LinhasParalelas linhasParalelas;
        int cont = 0;
        for(int i = 0; i < linhas.size(); i++){
            linhasParalelas = new LinhasParalelas();
            linhasParalelas.linhas.add(linhas.get(i));
            linhas.remove(i);
            i--;
            for(int j = i+1;  j < linhas.size();j++){
                if(linhasParalelas.linhas.get(0).tipoReta == linhas.get(j).tipoReta) {
                    linhasParalelas.linhas.add(linhas.get(j));
                    linhas.remove(j);
                    j--;
                }
            }
            for(int j = 0; j < linhasParalelas.linhas.size(); j++) {
                Linhas linha1 = linhasParalelas.linhas.get(j);
                for(int z = j+1; z < linhasParalelas.linhas.size();z++ ){
                    Linhas linha2 = linhasParalelas.linhas.get(z);
                    if (verificarRetasIguais(linha1, linha2)) {
                        linhasParalelas.linhas.remove(z);
                        z--;
                    }
                }
            }
            if(linhasParalelas.linhas.size() >= 3) {
                Collections.sort(linhasParalelas.linhas, new Comparator<Linhas>() {
                    public int compare(Linhas v1, Linhas v2) {
                        double tmp = v1.primeiro.x - v2.primeiro.x;
                        return (int)tmp;
                    }
                });
                todasLinhasParalelas.add(linhasParalelas);
            }
        }
        return todasLinhasParalelas;
    }

    /**
     * Metodo para verificar se as retas sao iguais.
     * @param linhas1 Linha um.
     * @param linhas2 Linha dois.
     * @return Voltar a resposta se sao iguais ou nao.
     */
    private boolean verificarRetasIguais(Linhas linhas1, Linhas linhas2) {
        double tmp1 = distanciaEuclidiana(linhas1.primeiro,linhas2.primeiro);
        double tmp2 = distanciaEuclidiana(linhas1.ultimo,linhas2.ultimo);
        double tmp3 = distanciaEuclidiana(linhas1.primeiro,linhas2.ultimo);
        double tmp4 = distanciaEuclidiana(linhas1.ultimo,linhas2.primeiro);
        if((tmp1 <= tamanhoDistancia && tmp2 <= tamanhoDistancia) || (tmp3 <= tamanhoDistancia  && tmp4 <= tamanhoDistancia)) {
            return true;
        }
        return false;
    }

    /**
     * Metodo para configuracao do dialog de preferencias de deteccao
     */
    public void doTheDialogThing(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.settings_dialog);
        dialog.setTitle(getResources().getString(R.string.user_settings));

        final TextView cannySoft = dialog.findViewById(R.id.actualCannyLow);
        final TextView cannyStrong = dialog.findViewById(R.id.actualCannyStrong);

        final SeekBar cannySoftSeekBar = dialog.findViewById(R.id.cannyLowBorders);
        final SeekBar cannyStrongSeekBar = dialog.findViewById(R.id.cannyStrongBorders);

        final RadioButton combination = dialog.findViewById(R.id.combinationHeuristic);
        final RadioButton noCombination = dialog.findViewById(R.id.noCombinationHeuristic);

        //pegando preferencias previamente armazenadas pelo usuario.
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoftPreviousPref = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrongPreviousPref = sharedPreferences.getInt("cannyStrong", 100);
        boolean combinationHeuristic = sharedPreferences.getBoolean("combinationHeuristic", false);

        //configurando os elementos do dialog com os valores iniciais.
        if(combinationHeuristic){
            combination.setChecked(true);
        }else {
            noCombination.setChecked(true);
        }

        //listeners para seekbar do canny
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
                //armazenando os valores alterados para as preferencias de usuario do aplicativo.
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt("cannySoft", cannySoftSeekBar.getProgress());
                sharedPreferencesEditor.putInt("cannyStrong", cannyStrongSeekBar.getProgress());
                sharedPreferencesEditor.putBoolean("combinationHeuristic", combination.isChecked());
                sharedPreferencesEditor.commit();
                Toast.makeText(getActivity(), "Suas configurações foram atualizadas!", Toast.LENGTH_SHORT).show();
                detectBoxes();
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