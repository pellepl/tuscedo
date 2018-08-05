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


// TODO things crash on linux openjdk when resizing width, running this class' main method.
// why the heck does it do this!!??!?!?!

// https://www.lighthouse3d.com/tutorials/opengl_framebuffer_objects/
public class Scene3D {
  volatile static boolean destroyed = false;
  GLFWErrorCallback errorCallback;
  GLFWKeyCallback keyCallback;
  GLFWFramebufferSizeCallback fbCallback;

  long window;
  int width = 400;
  int height = 300;
  int nwidth = width;
  int nheight = height;

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
          nwidth = w;
          nheight = h;
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
  BufferedImage swingImage;
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
  int mLocViewProjectionGL;
  int vLocTopColorGL;
  int vLocBotColorGL;
  int vLocLightPosGL;
  int vLocPlayerViewGL;
  int vLocPlayerPosGL;
  int vao_sculptureGL, vbo_sculptureGL;
  int vbo_sculptureArrIxGL;
  
  int numSculptureVertices;
  int numSculptureNormals;
  int numSculptureIndices;


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
        +"uniform vec3 tcolor; \n"
        +"uniform vec3 bcolor; \n"
        +"uniform mat4 model; \n"
        +"uniform mat4 viewproj; \n"
        +"uniform vec3 vLightPos ; \n"
        +"uniform vec3 vPlayerView; \n"
        +"uniform vec3 vPlayerPos; \n"
        +""
        +"out vec3 vOTVertexColor; \n"
        +"out vec3 vOBVertexColor; \n"
        +"out vec3 vONormal; \n"
        +"out vec3 vOLightPos; \n"
        +"out vec3 vOPosition; \n"
        +""
        +"void main() { \n"
        +"  vOTVertexColor = tcolor; \n"
        +"  vOBVertexColor = bcolor; \n"
        +""
        +"  vec4 P = model * vec4(position, 1.0); \n"
        +"  vONormal = normalize(mat3(model) * normal); \n"
        +""
        +"  vOLightPos = vLightPos; \n"
        +"  vOPosition = vec3(P); \n"
        +""
        +"  gl_Position = viewproj * P; \n"
        +"} \n"
        );
    int fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 vOTVertexColor; \n" 
        +"in vec3 vOBVertexColor; \n" 
        +"in vec3 vONormal; \n"
        +"in vec3 vOLightPos; \n"
        +"in vec3 vOPosition; \n" 
        +""
        +"out vec4 fragColor; \n" 
        +""
        +"void main() {\n"
        +"  vec3 color; \n" 
        +"  float ambient = 0.15; \n"
        +"  vec3 n;"
        +"  if (gl_FrontFacing) { \n"
        +"    n = vONormal; \n"
        +"    color = vOTVertexColor; \n"
        +"  } else { \n"
        +"    n = -vONormal; \n"
        +"    color = vOBVertexColor; \n"
        +"  } \n"
        +"  vec3 s = normalize( vec3(vOLightPos - vOPosition) ); \n"
        +"  vec3 v = normalize( vec3( -vOPosition) ); \n"
        +"  vec3 r = reflect( -s, n ); \n"
        +"  float diffuse = max(0, dot(-s, n)); \n"
        +"  float specular = pow(max(0, dot(r, v)), 42.0); \n" 
        +"  fragColor = vec4(color, 1.0) * min(1.0, ambient+diffuse) + vec4(1,1,1,1) * specular; \n" 
        +"} \n"
        );
    progGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocModelGL = glGetUniformLocation(progGL, "model");
    mLocViewProjectionGL = glGetUniformLocation(progGL, "viewproj");
    vLocTopColorGL = glGetUniformLocation(progGL, "tcolor");
    vLocBotColorGL = glGetUniformLocation(progGL, "bcolor");
    vLocLightPosGL = glGetUniformLocation(progGL, "vLightPos");
    vLocPlayerViewGL = glGetUniformLocation(progGL, "vPlayerView");
    vLocPlayerPosGL = glGetUniformLocation(progGL, "vPlayerPos");
  }

  ByteBuffer nativeBuffer;
  int ccap;
  void dumpToSwingImage() {
    // dump renderbuffer to image
    swingImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    int cap = width*height*3;
    cap = ((cap + 255) / 256) * 256;
    //System.out.printf("dump2SI:cap:%d (%dx%d) prev:%s\n", cap, width, height, nativeBuffer);
    if (nativeBuffer == null || cap != ccap) {
      nativeBuffer = BufferUtils.createByteBuffer(cap);
      ccap = cap;
    } else if (nativeBuffer != null) {
      nativeBuffer.flip();
    }
    GL11.glReadPixels(0, 0, width, height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, nativeBuffer);
    byte[] imgData = ((DataBufferByte)swingImage.getRaster().getDataBuffer()).getData();
    nativeBuffer.get(imgData);
  }
 
  final Vector3f _playerPos = new Vector3f();
  final Matrix4f viewProj = new Matrix4f();
  final FloatBuffer fbViewProj = BufferUtils.createFloatBuffer(16);
  final Matrix4f mRot = new Matrix4f();
  final Matrix4f mModel = new Matrix4f();
  final FloatBuffer fbModel = BufferUtils.createFloatBuffer(16);
  final Quaternionf qdirinv = new Quaternionf();

  static final float ZNEAR = 0.1f;
  static final float ZFAR = 10000f;
  
  static RenderSpec defspec = new RenderSpec();
  static {
    defspec.primitive = RenderSpec.PRIMITIVE_SOLID;
    defspec.depthTest = true;
    defspec.cullFaces = true;
    float grid[][] = new float[128][128];
    for (int xx = 0; xx < grid.length; xx++) {
      for (int zz = 0; zz < grid[0].length; zz++) {
        int dx = xx - grid.length / 2;
        int dz = zz - grid[0].length / 2;
      grid[xx][zz] = (float)(
      -15 + (float)Math.cos(0.3f*Math.sqrt(dx*dx + dz*dz)) * (8f - 0.4f*Math.sqrt(dx*dx + dz*dz))
      );
      }
    }
    defspec.model = grid;
    defspec.modelDirty = true;
  }
  
  int lastRenderSpecId;
  
  private void handleRenderSpec(RenderSpec rs) {
    boolean newModel = false;
    boolean newModelData = false;
    if (lastRenderSpecId != rs.id) {
      lastRenderSpecId = rs.id;
      newModel = true;
    }
    newModel |= rs.modelDirty;
    newModelData = rs.modelDataDirty;
    rs.modelDirty = false;
    rs.modelDataDirty = false;
    
    // TODO
    // see if things can differ from newModel / newModelData (optimize)
    // handle float[][][]
    // clear previous vaos and vbos when new model
    if (newModel || newModelData) {
      // sculpture data
      float[][] grid = (float[][])rs.model;
      RenderObject sculpture = new RenderGrid(grid); // RenderSphere(3f, 240);
      sculpture.build();
      numSculptureVertices = sculpture.vertices.size();
      numSculptureNormals = numSculptureVertices;
      numSculptureIndices = sculpture.indices.size();
      
      FloatBuffer verticesNormals = BufferUtils.createFloatBuffer((numSculptureVertices + numSculptureNormals) * 3);
      for (int i = 0; i < numSculptureVertices; i++) {
        Vector3f v = sculpture.vertices.get(i);
        Vector3f n = sculpture.normals.get(i);
        verticesNormals.put(v.x).put(v.y).put(v.z);
        verticesNormals.put(n.x).put(n.y).put(n.z);
      }
      verticesNormals.flip();
      
      ShortBuffer indices = BufferUtils.createShortBuffer(numSculptureIndices);
      for (int i = 0; i < numSculptureIndices; i++) {
        int index = sculpture.indices.get(i);
        indices.put((short)index);
      }
      indices.flip();
      
      vao_sculptureGL = glGenVertexArrays();
      glBindVertexArray(vao_sculptureGL);
      vbo_sculptureGL = glGenBuffers();
      glBindBuffer(GL_ARRAY_BUFFER, vbo_sculptureGL);
      glBufferData(GL_ARRAY_BUFFER, verticesNormals, GL_STATIC_DRAW);

      vbo_sculptureArrIxGL = glGenBuffers();
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_sculptureArrIxGL);
      glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
      
      // setup shader data inputs
      glUseProgram(progGL);

      glBindVertexArray(vao_sculptureGL);
      glBindBuffer(GL_ARRAY_BUFFER, vbo_sculptureGL);
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_sculptureArrIxGL);
      
      int attrPart_pos = glGetAttribLocation(progGL, "position");
      glEnableVertexAttribArray(attrPart_pos);
      glVertexAttribPointer(attrPart_pos, 3, GL_FLOAT, false, 6 * 4, 0);

      int attrPart_norm = glGetAttribLocation(progGL, "normal");
      glEnableVertexAttribArray(attrPart_norm);
      glVertexAttribPointer(attrPart_norm, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
    }
  }
  
  public void render() {
    render(defspec);
  }
  public void render(RenderSpec rs) {
    handleRenderSpec(rs);
    long thisTime = System.nanoTime();
    float diffMs = (thisTime - firstTime) / 1E9f;

    // calc global viewing matrices
    viewProj.setPerspective((float)Math.PI/4f, (float)width/(float)height, ZNEAR, ZFAR);
    _playerPos.set(rs.playerPos);
    _playerPos.negate();
    rs.qdir.invert(qdirinv); 
    qdirinv.get(mRot);
    viewProj.mul(mRot).translate(_playerPos);
    viewProj.get(fbViewProj);
    
    // setup GL view
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, width, height);
    glClearColor(.05f, .05f, .2f, 1f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    if (rs.depthTest) {
      glEnable(GL_DEPTH_TEST);
    } else {
      glDisable(GL_DEPTH_TEST);
    }
    if (rs.cullFaces) {
      glEnable(GL_CULL_FACE);
    } else {
      glDisable(GL_CULL_FACE);
    }

    // paint
    glUseProgram(progGL);
    glBindVertexArray(vao_sculptureGL);
    glUniformMatrix4fv(mLocViewProjectionGL, false, fbViewProj);
    glUniform3f(vLocPlayerPosGL, -rs.playerPos.x, -rs.playerPos.y, -rs.playerPos.z);
    glUniform3f(vLocPlayerViewGL, rs.vdirz.x, rs.vdirz.y, rs.vdirz.z);
    glUniform3f(vLocLightPosGL, 40000f, 40000f, 15000f);
    glUniform3f(vLocTopColorGL, .5f,.4f,.3f);
    glUniform3f(vLocBotColorGL, .1f,.4f,.2f);

    if (standalone) {
      mModel.identity();
      mModel.translate(
          (float)(5f*Math.sin(diffMs*1.1)),
          (float)(5f*Math.sin(diffMs*1.3)),
          (float)(-20f + 5f*Math.sin(diffMs*1.5)));
    } else {
      mModel.set(rs.modelMatrix);
    }
    mModel.get(fbModel);
    glUniformMatrix4fv(mLocModelGL, false, fbModel);
    int mode = GL_TRIANGLES;
    if (rs.primitive == RenderSpec.PRIMITIVE_WIREFRAME) mode = GL_LINES;
    else if (rs.primitive == RenderSpec.PRIMITIVE_DOTS) mode = GL_POINTS;
    glDrawElements(mode, numSculptureIndices, GL_UNSIGNED_SHORT, 0);
    
    int err = glGetError();
    if (err != 0) System.out.println("GLERROR:" + Integer.toHexString(err));

    // coda

    glfwSwapBuffers(window);
    glfwPollEvents();
  
    dumpToSwingImage();
    
    width = nwidth;
    height = nheight;
  }

  public void destroy() {
    //glDeleteVertexArrays(vao_sculptureGL);
    //glDeleteBuffers(vbo_sculptureGL);
    //glDeleteBuffers(vbo_sculptureArrIxGL);

    glfwDestroyWindow(window);
    //glDeleteProgram(progGL);
    keyCallback.free();
    glfwTerminate();
    errorCallback.free();
  }
  
  public BufferedImage getImage() {
    return swingImage;
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
  

  abstract class RenderObject {
    public List<Vector3f> vertices = new ArrayList<Vector3f>();
    public List<Vector3f> normals = new ArrayList<Vector3f>();
    public List<Integer> indices= new ArrayList<Integer>();
    public abstract void build();
  }
  
  class RenderGrid extends RenderObject {
    int w, h;
    float[][] map;
    public RenderGrid(float[][] map) {
      this.w = map.length-1;
      this.h = map[0].length-1;
      this.map = map;
    }
    
    public void build() {
      float offsX = -(float)w / 2;
      float offsZ = -(float)h / 2;
      for (int z = 0; z < h; z++) {
        for (int x = 0; x < w; x++) {
          vertices.add(new Vector3f(x+offsX, map[x][z], z+offsZ));
          
          Vector3f nx = new Vector3f(1f, map[x+1][z] - map[x][z], 0f);
          Vector3f nz = new Vector3f(0f, map[x][z+1] - map[x][z], 1f);
          nx.normalize();
          nz.normalize();
          normals.add(nx.cross(nz));
        }
      }
      for (int z = 0; z < h-1; z++) {
        for (int x = 0; x < w-1; x++) {
          indices.add((z+0)*w + (x+0));
          indices.add((z+1)*w + (x+0));
          indices.add((z+0)*w + (x+1));
          
          indices.add((z+1)*w + (x+0));
          indices.add((z+1)*w + (x+1));
          indices.add((z+0)*w + (x+1));
        }
      }
    }
  }
  
  // TODO remove this
  class RenderSphere extends RenderObject {
    float r;
    int div;

    public RenderSphere(float radius, int div) {
      r = radius;
      this.div = div;
    }
    
    public void build() {
      vertices.add(new Vector3f(0,r,0));
      normals.add(new Vector3f(0,1,0));
      for (int aa = 1; aa <= div/2; aa++) {
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
          normals.add(new Vector3f(x, y, z).normalize());
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
            indices.add((div/2)*(div-4) + 1 + 1);
            indices.add((1+bb)%div + (div/2)*(div-4) + 1);
            indices.add(bb + (div/2)*(div-4) + 1);
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
