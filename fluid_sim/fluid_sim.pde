import controlP5.*;

ControlP5 cp5;
float diffusion = 0.0000001;// min=0.0000001, max=0.00015
float viscosity = 0.0000001;// min=0.0000001, max=0.0006

final int N = 128;
final int iter = 16;
final int SCALE = 6;
float t = 0;

Fluid fluid;

void settings() {
  size(N*SCALE, N*SCALE);
}

void setup() {
  cp5 = new ControlP5(this);
  fluid = new Fluid(0.2, diffusion, viscosity);
  cp5.addSlider("diffusion")
    .setPosition(600, 50)
    .setRange(0.0000001, 0.00015);
  cp5.addSlider("viscosity")
    .setPosition(600, 70)
    .setRange(0.0000001, 0.0006);
}

void mouseDragged() {
  fluid.addDensity(mouseX/SCALE, mouseY/SCALE, 100);
  float amtX = 0;
  float amtY = 0;
  if (mouseY - pmouseY > 0) {
    amtY = mouseY - (pmouseY + 1.05);
  } else if (mouseY - pmouseY < 0) {
    amtY = mouseY - (pmouseY - 1.05);
  }
  if (mouseX - pmouseX > 0) {
    amtX = mouseX - (pmouseX + 1.05);
  } else if (mouseX - pmouseX < 0) {
    amtX = mouseX - (pmouseX - 1.05);
  }
  amtX = constrain(amtX, -0.2, 0.2);
  amtY = constrain(amtX, -0.2, 0.2);
  fluid.addVelocity(mouseX/SCALE, mouseY/SCALE, amtX, amtY);
}

void mousePressed() {
  fluid.addDensity(mouseX/SCALE, mouseY/SCALE, 1000);
  float amtX = random(0.0, 1.0);
  float amtY = random(0.0, 1.0);
  fluid.addVelocity(mouseX/SCALE, mouseY/SCALE, amtX, amtY);
}

void draw() {
  background(0);
  if (mousePressed && (mouseButton == LEFT) && (mouseX - pmouseX == 0) && (mouseY - pmouseY == 0)) {
    fluid.addDensity(mouseX/SCALE, mouseY/SCALE, 400);
    float amtX = random(-0.3, 0.3);
    float amtY = random(-0.3, 0.0);
    fluid.addVelocity(mouseX/SCALE, mouseY/SCALE, amtX, amtY);
  }
  
  fluid.diff = diffusion;
  fluid.visc = viscosity;
  
  
  fluid.step();
  fluid.renderD();
  fluid.fadeD();
}
