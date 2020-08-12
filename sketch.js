let fluid;

function setup() {
  createCanvas(N * SCALE, N * SCALE);
  frameRate(60);
  fluid = new Fluid(0.2, 0, 0.0000001);
}
function draw() {
  background(0);
  fluid.step();
  fluid.renderD();
}
function mouseDragged() {
  fluid.addDensity(mouseX/SCALE, mouseY/SCALE, 100);
  print("mouse dragged");
}