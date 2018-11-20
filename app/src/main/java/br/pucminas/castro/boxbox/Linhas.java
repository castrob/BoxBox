package br.pucminas.castro.boxbox;

import org.opencv.core.Point;
import org.opencv.imgproc.LineSegmentDetector;

public class Linhas {
    public Point primeiro;
    public Point ultimo;
    public double a;
    public int tipoReta; //0 = angulo positivo, 1 = angulo negativo , 2 = indo pro infinito.

    public Linhas(Point primeiro, Point ultimo) {
        this.primeiro = primeiro;
        this.ultimo = ultimo;
        //System.out.println("Primeiro ponto: = (" + this.primeiro.x + "," + this.primeiro.y + ")" + " ultimo ponto: = (" + this.ultimo.x + "," + this.ultimo.y + ")");
        this.a = calcularA();
        //System.out.println("A = " + this.a);
        this.tipoReta = calcularTipoReta();
    }

    private double calcularA() {
        double deltaY = (-1 * ultimo.y) - ( -1 * primeiro.y);
        double deltaX = (ultimo.x) - (primeiro.x);
        return deltaY/deltaX;
    }

    private int calcularTipoReta() {
        int resp;
        if(this.a >= 4 || this.a <= -4) {
            resp = 2;
        }else if(this.a >= 0 && this.a < 4) {
            resp = 0;
        }else{
            resp = 1;
        }
        return resp;
    }
}
