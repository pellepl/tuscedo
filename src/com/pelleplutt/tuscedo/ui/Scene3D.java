package com.pelleplutt.tuscedo.ui;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glLoadMatrixf;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

// https://www.lighthouse3d.com/tutorials/opengl_framebuffer_objects/
public class Scene3D {
  volatile static boolean destroyed = false;
  GLFWErrorCallback errorCallback;
  GLFWKeyCallback keyCallback;
  GLFWFramebufferSizeCallback fbCallback;

  long window;
  int width = 400;
  int height = 300;

  // JOML matrices
  Matrix4f projMatrix = new Matrix4f();
  Matrix4f viewMatrix = new Matrix4f();
  Matrix4f modelMatrix = new Matrix4f();
  Matrix4f modelViewMatrix = new Matrix4f();

  // FloatBuffer for transferring matrices to OpenGL
  FloatBuffer fb = BufferUtils.createFloatBuffer(16);

  void glInit() {
    glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");

    // Configure our window
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

    window = glfwCreateWindow(width, height, "offscreen", NULL, NULL);
    if (window == NULL)
      throw new RuntimeException("Failed to create the GLFW window");

    glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
      @Override
      public void invoke(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
          destroyed = true;
          glfwSetWindowShouldClose(window, true);
      }
    });
    glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
      @Override
      public void invoke(long window, int w, int h) {
        if (w > 0 && h > 0) {
          width = w;
          height = h;
        }
      }
    });

    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);

    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
    if (standalone) glfwShowWindow(window);
  }

  void renderCube() {
    glBegin(GL_QUADS);
    glColor3f(0.0f, 0.0f, 0.2f);
    glVertex3f(0.5f, -0.5f, -0.5f);
    glVertex3f(-0.5f, -0.5f, -0.5f);
    glVertex3f(-0.5f, 0.5f, -0.5f);
    glVertex3f(0.5f, 0.5f, -0.5f);
    glColor3f(0.0f, 0.0f, 1.0f);
    glVertex3f(0.5f, -0.5f, 0.5f);
    glVertex3f(0.5f, 0.5f, 0.5f);
    glVertex3f(-0.5f, 0.5f, 0.5f);
    glVertex3f(-0.5f, -0.5f, 0.5f);
    glColor3f(1.0f, 0.0f, 0.0f);
    glVertex3f(0.5f, -0.5f, -0.5f);
    glVertex3f(0.5f, 0.5f, -0.5f);
    glVertex3f(0.5f, 0.5f, 0.5f);
    glVertex3f(0.5f, -0.5f, 0.5f);
    glColor3f(0.2f, 0.0f, 0.0f);
    glVertex3f(-0.5f, -0.5f, 0.5f);
    glVertex3f(-0.5f, 0.5f, 0.5f);
    glVertex3f(-0.5f, 0.5f, -0.5f);
    glVertex3f(-0.5f, -0.5f, -0.5f);
    glColor3f(0.0f, 1.0f, 0.0f);
    glVertex3f(0.5f, 0.5f, 0.5f);
    glVertex3f(0.5f, 0.5f, -0.5f);
    glVertex3f(-0.5f, 0.5f, -0.5f);
    glVertex3f(-0.5f, 0.5f, 0.5f);
    glColor3f(0.0f, 0.2f, 0.0f);
    glVertex3f(0.5f, -0.5f, -0.5f);
    glVertex3f(0.5f, -0.5f, 0.5f);
    glVertex3f(-0.5f, -0.5f, 0.5f);
    glVertex3f(-0.5f, -0.5f, -0.5f);
    glEnd();
  }

  long firstTime;
  BufferedImage image;
  static boolean standalone = false;

  public void init() {
    glInit();
    GL.createCapabilities();

    // Set the clear color
    glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
    // Enable depth testing
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);

    // Remember the current time.
    firstTime = System.nanoTime();
  }

  public void render() {
    // Build time difference between this and first time.
    long thisTime = System.nanoTime();
    float diff = (thisTime - firstTime) / 1E9f;
    // Compute some rotation angle.
    float angle = diff;

    // Make the viewport always fill the whole window.
    glViewport(0, 0, width, height);

    // Build the projection matrix. Watch out here for integer division
    // when computing the aspect ratio!
    projMatrix.setPerspective((float) Math.toRadians(40), (float) width / height, 0.01f, 100.0f);
    glMatrixMode(GL_PROJECTION);
    glLoadMatrixf(projMatrix.get(fb));

    // Set lookat view matrix
    viewMatrix.setLookAt(0.0f, 4.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    glMatrixMode(GL_MODELVIEW);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Render some grid of cubes at different x and z positions
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        modelMatrix.translation(x * 2.0f, 0, z * 2.0f).rotateY(angle * (float) Math.toRadians(90));
        glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
        renderCube();
      }
    }
    glfwSwapBuffers(window);
    glfwPollEvents();
    
    // dump renderbuffer to image
    image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    ByteBuffer nativeBuffer = BufferUtils.createByteBuffer(width*height*3);
    GL11.glReadPixels(0, 0, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, nativeBuffer);
    byte[] imgData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
    nativeBuffer.get(imgData);
  }

  public void destroy() {
    glfwDestroyWindow(window);
    keyCallback.free();
    glfwTerminate();
    errorCallback.free();
  }
  
  public BufferedImage getImage() {
    return image;
  }
  
  int createShader(int type, String src) {
    int shader = glCreateShader(type);
    glShaderSource(shader, src);
    glCompileShader(shader);
    int status = glGetShaderi(shader, GL_COMPILE_STATUS);
    if (status != GL_TRUE) {
      throw new RuntimeException(glGetShaderInfoLog(shader));
    }
    glGetError();

    return shader;
  }
  
  int createProgram(int... shaders) {
    int prog = glCreateProgram();
    for (int shader : shaders) {
      glAttachShader(prog, shader);
    }
    glLinkProgram(prog);
    int status = glGetShaderi(prog, GL_LINK_STATUS);
    if (status != GL_TRUE) {
      throw new RuntimeException(glGetShaderInfoLog(prog));
    }
    for (int shader : shaders) {
      glDetachShader(prog, shader);
      glDeleteShader(shader);
    }
    glGetError();
    
    return prog;
  }
  
  // image -> bytebuffer (8-bit RED, 8-bit GREEN, 8-bit BLUE, 8-bit wotevah)
  ByteBuffer createTextureBuffer(BufferedImage bufferedImage) {
    ByteBuffer imageBuffer;
    WritableRaster raster;
    BufferedImage texImage;

    ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
            .getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
            true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
            bufferedImage.getWidth(), bufferedImage.getHeight(), 4, null);
    texImage = new BufferedImage(glAlphaColorModel, raster, true, null);

    // copy the source image into the produced image
    Graphics g = texImage.getGraphics();
    g.setColor(new Color(0f, 0f, 0f, 0f));
    g.fillRect(0, 0, texImage.getWidth(), texImage.getHeight());
    g.drawImage(bufferedImage, 0, 0, null);

    // build a byte buffer from the temporary image
    // that is used by OpenGL to produce a texture.
    byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer())
            .getData();

    imageBuffer = ByteBuffer.allocateDirect(data.length);
    imageBuffer.order(ByteOrder.nativeOrder());
    imageBuffer.put(data, 0, data.length);
    imageBuffer.flip();

    return imageBuffer;
  }

  public static void main(String[] args) {
    standalone = true;
    Scene3D s = new Scene3D();
    s.init();
    
    while (!destroyed && !glfwWindowShouldClose(s.window)) {
      s.render();
    }
    s.destroy();
  }
}
