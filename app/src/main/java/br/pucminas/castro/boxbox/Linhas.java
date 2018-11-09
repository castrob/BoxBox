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
        if(this.a >= 6 || this.a <= -6) {
            resp = 90;
        }else if(this.a >= 1.79) {
            resp = 6180;
        }else if(this.a >= 0.85) {
            resp = 4160;
        }else if(this.a >= 0.37) {
            resp = 2140;
        }else if(this.a >= 0) {
            resp = 19;
        }else if(this.a <= -1.79) {
            resp = 6180;
        }else if(this.a <= -0.85) {
            resp = 4160;
        }else if(this.a <= -0.37) {
            resp = 2140;
        }else{
            resp = 19;
        }
        return resp;
    }
}
