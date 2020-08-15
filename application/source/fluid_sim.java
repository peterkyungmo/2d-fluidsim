import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class fluid_sim extends PApplet {



ControlP5 cp5;
float diffusion = 0.000001f;// min=0.0000001, max=0.00015
float viscosity = 0.0000001f;// min=0.0000001, max=0.0006

final int N = 128;
final int iter = 16;
final int SCALE = 6;
float t = 0;
int radius = 1;

Fluid fluid;

public void settings() {
  size(N*SCALE, N*SCALE);
}

public void setup() {
  fluid = new Fluid(0.2f, diffusion, viscosity);
  addGUI();
}

public void addGUI() {
  cp5 = new ControlP5(this);
  cp5.addSlider("diffusion")
    .setPosition(600, 50)
    .setRange(0.0000001f, 0.00015f);
  cp5.addSlider("viscosity")
    .setPosition(600, 70)
    .setRange(0.0000001f, 0.0006f);
  cp5.addSlider("radius")
    .setPosition(600, 90)
    .setRange(1, 20);
}

public void mouseDragged() {
  int originX = mouseX - radius;
  int originY = mouseY - radius;
  for (int i = originX; i <= mouseX + radius/SCALE; i++) {
    for (int j = originY; j <= mouseY + radius/SCALE; j++) {
      fluid.addDensity(i/SCALE, j/SCALE, 100);
    }
  }
  float amtX = 0;
  float amtY = 0;
  if (mouseY - pmouseY > 0) {
    amtY = mouseY - (pmouseY + 1.05f);
  } else if (mouseY - pmouseY < 0) {
    amtY = mouseY - (pmouseY - 1.05f);
  }
  if (mouseX - pmouseX > 0) {
    amtX = mouseX - (pmouseX + 1.05f);
  } else if (mouseX - pmouseX < 0) {
    amtX = mouseX - (pmouseX - 1.05f);
  }
  amtX = constrain(amtX, -0.2f, 0.2f);
  amtY = constrain(amtX, -0.2f, 0.2f);
  fluid.addVelocity(mouseX/SCALE, mouseY/SCALE, amtX, amtY);
}

public void draw() {
  background(0);
  if (mousePressed && (mouseButton == LEFT) && (mouseX - pmouseX == 0) && (mouseY - pmouseY == 0)) {
    //addDensity based on radius of pixel
    int originX = mouseX - radius;
    int originY = mouseY - radius;
    
    for (int i = originX; i <= mouseX + radius/SCALE; i++) {
      for (int j = originY; j <= mouseY + radius/SCALE; j++) {
        fluid.addDensity(i/SCALE, j/SCALE, 400);
      }
    }
    float amtX = random(-0.3f, 0.3f);
    float amtY = random(-0.3f, 0.0f);
    fluid.addVelocity(mouseX/SCALE, mouseY/SCALE, amtX, amtY);
  }
  
  fluid.diff = diffusion;
  fluid.visc = viscosity;
  
  
  fluid.step();
  fluid.render();
}
public int IX(int x, int y) {
  x = constrain(x, 0, N-1);
  y = constrain(y, 0, N-1);
  return x + (y * N);
}

class Fluid {
  int size;
  float dt;
  float diff;
  float visc;

  float[] s;
  float[] density;

  float[] Vx;
  float[] Vy;

  float[] Vx0;
  float[] Vy0;

  Fluid(float dt, float diffusion, float viscosity) {
    this.size = N;
    this.visc = viscosity;
    this.diff = diffusion;
    this.dt = dt;
    this.Vx = new float[N*N];
    this.Vy = new float[N*N];
    this.Vx0 = new float[N*N];
    this.Vy0 = new float[N*N];
    this.s = new float[N*N];
    this.density = new float[N*N];
  }

  public void step() {
    float visc     = this.visc;
    float diff     = this.diff;
    float dt       = this.dt;
    float[] Vx      = this.Vx;
    float[] Vy      = this.Vy;
    float[] Vx0     = this.Vx0;
    float[] Vy0     = this.Vy0;
    float[] s       = this.s;
    float[] density = this.density;

    diffuse(1, Vx0, Vx, visc, dt);
    diffuse(2, Vy0, Vy, visc, dt);
    project(Vx0, Vy0, Vx, Vy);
    advect(1, Vx, Vx0, Vx0, Vy0, dt);
    advect(2, Vy, Vy0, Vx0, Vy0, dt);
    project(Vx, Vy, Vx0, Vy0);
    diffuse(0, s, density, diff, dt);
    advect(0, density, s, Vx, Vy, dt);
  }

  public void addDensity(int x, int y, float amt) {
    int i = IX(x, y);
    this.density[i] += amt;
  }

  public void addVelocity(int x, int y, float amtX, float amtY) {
    int i = IX(x, y);
    this.Vx[i] += amtX;
    this.Vy[i] += amtY;
  }

  public void render() {
    colorMode(HSB, 255);
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        float x = i * SCALE;
        float y = j * SCALE;
        float d = this.density[IX(i, j)];
        fill(255, d);
        noStroke();
        square(x, y, SCALE);
      }
    }
    for (int i = 0; i < this.density.length; i++) {
      float d = density[i];
      density[i] = constrain(d-0.3f, 0, 255);
    }
  }
}

public void diffuse (int b, float[] x, float[] x0, float diff, float dt) {
  float a = dt * diff * (N - 2) * (N - 2);
  gauss_seidel(b, x, x0, a, 1 + 4 * a);
}

public void gauss_seidel(int b, float[] x, float[] x0, float a, float c) {
  float cRecip = 1.0f / c;
  for (int k = 0; k < iter; k++) {
    for (int j = 1; j < N - 1; j++) {
      for (int i = 1; i < N - 1; i++) {
        x[IX(i, j)] =
          (x0[IX(i, j)]
          + a*(    x[IX(i+1, j)]
          +x[IX(i-1, j)]
          +x[IX(i, j+1)]
          +x[IX(i, j-1)]
          )) * cRecip;
      }
    }

    set_bnd(b, x);
  }
}
public void project(float[] velocX, float[] velocY, float[] p, float[] div) {
  for (int j = 1; j < N - 1; j++) {
    for (int i = 1; i < N - 1; i++) {
      div[IX(i, j)] = -0.5f*(
        velocX[IX(i+1, j)]
        -velocX[IX(i-1, j)]
        +velocY[IX(i, j+1)]
        -velocY[IX(i, j-1)]
        )/N;
      p[IX(i, j)] = 0;
    }
  }

  set_bnd(0, div); 
  set_bnd(0, p);
  gauss_seidel(0, p, div, 1, 4);

  for (int j = 1; j < N - 1; j++) {
    for (int i = 1; i < N - 1; i++) {
      velocX[IX(i, j)] -= 0.5f * (  p[IX(i+1, j)]
        -p[IX(i-1, j)]) * N;
      velocY[IX(i, j)] -= 0.5f * (  p[IX(i, j+1)]
        -p[IX(i, j-1)]) * N;
    }
  }
  set_bnd(1, velocX);
  set_bnd(2, velocY);
}


public void advect(int b, float[] d, float[] d0, float[] velocX, float[] velocY, float dt) {
  float i0, i1, j0, j1;

  float dtx = dt * (N - 2);
  float dty = dt * (N - 2);

  float s0, s1, t0, t1;
  float tmp1, tmp2, x, y;

  float Nfloat = N;
  float ifloat, jfloat;
  int i, j;

  for (j = 1, jfloat = 1; j < N - 1; j++, jfloat++) { 
    for (i = 1, ifloat = 1; i < N - 1; i++, ifloat++) {
      tmp1 = dtx * velocX[IX(i, j)];
      tmp2 = dty * velocY[IX(i, j)];
      x    = ifloat - tmp1; 
      y    = jfloat - tmp2;

      if (x < 0.5f) x = 0.5f; 
      if (x > Nfloat + 0.5f) x = Nfloat + 0.5f; 
      i0 = floor(x); 
      i1 = i0 + 1.0f;
      if (y < 0.5f) y = 0.5f; 
      if (y > Nfloat + 0.5f) y = Nfloat + 0.5f; 
      j0 = floor(y);
      j1 = j0 + 1.0f; 

      s1 = x - i0; 
      s0 = 1.0f - s1; 
      t1 = y - j0; 
      t0 = 1.0f - t1;

      int i0i = PApplet.parseInt(i0);
      int i1i = PApplet.parseInt(i1);
      int j0i = PApplet.parseInt(j0);
      int j1i = PApplet.parseInt(j1);

      d[IX(i, j)] = 
        s0 * (t0 * d0[IX(i0i, j0i)] + t1 * d0[IX(i0i, j1i)]) +
        s1 * (t0 * d0[IX(i1i, j0i)] + t1 * d0[IX(i1i, j1i)]);
    }
  }

  set_bnd(b, d);
}



public void set_bnd(int b, float[] x) {
  for (int i = 1; i < N - 1; i++) {
    x[IX(i, 0  )] = b == 2 ? -x[IX(i, 1  )] : x[IX(i, 1 )];
    x[IX(i, N-1)] = b == 2 ? -x[IX(i, N-2)] : x[IX(i, N-2)];
  }
  for (int j = 1; j < N - 1; j++) {
    x[IX(0, j)] = b == 1 ? -x[IX(1, j)] : x[IX(1, j)];
    x[IX(N-1, j)] = b == 1 ? -x[IX(N-2, j)] : x[IX(N-2, j)];
  }

  x[IX(0, 0)] = 0.5f * (x[IX(1, 0)] + x[IX(0, 1)]);
  x[IX(0, N-1)] = 0.5f * (x[IX(1, N-1)] + x[IX(0, N-2)]);
  x[IX(N-1, 0)] = 0.5f * (x[IX(N-2, 0)] + x[IX(N-1, 1)]);
  x[IX(N-1, N-1)] = 0.5f * (x[IX(N-2, N-1)] + x[IX(N-1, N-2)]);
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "fluid_sim" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
