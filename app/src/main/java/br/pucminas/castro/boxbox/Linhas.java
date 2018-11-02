package br.pucminas.castro.boxbox;

import org.opencv.core.Point;
import org.opencv.imgproc.LineSegmentDetector;

public class Linhas {
    public Point primeiro;
    public Point ultimo;
    public double a;
    public double b;

    public Linhas(Point primeiro, Point ultimo) {
        this.primeiro = primeiro;
        this.ultimo = ultimo;
        this.a = calcularA();
        System.out.println("A = " + this.a);
        this.b = calcularB();
    }

    private double calcularA() {
        double deltaY = (-1 * ultimo.y) - ( -1 * primeiro.y);
        double deltaX = (ultimo.x) - (primeiro.x);
        return deltaY/deltaX;
    }

    private double calcularB() {
        return primeiro.y - (a * primeiro.x);
    }
}
