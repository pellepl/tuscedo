package com.pelleplutt.tuscedo.math;

public class Complex {
  double r, i;

  public Complex() {
  }

  public Complex(double r, double i) {
    this.r = r;
    this.i = i;
  }

  public void set(double r, double i) {
    this.r = r;
    this.i = i;
  }

  public Complex conjugate() {
    return new Complex(r, -i);
  }

  public Complex scale(double s) {
    return new Complex(r * s, i * s);
  }

  public Complex plus(Complex c) {
    return new Complex(r + c.r, i + c.i);
  }

  public Complex minus(Complex c) {
    return new Complex(r - c.r, i - c.i);
  }

  public Complex times(Complex c) {
    return new Complex(r * c.r - i * c.i, r * c.i + c.r * i);
  }

  public double length() {
    return Math.sqrt(r * r + i * i);
  }

  public double getReal() {
    return this.r;
  }
  
  public double getImaginary() {
    return this.i;
  }
} // class Complex
