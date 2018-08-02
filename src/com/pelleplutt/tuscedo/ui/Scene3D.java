package com.pelleplutt.tuscedo.ui;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.lang.Math;
import java.nio.*;
import java.util.*;
import java.util.List;

import org.joml.*;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

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

  // floatBuffer for transferring matrices to OpenGL
  FloatBuffer fb = BufferUtils.createFloatBuffer(16);

  void initGLFW() {
    glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");

    // configure our window
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

    // set callbacks
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

    // misc stuff
    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
    if (standalone) glfwShowWindow(window);
  }
  
  long firstTime;
  BufferedImage image;
  static boolean standalone = false;

  public void init() {
    initGLFW();
    initGL();
    
    // Set the clear color
    glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
    // Enable depth testing
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);

    // Remember the current time.
    firstTime = System.nanoTime();
  }
  
  int progGL;
  int mLocModelGL;
  int mLocVPGL;
  int vLocColorGL;
  int vLocLightPosGL;
  int vLocPlayerViewGL;
  int vLocPlayerPosGL;
  int vao_sphereGL, vbo_sphereGL;
  int vbo_sphere_arr_ixGL;
  
  int numSphereVertices;
  int numSphereNormals;
  int numSphereIndices;


  void initGL() {
    GL.createCapabilities();
    System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
    createProgramGL();
  }
  
  void createProgramGL() {
    int vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 position; \n"
        +"in vec3 normal; \n"
        +""
        +"uniform vec3 scolor; \n"
        +"uniform mat4 model; \n"
        +"uniform mat4 viewproj; \n"
        +"uniform vec3 vLightPos = vec3(0.0, 0.0, 0.0); \n"
        +"uniform vec3 vPlayerView; \n"
        +"uniform vec3 vPlayerPos; \n"
        +""
        +"out vec3 vertexColor; \n"
        +"out vec3 vNormal; \n"
        +"out vec3 vONormal; \n"
        +"out vec3 vLight; \n"
        +"out vec3 vView; \n"
        +"out vec3 vPlayerLight; \n"
        +""
        +"void main() { \n"
        +"  vertexColor = scolor; \n"
        +""
        +"  vec4 P = model * vec4(position, 1.0); \n"
        +""
        +"  vNormal = mat3(model) * normal; \n"
        +"  vONormal = normal; \n"
        +"  vLight = vLightPos - P.xyz; \n"
        +"  vView = vPlayerPos - P.xyz;//vPlayerView; // -P; \n"
        +"  vPlayerLight = vLight - vPlayerPos; \n"
        +""
        +"  gl_Position = viewproj * P; \n"
        +"} \n"
        );
    int fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 vertexColor; \n" 
        +"in vec3 vNormal; \n"
        +"out vec4 fragColor; \n" 
        +""
        +"uniform vec3 veyepos; \n" 
        +""
        +"void main() {\n"
        +"  float ambient = 0.15; \n"
        +"  vec3 vLightPos = vec3(400, 400, 400); \n"
        +"  vec3 vLight = normalize(veyepos - vLightPos); \n"
        +"  vec3 H = vLight; //normalize(vLightPos + veyepos); \n" 
        +"  //vec3 H = normalize(veyepos); \n"
        +"  float diffuse = max(0, dot(vLight, vNormal)); \n"
        +"  float specular = pow(max(0, dot(H, vNormal)), 42.0); \n" 
        +"  fragColor = vec4(vertexColor, 1.0) * min(1.0, ambient+diffuse) + vec4(1,1,1,1) * specular; \n" 
        +"} \n"
        );
    progGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocModelGL = glGetUniformLocation(progGL, "model");
    mLocVPGL = glGetUniformLocation(progGL, "viewproj");
    vLocColorGL = glGetUniformLocation(progGL, "scolor");
    vLocLightPosGL = glGetUniformLocation(progGL, "vLightPos");
    vLocPlayerViewGL = glGetUniformLocation(progGL, "vPlayerView");
    vLocPlayerPosGL = glGetUniformLocation(progGL, "vPlayerPos");
    
    // sphere data
    RenderSphere rs = new RenderSphere(1f, 8);
    rs.build();
    numSphereVertices = rs.vertices.size();
    numSphereNormals = numSphereVertices;
    numSphereIndices = rs.indices.size();
    
    FloatBuffer verticesNormals = BufferUtils.createFloatBuffer((numSphereVertices + numSphereNormals) * 3);
    for (int i = 0; i < numSphereVertices; i++) {
      Vector3f v = rs.vertices.get(i);
      Vector3f n = rs.normals.get(i);
      verticesNormals.put(v.x).put(v.y).put(v.z);
      verticesNormals.put(n.x).put(n.y).put(n.z);
    }
    verticesNormals.flip();
    
    ShortBuffer indices = BufferUtils.createShortBuffer(numSphereIndices);
    for (int i = 0; i < numSphereIndices; i++) {
      int index = rs.indices.get(i);
      indices.put((short)index);
    }
    indices.flip();
    
    vao_sphereGL = glGenVertexArrays();
    glBindVertexArray(vao_sphereGL);
    vbo_sphereGL = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbo_sphereGL);
    glBufferData(GL_ARRAY_BUFFER, verticesNormals, GL_STATIC_DRAW);

    vbo_sphere_arr_ixGL = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_sphere_arr_ixGL);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
    
    // setup shader data inputs
    glUseProgram(progGL);

    glBindVertexArray(vao_sphereGL);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_sphereGL);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_sphere_arr_ixGL);
    
    int attrPart_pos = glGetAttribLocation(progGL, "position");
    glEnableVertexAttribArray(attrPart_pos);
    glVertexAttribPointer(attrPart_pos, 3, GL_FLOAT, false, 6 * 4, 0);

    int attrPart_norm = glGetAttribLocation(progGL, "normal");
    glEnableVertexAttribArray(attrPart_norm);
    glVertexAttribPointer(attrPart_norm, 3, GL_FLOAT, false, 6 * 4, 3 * 4);

    
  }

  void dumpToSwingImage() {
    // dump renderbuffer to image
    image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    ByteBuffer nativeBuffer = BufferUtils.createByteBuffer(width*height*3);
    GL11.glReadPixels(0, 0, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, nativeBuffer);
    byte[] imgData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
    nativeBuffer.get(imgData);
  }

  
  final Vector3f playerPos = new Vector3f();
  final Matrix4f viewProj = new Matrix4f();
  final FloatBuffer fbViewProj = BufferUtils.createFloatBuffer(16);
  final Matrix4f mRot = new Matrix4f();
  final Quaternionf qdir = new Quaternionf();
  final Quaternionf qdirinv = new Quaternionf();
  final Matrix4f mModel = new Matrix4f();
  final FloatBuffer fbModel = BufferUtils.createFloatBuffer(16);

  static final float ZNEAR = 0.1f;
  static final float ZFAR = 10000f;
  
  public void render() {
    long thisTime = System.nanoTime();
    float diffMs = (thisTime - firstTime) / 1E9f;

    // calc global viewing matrices
    commonCameraUpdate(0, 0, 0);
    viewProj.setPerspective((float)Math.PI/4f, (float)width/(float)height, ZNEAR, ZFAR);
    playerPos.set(0f, 0f, 0f); // TODO
    playerPos.negate();
    qdir.invert(qdirinv); 
    qdirinv.get(mRot);
    viewProj.mul(mRot).translate(playerPos);
    viewProj.get(fbViewProj);
    
    // setup GL view
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, width, height);
    glClearColor(.0f, .0f, .0f, 1f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL_DEPTH_TEST);

    // TODO
    glUseProgram(progGL);
    glBindVertexArray(vao_sphereGL);
    glUniformMatrix4fv(mLocVPGL, false, fbViewProj);
    glUniform3f(vLocPlayerPosGL, -playerPos.x, -playerPos.y, -playerPos.z);
    glUniform3f(vLocPlayerViewGL, vdirz.x, vdirz.y, vdirz.z);
    glUniform3f(vLocLightPosGL, -1000f, -1000f, 0);
    glUniform3f(vLocColorGL, 1f,0f,1f);

    mModel.identity();
    mModel.translate(
        (float)(5f*Math.sin(diffMs*1.1)),
        (float)(5f*Math.sin(diffMs*1.3)),
        (float)(-20f + 5f*Math.sin(diffMs*1.5)));
    mModel.get(fbModel);
    glUniformMatrix4fv(mLocModelGL, false, fbModel);
    glDrawElements(GL_TRIANGLES, numSphereIndices, GL_UNSIGNED_SHORT, 0);
    
    int err = glGetError();
    if (err != 0) System.out.println("GLERROR:" + Integer.toHexString(err));

    // coda

    glfwSwapBuffers(window);
    glfwPollEvents();
    
    

    dumpToSwingImage();
  }

  public void destroy() {
    glDeleteVertexArrays(vao_sphereGL);
    glDeleteBuffers(vbo_sphereGL);
    glDeleteBuffers(vbo_sphere_arr_ixGL);

    glfwDestroyWindow(window);
    glDeleteProgram(progGL);
    keyCallback.free();
    glfwTerminate();
    errorCallback.free();
  }
  
  public BufferedImage getImage() {
    return image;
  }
  
  final AxisAngle4f aayaw = new AxisAngle4f();
  final AxisAngle4f aapitch = new AxisAngle4f();
  final AxisAngle4f aaroll = new AxisAngle4f();
  final Quaternionf qyaw = new Quaternionf();
  final Quaternionf qpitch = new Quaternionf();
  final Quaternionf qroll = new Quaternionf();
  final Matrix3f qrotm = new Matrix3f();
  final Vector3f vdirx = new Vector3f(1,0,0);
  final Vector3f vdiry = new Vector3f(0,1,0);
  final Vector3f vdirz = new Vector3f(0,0,1);

  public void commonCameraUpdate(float dx, float dy, float droll)  {
    // get delta yaw and pitch quaternions
    aayaw.set(dx*0.001f, 0,1,0);
    aapitch.set(dy*0.001f, 1,0,0);
    aaroll.set(droll*0.001f, 0,0,1);
    qyaw.set(aayaw);
    qpitch.set(aapitch);
    qroll.set(aaroll);
    
    // apply to current direction
    qdir.mul(qroll).mul(qpitch).mul(qyaw);
    qdir.normalize();

    // get base vectors of rotation
    qdir.get(qrotm);
    qrotm.getColumn(0, vdirx);
    qrotm.getColumn(1, vdiry);
    qrotm.getColumn(2, vdirz);
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
  
  //
  // GL helpers
  //
  
  int createShader(int type, String src) {
    System.out.println("create shader " + type);
    int shader = glCreateShader(type);
    glShaderSource(shader, src);
    glCompileShader(shader);
    int status = glGetShaderi(shader, GL_COMPILE_STATUS);
    if (status != GL_TRUE) {
      int err = glGetError();
      throw new RuntimeException("glerr:" + err + "\n" + glGetShaderInfoLog(shader));
    }
    glGetError();

    return shader;
  }
  
  int createProgram(int... shaders) {
    System.out.println("create program");
    int prog = glCreateProgram();
    System.out.println("attaching " + shaders.length +  " shaders");
    for (int shader : shaders) {
      glAttachShader(prog, shader);
    }
    System.out.println("linking program");
    glLinkProgram(prog);
    int status = glGetProgrami(prog, GL_LINK_STATUS);
    if (status != GL_TRUE) {
      int err = glGetError();
      throw new RuntimeException("glerr:" + err + "\n" + glGetProgramInfoLog(prog));
    }
    System.out.println("deleting shader info");
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
  

  
  // TODO remove this
  class RenderSphere {
    float r;
    int div;
    public List<Vector3f> vertices = new ArrayList<Vector3f>();
    public List<Vector3f> normals = new ArrayList<Vector3f>();
    public List<Integer> indices= new ArrayList<Integer>();

    public RenderSphere(float radius, int div) {
      r = radius;
      this.div = div;
    }
    
    public void build() {
      vertices.add(new Vector3f(0,r,0));
      normals.add(new Vector3f(0,1,0));
      for (int aa = 1; aa < div/2; aa++) {
        float y,rr,x,z;
        float a = (float)Math.PI * 2f * (float) aa / div;
        for (int bb = 0; bb < div; bb++) {
          float b = (float)Math.PI * 2f * (float) bb / div;
          y = (float)Math.cos(a);
          rr = (float)Math.sin(a);
          x = rr*(float)Math.cos(b);
          z = rr*(float)Math.sin(b);
          // coords
          vertices.add(new Vector3f(r*x, r*y, r*z));
          // normals
          normals.add(new Vector3f(x, y, z));
        }
      }
      vertices.add(new Vector3f(0,-r,0));
      normals.add(new Vector3f(0,-1,0));
      
      for (int aa = 0; aa <= div/2; aa++) {
        for (int bb = 0; bb < div; bb++) {
          if (aa == 0) {
            // top triangle fan
            indices.add(0);
            indices.add(bb + 1);
            indices.add((1+bb)%div + 1);
          } else if (aa == div/2) {
            // bottom triangle fan
            indices.add(bb + (div/2)*(div-4) + 1);
            indices.add((1+bb)%div + (div/2)*(div-4) + 1);
            indices.add((div/2)*(div-4) + 1 + 1);
          } else {
            // middle quad stripe
            // tri a
            indices.add((aa-1) * div + bb + 1);
            indices.add((1+aa-1) * div + bb + 1);
            indices.add((1+aa-1) * div + (1+bb)%div + 1);
            // tri b
            indices.add((aa-1) * div + bb + 1);
            indices.add((1+aa-1) * div + (1+bb)%div + 1);
            indices.add((aa-1) * div + (1+bb)%div + 1);
          }
        }
      }
    }
  }

}
