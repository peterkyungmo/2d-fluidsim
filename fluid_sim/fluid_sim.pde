final int N = 128;
final int iter = 16;
final int SCALE = 4;
float t = 0;

Fluid fluid;

void settings() {
  size(N*SCALE, N*SCALE);
}

void setup() {
  fluid = new Fluid(0.2, 0, 0.0000001);
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

void draw() {
  background(0);
  fluid.step();
  fluid.renderD();
  fluid.fadeD();
}
