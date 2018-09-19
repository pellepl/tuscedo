package com.pelleplutt.tuscedo.ui;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.io.*;
import java.lang.Math;
import java.nio.*;
import java.util.*;
import java.util.List;

import org.joml.*;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import com.pelleplutt.util.*;


// TODO things crash on linux openjdk when resizing width, running this class' main method.
// why the heck does it do this!!??!?!?!

// https://www.lighthouse3d.com/tutorials/opengl_framebuffer_objects/
public class Scene3D {
  volatile static boolean destroyed = false;
  GLFWErrorCallback errorCallback;
  GLFWKeyCallback keyCallback;
  GLFWFramebufferSizeCallback fbCallback;

  long window;

  static final float ZNEAR = 0.1f;
  static final float ZFAR = 5000f;
  static final float SHADOW_PROJECTION_RADIUS = 100f;
  static final float SHADOW_Z_BIAS = 0.0005f;
  
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
    window = glfwCreateWindow(100, 100, "offscreen", NULL, NULL);
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
//        if (w > 0 && h > 0) {
//          nwidth = w;
//          nheight = h;
//        }
      }
    });

    // misc stuff
    GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window, (vidmode.width() - 100) / 2, (vidmode.height() - 100) / 2);
    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
  }
  
  BufferedImage swingImage;

  public void init() {
    initGLFW();
    initGL();
  }
  
  int progShadowGL;
  int fbo_depthMap;
  int texShadowMap;
  int mLocShadowModelGL;
  int mLocShadowLightProjGL;
  
  int progModelGL;
  int mLocModelGL;
  int mLocViewProjectionGL;
  int mLocLightSpaceGL;
  int vLocBotColorGL;
  int vLocLightPosGL;
  int vLocPlayerViewGL;
  int vLocPlayerPosGL;
  int iLocSmoothOrFlatColorGL;
  int iLocShadowsGL;
  int iLocCheckeredGL;
  int texModelGrid;

  int progGridGL;
  int mLocGridModelGL;
  int mLocGridViewProjectionGL;
  int vLocGridColorGL;
  int vLocGridPlayerPosGL;
  int vao_gridGL, vbo_gridGL;
  int vbo_gridArrIxGL;

  int progSkyboxGL;
  int mLocSkyboxViewProjectionGL;
  int vao_skyboxGL;
  int texSkybox;

  int progTestGL;
  int texTest;

  
  final int gridLines = 201;

  
  void initGL() {
    GL.createCapabilities();
    System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
    setupGL();
  }
  
  static final int SHADOW_MAP_W = 1024;
  static final int SHADOW_MAP_H = 1024;
  
  void setupGL() {
    int vertexShader, fragmentShader;
    // SHADOW MAPPER
    fbo_depthMap = glGenFramebuffers();
    texShadowMap = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, texShadowMap);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, 
        SHADOW_MAP_W, SHADOW_MAP_H, 0, GL_DEPTH_COMPONENT, GL_FLOAT, NULL);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER); 
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);  
    float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);  
    
    vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 position; \n"
        +""
        +"uniform mat4 model; \n"
        +"uniform mat4 mLightSpace; \n"
        +""
        +"void main() { \n"
        +"  gl_Position = mLightSpace* model * vec4(position, 1.0); \n"
        +"} \n"
        );
    fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"void main() {\n"
        +"  // gl_FragDepth = gl_FragCoord.z; \n"
        +"} \n"
        );
    progShadowGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocShadowModelGL = glGetUniformLocation(progShadowGL, "model");
    mLocShadowLightProjGL = glGetUniformLocation(progShadowGL, "mLightSpace");
    
    // MODEL SHADERS
    
    vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 position; \n"
        +"in vec3 normal; \n"
        +"in float fcolor; \n"
        +""
        +"uniform vec3 bcolor; \n"
        +"uniform mat4 model; \n"
        +"uniform mat4 viewproj; \n"
        +"uniform mat4 mLightSpace; \n"
        +"uniform vec3 vLightPos ; \n"
        +"uniform vec3 vPlayerView; \n"
        +"uniform vec3 vPlayerPos; \n"
        +""
        +"out vec3 vOTVertexColorSmooth; \n"
        +"flat out vec3 vOTVertexColor; \n"
        +"out vec3 vOBVertexColor; \n"
        +"out vec3 vONormal; \n"
        +"out vec3 vOLightPos; \n"
        +"out vec3 vOPosition; \n"
        +"out vec3 vOP; \n"
        +"out vec3 vOPlayerPosition; \n"
        +"out vec4 vOFragPosLightSpace; \n"
        +""
        +"vec3 unpack_color(float f) { \n"
        +"  vec3 c; \n"
        +"  c.b = floor(f / 256.0 / 256.0); \n"
        +"  c.g = floor((f - c.b * 256.0 * 256.0) / 256.0); \n"
        +"  c.r = floor(f - c.b * 256.0 * 256.0 - c.g * 256.0); \n"
        +"  return c / 255.0; \n"
        +"} \n"
        +""
        +"void main() { \n"
        +"  vOTVertexColor = unpack_color(fcolor); \n"
        +"  vOTVertexColorSmooth = vOTVertexColor; \n"
        +"  vOBVertexColor = bcolor; \n"
        +""
        +"  vec4 P = model * vec4(position, 1.0); \n"
        +"  vOFragPosLightSpace = mLightSpace * P; \n"
        +"  vONormal = normalize(mat3(model) * normal); \n"
        +"  vOP = position; \n"
        +""
        +"  vOLightPos = vLightPos; \n"
        +"  vOPosition = vec3(P); \n"
        +"  vOPlayerPosition = vPlayerPos; \n"
        +""
        +"  gl_Position = viewproj * P; \n"
        +"} \n"
        );
    //https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping
    fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 vOTVertexColorSmooth; \n" 
        +"flat in vec3 vOTVertexColor; \n" 
        +"in vec3 vOBVertexColor; \n"
        +"in vec3 vOPosition; \n" 
        +"in vec3 vOP; \n" 
        +"in vec3 vONormal; \n" 
        +"in vec3 vOLightPos; \n" 
        +"in vec3 vOPlayerPosition; \n" 
        +"in vec4 vOFragPosLightSpace; \n"
        +""
        +"uniform int iSmoothOrFlatColor; \n"
        +"uniform int iShadows; \n"
        +"uniform int iCheckered; \n"
        +"uniform sampler2D shadowMap; \n"
        +""
        +"out vec4 fragColor; \n" 
        +""
        +"float calc_shadow(vec4 fragPosLightSpace) { \n" 
        +"    // perform perspective divide \n" 
        +"    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w; \n" 
        +"    // transform to [0,1] range \n" 
        +"    projCoords = projCoords * 0.5 + 0.5; \n" 
        +"    // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords) \n" 
        +"    float closestDepth = texture(shadowMap, projCoords.xy).r; \n" 
        +"    // get depth of current fragment from light's perspective \n" 
        +"    float currentDepth = projCoords.z; \n" 
        +"    // check whether current frag pos is in shadow \n"
        +"    float bias = " + SHADOW_Z_BIAS + "; \n"
        +"    float shadow = 0.0;\n" 
        +"    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);\n" 
        +"    for(int x = -2; x <= 2; ++x) {\n" 
        +"      for(int y = -2; y <= 2; ++y) {\n" 
        +"        float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r; \n" 
        +"        shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;        \n" 
        +"      } \n" 
        +"    } \n" 
        +"    shadow /= 25.0; \n"
        +"    if (projCoords.z > 1.0) shadow = 0.0; \n"
        
        +"    return shadow;\n"
        +"} \n"
        +""
        +"void main() {\n"
        +"  vec3 color; \n" 
        +"  vec3 n; \n"
        +"  vec3[2] carr; \n"
        +""
        +"  carr[0] = vOTVertexColorSmooth; \n"
        +"  carr[1] = vOTVertexColor; \n"
        +"  float ambient = 0.15; \n"
        +"  if (gl_FrontFacing) { \n"
        +"    n = vONormal; \n"
        +"    color = carr[iSmoothOrFlatColor]; \n"
        +"  } else { \n"
        +"    n = -vONormal; \n"
        +"    color = vOBVertexColor; \n"
        +"  } \n"
        +"  vec3 s = normalize( vOLightPos - vOPosition ); \n"
        +"  vec3 v = normalize( vOPlayerPosition - vOPosition ); \n"
        +"  vec3 r = reflect( -s, n ); \n"
        +"  float diffuse = max(0, dot(-s, n)); \n"
        +"  float specular = pow(max(0, dot(r, v)), 50.0); \n" 
        +"  float shadow = iShadows != 0 ? (gl_FrontFacing ? calc_shadow(vOFragPosLightSpace) : 0) : 0; \n"
        +"  if (iCheckered != 0 && (int((vOP.x+10000)/5) + int((vOP.y+10000)/5) + int((vOP.z+10000)/5) ) %2 < 1 ) \n"
        +"     color *= 0.5;  \n" 
        +"  specular *= (1.0 - shadow);  \n" 
        +"  fragColor = (ambient + (1.0 - shadow * 0.8) * (diffuse + specular)) * vec4(color, 1.0) +  \n"
        +"              specular * vec4(.2,.2,.2,1); \n"
        +"} \n"
        );
    progModelGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocModelGL = glGetUniformLocation(progModelGL, "model");
    mLocViewProjectionGL = glGetUniformLocation(progModelGL, "viewproj");
    mLocLightSpaceGL = glGetUniformLocation(progModelGL, "mLightSpace");
    vLocBotColorGL = glGetUniformLocation(progModelGL, "bcolor");
    vLocLightPosGL = glGetUniformLocation(progModelGL, "vLightPos");
    vLocPlayerViewGL = glGetUniformLocation(progModelGL, "vPlayerView");
    vLocPlayerPosGL = glGetUniformLocation(progModelGL, "vPlayerPos");
    iLocSmoothOrFlatColorGL = glGetUniformLocation(progModelGL, "iSmoothOrFlatColor");
    iLocShadowsGL = glGetUniformLocation(progModelGL, "iShadows");
    iLocCheckeredGL = glGetUniformLocation(progModelGL, "iCheckered");
  
    // GRID SHADERS
    
    vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec3 position; \n"
        +"in float thickness; \n"
        +""
        +"uniform vec4 color; \n"
        +"uniform mat4 model; \n"
        +"uniform mat4 viewproj; \n"
        +"uniform vec3 vPlayerPos; \n"
        +""
        +"out vec4 vOVertexColor; \n"
        +"out float fOThickness; \n"
        +"out vec3 vOPosition; \n"
        +"out vec3 vOPlayerPosition; \n"
        +""
        +"void main() { \n"
        +"  vOVertexColor = color; \n"
        +"  vec4 P = model * vec4(position, 1.0); \n"
        +"  fOThickness = thickness; \n"
        +"  vOPosition = vec3(P); \n"
        +"  vOPlayerPosition = vPlayerPos; \n"
        +"  gl_Position = viewproj * P; \n"
        +"} \n"
        );
    fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"in vec4 vOVertexColor; \n" 
        +"in float fOThickness; \n"
        +"in vec3 vOPosition; \n" 
        +"in vec3 vOPlayerPosition; \n" 
        +""
        +"out vec4 fragColor; \n" 
        +""
        +"void main() {\n"
        +"  vec3 d = vOPlayerPosition - vOPosition; \n"
        +"  float distfact = min(1, length(d)/" + (ZFAR - ZNEAR)/(4f*4f)  +"); \n"
        +"  fragColor = vec4(vOVertexColor.xyz, vOVertexColor.w * fOThickness * (1 - distfact*distfact)); \n"
        +"} \n"
        );
    progGridGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocGridModelGL = glGetUniformLocation(progGridGL, "model");
    mLocGridViewProjectionGL = glGetUniformLocation(progGridGL, "viewproj");
    vLocGridColorGL = glGetUniformLocation(progGridGL, "color");
    vLocGridPlayerPosGL = glGetUniformLocation(progGridGL, "vPlayerPos");

    // grid data
    int numGridVertices = (gridLines + gridLines) * 2;
    final float mul = 1.0f;
    FloatBuffer gridVertices = BufferUtils.createFloatBuffer(numGridVertices * 4);
    Vector3f v; 
    for (int i = 0; i < gridLines; i++) {
      float t = 0.25f;
      int ix = i <= gridLines/2 ? i : i - (gridLines/2);
      if (i == gridLines / 2) {
        t = 1f;
      } else if ((ix % 10) == 0) {
        t = 0.5f;
      }
      v = new Vector3f(-gridLines/2 + i, 0, -gridLines/2); v.mul(mul); 
      gridVertices.put(v.x).put(v.y).put(v.z);
      gridVertices.put(t);
      v = new Vector3f(-gridLines/2 + i, 0, gridLines/2);  v.mul(mul);
      gridVertices.put(v.x).put(v.y).put(v.z);
      gridVertices.put(t);
      
      v = new Vector3f(-gridLines/2, 0, -gridLines/2 + i);  v.mul(mul);
      gridVertices.put(v.x).put(v.y).put(v.z);
      gridVertices.put(t);
      v = new Vector3f(gridLines/2, 0, -gridLines/2 + i);  v.mul(mul);
      gridVertices.put(v.x).put(v.y).put(v.z);
      gridVertices.put(t);
    }

    gridVertices.flip();
    
    IntBuffer indices = BufferUtils.createIntBuffer(numGridVertices);
    for (int i = 0; i < numGridVertices; i++) {
      indices.put(i);
    }
    indices.flip();
    
    vao_gridGL = glGenVertexArrays();
    glBindVertexArray(vao_gridGL);
    vbo_gridGL = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbo_gridGL);
    glBufferData(GL_ARRAY_BUFFER, gridVertices, GL_STATIC_DRAW);

    vbo_gridArrIxGL = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_gridArrIxGL);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
    
    // setup shader data inputs
    glUseProgram(progGridGL);

    glBindVertexArray(vao_gridGL);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_gridGL);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo_gridArrIxGL);
    
    int attrPart_pos = glGetAttribLocation(progGridGL, "position");
    glEnableVertexAttribArray(attrPart_pos);
    glVertexAttribPointer(attrPart_pos, 3, GL_FLOAT, false, 4 * 4, 0);

    int attrPart_thick = glGetAttribLocation(progGridGL, "thickness");
    glEnableVertexAttribArray(attrPart_thick);
    glVertexAttribPointer(attrPart_thick, 1, GL_FLOAT, false, 4 * 4, 4 * 3);
    
    // SKYBOX SHADERS
    
    vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"uniform mat4 viewproj; \n"
        +""
        +"out vec3 vtc; \n"
        +""
        +"void main() { \n"
        +"  vec3[36] verts = vec3[36]( \n"
        +"    vec3(-1.0, 1.0,-1.0), vec3(-1.0,-1.0,-1.0), vec3( 1.0,-1.0,-1.0), \n"
        +"    vec3( 1.0,-1.0,-1.0), vec3( 1.0, 1.0,-1.0), vec3(-1.0, 1.0,-1.0), \n"
        +""
        +"    vec3(-1.0,-1.0, 1.0), vec3(-1.0,-1.0,-1.0), vec3(-1.0, 1.0,-1.0), \n"
        +"    vec3(-1.0, 1.0,-1.0), vec3(-1.0, 1.0, 1.0), vec3(-1.0,-1.0, 1.0), \n"
        +""
        +"    vec3( 1.0,-1.0,-1.0), vec3( 1.0,-1.0, 1.0), vec3( 1.0, 1.0, 1.0), \n"
        +"    vec3( 1.0, 1.0, 1.0), vec3( 1.0, 1.0,-1.0), vec3( 1.0,-1.0,-1.0), \n"
        +""
        +"    vec3(-1.0,-1.0, 1.0), vec3(-1.0, 1.0, 1.0), vec3( 1.0, 1.0, 1.0), \n"
        +"    vec3( 1.0, 1.0, 1.0), vec3( 1.0,-1.0, 1.0), vec3(-1.0,-1.0, 1.0), \n"
        +""
        +"    vec3(-1.0, 1.0,-1.0), vec3( 1.0, 1.0,-1.0), vec3( 1.0, 1.0, 1.0), \n"
        +"    vec3( 1.0, 1.0, 1.0), vec3(-1.0, 1.0, 1.0), vec3(-1.0, 1.0,-1.0), \n"
        +""
        +"    vec3(-1.0,-1.0,-1.0), vec3(-1.0,-1.0, 1.0), vec3( 1.0,-1.0,-1.0), \n"
        +"    vec3( 1.0,-1.0,-1.0), vec3(-1.0,-1.0, 1.0), vec3( 1.0,-1.0, 1.0)); \n"
        +"  vtc = verts[gl_VertexID]; \n"
        +"  gl_Position = viewproj * vec4(verts[gl_VertexID], 1); \n"
        +"} \n"
        );
    fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"uniform samplerCube tex_cubemap; \n"
        +""
        +"in vec3 vtc; \n"
        +""
        +"out vec4 fragColor; \n" 
        +""
        +"void main() {\n"
        +"  fragColor = texture(tex_cubemap, vtc); \n"
        +"} \n"
        );
    progSkyboxGL = createProgram(vertexShader, fragmentShader);
    
    // obtain uniform locations for shader variables
    mLocSkyboxViewProjectionGL = glGetUniformLocation(progSkyboxGL, "viewproj");
    vao_skyboxGL = glGenVertexArrays();
    // create texture
    texSkybox = glGenTextures();
    glBindTexture(GL_TEXTURE_CUBE_MAP, texSkybox);
    //loadSkyboxTextures("skyboxes/maskonaive2");
    calcSkyboxTextures(new Color(8,8,64), new Color(0,0,8));

  
    // TEST SHADERS
    
    vertexShader = createShader(GL_VERTEX_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"uniform mat4 viewproj; \n"
        +""
        +"out vec3 vtc; \n"
        +""
        +"void main() { \n"
        +"  vec3[6] verts = vec3[6]( \n"
        +"    vec3(-1.0, 1.0,-1.0), vec3(-1.0,-1.0,-1.0), vec3( 1.0,-1.0,-1.0), \n"
        +"    vec3( 1.0,-1.0,-1.0), vec3( 1.0, 1.0,-1.0), vec3(-1.0, 1.0,-1.0)); \n"
        +"  vtc = verts[gl_VertexID]; \n"
        +"  gl_Position = vec4(verts[gl_VertexID], 1); \n"
        +"} \n"
        );
    fragmentShader = createShader(GL_FRAGMENT_SHADER, "" 
        +"#version 330 core \n" 
        +""
        +"uniform sampler2D tex; \n"
        +""
        +"in vec3 vtc; \n"
        +""
        +"out vec4 fragColor; \n" 
        +""
        +"void main() {\n"
        +"  fragColor = vec4(texture(tex, vtc.xy).r,0,0,1); \n"
        +"} \n"
        );
//    progTestGL = createProgram(vertexShader, fragmentShader);
//    texTest = glGenTextures();
//    glBindTexture(GL_TEXTURE_2D, texTest);
//    BufferedImage bi = new BufferedImage(1024, 1024, BufferedImage.TYPE_4BYTE_ABGR);
//    Graphics g = bi.getGraphics();
//    g.setColor(Color.white);
//    g.fillRect(0, 0, 1024, 1024);
//    g.setColor(Color.black);
//    g.drawLine(0, 0, 1024, 1024);
//    g.drawLine(1024, 0, 0, 1024);
//    g.fillRect(500, 500, 24, 24);
//    g.dispose();
//    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, SHADOW_MAP_W, SHADOW_MAP_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, 
//        createTextureBuffer(bi));
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); 
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);  
  }
  
  ByteBuffer nativeBuffer;
  int ccap;
  void dumpToSwingImage(RenderSpec rs) {
    // dump renderbuffer to image
    swingImage = new BufferedImage(rs.width, rs.height, BufferedImage.TYPE_3BYTE_BGR);
    int cap = rs.width*rs.height*3;
    //cap = ((cap + 255) / 256) * 256;
    //System.out.printf("dump2SI:cap:%d (%dx%d) prev:%s\n", cap, width, height, nativeBuffer);
    if (nativeBuffer == null || cap != ccap) {
      nativeBuffer = BufferUtils.createByteBuffer(cap);
      ccap = cap;
    } else if (nativeBuffer != null) {
      nativeBuffer.flip();
    }
    GL11.glReadPixels(0, 0, rs.width, rs.height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, nativeBuffer);
    byte[] imgData = ((DataBufferByte)swingImage.getRaster().getDataBuffer()).getData();
    nativeBuffer.get(imgData);
  }
 
  final Vector3f _playerPos = new Vector3f();
  final Matrix4f viewProj = new Matrix4f();
  final Matrix4f lightProj = new Matrix4f();
  final FloatBuffer fbLightProj = BufferUtils.createFloatBuffer(16);
  final FloatBuffer fbViewProj = BufferUtils.createFloatBuffer(16);
  final FloatBuffer fbViewRot = BufferUtils.createFloatBuffer(16);
  final Matrix4f mRot = new Matrix4f();
  final Matrix4f mModel = new Matrix4f();
  final FloatBuffer fbModel = BufferUtils.createFloatBuffer(16);
  final Quaternionf qdirinv = new Quaternionf();

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
  
  public static float colToFloat(float r, float g, float b) {
    r = (int)(Math.min(1f,r)*255f);
    g = (int)(Math.min(1f,g)*255f);
    b = (int)(Math.min(1f,b)*255f);
    return r + (float)Math.floor(g*256f) + (float)Math.floor(b*65536f);
  }
  
  private void handleRenderSpec(RenderSpec rs) {
    boolean newSpec = false;
    boolean calcModel = false;
    if (lastRenderSpecId != rs.id) {
      lastRenderSpecId = rs.id;
      newSpec = true;
    }
    
    if (newSpec || rs.dimensionDirty) {
      glfwSetWindowSize(window, rs.width, rs.height);
      // TODO need to do this in order to make size changes work
      glfwShowWindow(window);
      glfwHideWindow(window);
      rs.dimensionDirty = false;
    }
    
    calcModel |= rs.modelDirty;
    calcModel |= rs.modelDataDirty;
    rs.modelDirty = false;
    rs.modelDataDirty = false;
    
    // clear previous vaos and vbos when new model
    if (calcModel) {
      if (rs.vao_sculptureGL != 0) {
        glDeleteVertexArrays(rs.vao_sculptureGL);
      }
      if (rs.vbo_sculptureArrIxGL != 0) {
        glDeleteBuffers(rs.vbo_sculptureArrIxGL);
      }
      if (rs.vbo_sculptureGL != 0) {
        glDeleteBuffers(rs.vbo_sculptureGL);
      }
      // sculpture data
      Modeller3D sculpture = null;
      if (rs.modeltype == RenderSpec.MODEL_HEIGHTMAP || 
          rs.modeltype == RenderSpec.MODEL_HEIGHTMAP_COLOR) {
        if (rs.modeltype == RenderSpec.MODEL_HEIGHTMAP) {
          sculpture = new Modeller3D.Grid((float[][])rs.model);
        } else {
          sculpture = new Modeller3D.Grid((float[][][])rs.model);
        }
      } else if (rs.modeltype == RenderSpec.MODEL_POINTCLOUD ||
                 rs.modeltype == RenderSpec.MODEL_POINTCLOUD_COLOR) {
        if (rs.modeltype == RenderSpec.MODEL_POINTCLOUD) {
          sculpture = new Modeller3D.Cloud((float[][][])rs.model, rs.isolevel, rs.faceted);
        } else {
          sculpture = new Modeller3D.Cloud((float[][][][])rs.model, rs.isolevel, rs.faceted);
        }
      }

      sculpture.build();
      rs.numSculptureVertices = sculpture.vertices.size();
      rs.numSculptureNormals = rs.numSculptureVertices;
      rs.numSculptureIndices = sculpture.indices.size();
      
      FloatBuffer verticesNormals = BufferUtils.createFloatBuffer((rs.numSculptureVertices + rs.numSculptureNormals) * 3 +
          rs.numSculptureVertices);
      for (int i = 0; i < rs.numSculptureVertices; i++) {
        Vector3f v = sculpture.vertices.get(i);
        Vector3f n = sculpture.normals.get(i);
        verticesNormals.put(v.x).put(v.y).put(v.z);
        verticesNormals.put(n.x).put(n.y).put(n.z);
        if (rs.modeltype == RenderSpec.MODEL_HEIGHTMAP || rs.modeltype == RenderSpec.MODEL_POINTCLOUD)
          verticesNormals.put(colToFloat(.5f,.4f,.3f));
        else
          verticesNormals.put(sculpture.colors.get(i));
      }
      verticesNormals.flip();
      
      IntBuffer indices = BufferUtils.createIntBuffer(rs.numSculptureIndices);
      for (int i = 0; i < rs.numSculptureIndices; i++) {
        int index = sculpture.indices.get(i);
        indices.put(index);
      }
      indices.flip();
      
      rs.vao_sculptureGL = glGenVertexArrays();
      glBindVertexArray(rs.vao_sculptureGL);
      rs.vbo_sculptureGL = glGenBuffers();
      glBindBuffer(GL_ARRAY_BUFFER, rs.vbo_sculptureGL);
      glBufferData(GL_ARRAY_BUFFER, verticesNormals, GL_STATIC_DRAW);

      rs.vbo_sculptureArrIxGL = glGenBuffers();
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rs.vbo_sculptureArrIxGL);
      glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
      
      // setup shader data inputs
      glUseProgram(progModelGL);

      int attrPart_pos = glGetAttribLocation(progModelGL, "position");
      glEnableVertexAttribArray(attrPart_pos);
      glVertexAttribPointer(attrPart_pos, 3, GL_FLOAT, false, 7 * 4, 0);

      int attrPart_norm = glGetAttribLocation(progModelGL, "normal");
      glEnableVertexAttribArray(attrPart_norm);
      glVertexAttribPointer(attrPart_norm, 3, GL_FLOAT, false, 7 * 4, 3 * 4);

      int attrPart_col = glGetAttribLocation(progModelGL, "fcolor");
      glEnableVertexAttribArray(attrPart_col);
      glVertexAttribPointer(attrPart_col, 1, GL_FLOAT, false, 7 * 4, 6 * 4);
    }
  }
  
  public void render() {
    render(defspec);
  }
  public synchronized void render(RenderSpec rs) {
    int mode;
    RenderSpec currs;
    glGetError(); // clear gl error
    try {
    if (!deadRenderSpecs.isEmpty()) {
      synchronized (deadRenderSpecs) {
        while(!deadRenderSpecs.isEmpty()) {
          RenderSpec deadrs = deadRenderSpecs.remove(0);
          if (deadrs.getUIInfo().getParent() == null) {
            if (deadrs.vao_sculptureGL != 0) {
              glDeleteVertexArrays(deadrs.vao_sculptureGL);
            }
            if (deadrs.vbo_sculptureArrIxGL != 0) {
              glDeleteBuffers(deadrs.vbo_sculptureArrIxGL);
            }
            if (deadrs.vbo_sculptureGL != 0) {
              glDeleteBuffers(deadrs.vbo_sculptureGL);
            }
            deadrs.vao_sculptureGL = deadrs.vbo_sculptureArrIxGL = deadrs.vbo_sculptureGL = 0;
            glerr();
          }
        }
      }
    }
    handleRenderSpec(rs);
    glerr();

    // calc global viewing matrices
    _playerPos.set(rs.playerPos);
    _playerPos.negate();
    viewProj.setPerspective((float)Math.PI/4f, (float)rs.width/(float)rs.height, ZNEAR, ZFAR);
    rs.qdir.invert(qdirinv); 
    qdirinv.get(mRot);
    viewProj.mul(mRot);
    viewProj.get(fbViewRot);
    viewProj.translate(_playerPos);
    viewProj.get(fbViewProj);
    lightProj.identity();
    lightProj.ortho(-SHADOW_PROJECTION_RADIUS, SHADOW_PROJECTION_RADIUS, 
        -SHADOW_PROJECTION_RADIUS, SHADOW_PROJECTION_RADIUS, ZNEAR, ZFAR);
    lightProj.lookAt(rs.lightPos.x, rs.lightPos.y, rs.lightPos.z, 0,0,0, 0,1,0);
    lightProj.get(fbLightProj);
    
    // render shadow map
    glUseProgram(progShadowGL);
    glUniformMatrix4fv(mLocShadowLightProjGL, false,  fbLightProj);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_depthMap);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texShadowMap, 0);
    glDrawBuffer(GL_NONE);
    glReadBuffer(GL_NONE);
    glViewport(0, 0, SHADOW_MAP_W, SHADOW_MAP_H);
    glClear(GL_DEPTH_BUFFER_BIT);
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glBindVertexArray(rs.vao_sculptureGL);
    glBindBuffer(GL_ARRAY_BUFFER, rs.vbo_sculptureGL);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rs.vbo_sculptureArrIxGL);
    mode = GL_TRIANGLES;
    if (rs.primitive == RenderSpec.PRIMITIVE_WIREFRAME) mode = GL_LINES;
    else if (rs.primitive == RenderSpec.PRIMITIVE_DOTS) mode = GL_POINTS;
    
    currs = rs.first();
    while (currs != null) {
      mModel.set(currs.modelMatrix);
      mModel.get(fbModel);
      glUniformMatrix4fv(mLocShadowModelGL, false, fbModel);
      glBindVertexArray(currs.vao_sculptureGL);
      glBindBuffer(GL_ARRAY_BUFFER, currs.vbo_sculptureGL);
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, currs.vbo_sculptureArrIxGL);
      glDrawElements(mode, currs.numSculptureIndices, GL_UNSIGNED_INT, 0);
      glerr();
      currs = currs.next();
    }

    // setup GL view
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, rs.width, rs.height);
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
    glerr();

    // paint skybox
    glUseProgram(progSkyboxGL);
    glBindVertexArray(vao_skyboxGL);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_CUBE_MAP, texSkybox);
    glDisable(GL_DEPTH_TEST);
    glUniformMatrix4fv(mLocSkyboxViewProjectionGL, false, fbViewRot);
    glDrawArrays(GL_TRIANGLES, 0, 36);
    glEnable(GL_DEPTH_TEST);
    glerr();

    // paint sculpture
    glUseProgram(progModelGL);
    glUniformMatrix4fv(mLocViewProjectionGL, false, fbViewProj);
    glUniformMatrix4fv(mLocLightSpaceGL, false, fbLightProj);
    glUniform3f(vLocPlayerPosGL, rs.playerPos.x, rs.playerPos.y, rs.playerPos.z);
    glUniform3f(vLocPlayerViewGL, rs.vdirz.x, rs.vdirz.y, rs.vdirz.z);
    glUniform3f(vLocLightPosGL, rs.lightPos.x, rs.lightPos.y, rs.lightPos.z);
    glUniform3f(vLocBotColorGL, .05f,.3f,.6f);
    glUniform1i(iLocSmoothOrFlatColorGL, rs.smoothOrFlat);
    glUniform1i(iLocShadowsGL, rs.disableShadows ? 0 : 1);
    glUniform1i(iLocCheckeredGL, rs.checkered ? 1 : 0);

    glBindTexture(GL_TEXTURE_2D, texShadowMap);
    glBindVertexArray(rs.vao_sculptureGL);
    glBindBuffer(GL_ARRAY_BUFFER, rs.vbo_sculptureGL);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rs.vbo_sculptureArrIxGL);
    mode = GL_TRIANGLES;
    if (rs.primitive == RenderSpec.PRIMITIVE_WIREFRAME) mode = GL_LINES;
    else if (rs.primitive == RenderSpec.PRIMITIVE_DOTS) mode = GL_POINTS;

    currs = rs.first();
    while (currs != null) {
      mModel.set(currs.modelMatrix);
      mModel.get(fbModel);
      glUniformMatrix4fv(mLocModelGL, false, fbModel);
      glBindVertexArray(currs.vao_sculptureGL);
      glBindBuffer(GL_ARRAY_BUFFER, currs.vbo_sculptureGL);
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, currs.vbo_sculptureArrIxGL);
      glDrawElements(mode, currs.numSculptureIndices, GL_UNSIGNED_INT, 0);
      glerr();
      currs = currs.next();
    }

    // paint grid
    glUseProgram(progGridGL);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_LINE_SMOOTH);
    glDepthMask(false);
    glBindVertexArray(vao_gridGL);
    mModel.identity();
    mModel.scale(rs.gridMul);
    mModel.get(fbModel);
    glUniformMatrix4fv(mLocGridModelGL, false, fbModel);
    glUniformMatrix4fv(mLocGridViewProjectionGL, false, fbViewProj);
    glUniform4f(vLocGridColorGL, .0f,0.1f,.0f, rs.gridContrast * 0.2f);
    glUniform3f(vLocGridPlayerPosGL, rs.playerPos.x, rs.playerPos.y, rs.playerPos.z);
    glDrawElements(GL_LINES, (gridLines + gridLines) * 2, GL_UNSIGNED_INT, 0);
    glEnable(GL_DEPTH_TEST);
    glDepthMask(true);
    glUniform4f(vLocGridColorGL, .0f,0.5f,.1f, rs.gridContrast);
    glDrawElements(GL_LINES, (gridLines + gridLines) * 2, GL_UNSIGNED_INT, 0);

    glDisable(GL_BLEND);
    
    glerr();

    // test
//    glDisable(GL_DEPTH_TEST);
//    glUseProgram(progTestGL);
//    glBindTexture(GL_TEXTURE_2D, texShadowMap);
//    glDrawArrays(GL_TRIANGLES, 0, 6);
    
    // coda

    } catch (GLError err) { err.printStackTrace(); }
    glfwSwapBuffers(window);
    glfwPollEvents();
  
    dumpToSwingImage(rs);
  }
  
  static void glerr() {
    glerr("");
  }
  static void glerr(String prefix) {
    int err = glGetError();
    if (err != 0) {
      throw new GLError(err);
    }
    
  }

  public void destroy() {
    //glDeleteVertexArrays(vao_sculptureGL);
    //glDeleteBuffers(vbo_sculptureGL);
    //glDeleteBuffers(vbo_sculptureArrIxGL);

    glfwDestroyWindow(window);
    //glDeleteProgram(progGL);
    if (keyCallback != null) keyCallback.free();
    glfwTerminate();
    if (errorCallback != null) errorCallback.free();
  }
  
  public BufferedImage getImage() {
    return swingImage;
  }

  //
  // GL helpers
  //
  
  static int createShader(int type, String src) {
    //System.out.println("create shader " + type);
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
  
  static int createProgram(int... shaders) {
    //System.out.println("create program");
    int prog = glCreateProgram();
    //System.out.println("attaching " + shaders.length +  " shaders");
    for (int shader : shaders) {
      glAttachShader(prog, shader);
    }
    //System.out.println("linking program");
    glLinkProgram(prog);
    int status = glGetProgrami(prog, GL_LINK_STATUS);
    if (status != GL_TRUE) {
      int err = glGetError();
      throw new RuntimeException("glerr:" + err + "\n" + glGetProgramInfoLog(prog));
    }
    //System.out.println("deleting shader info");
    for (int shader : shaders) {
      glDetachShader(prog, shader);
      glDeleteShader(shader);
    }
    glGetError();
    
    return prog;
  }
  
  // image -> bytebuffer (8-bit RED, 8-bit GREEN, 8-bit BLUE, 8-bit wotevah)
  static ByteBuffer createTextureBuffer(BufferedImage bufferedImage) {
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

  static void calcSkyboxTextures(Color heaven, Color ground) {
    int dim = 256;
    int target = GL_TEXTURE_CUBE_MAP_POSITIVE_X;
    Paint paint = new GradientPaint(0,0,heaven, 0,dim,ground);
    for (String n : SKYBOX_NAMES) {
      BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D g = (Graphics2D)img.getGraphics();
      //System.out.print("creating img " + n + " " + img.getWidth() + "x" + img.getHeight());
      if (target == GL_TEXTURE_CUBE_MAP_POSITIVE_Y)       g.setColor(heaven);
      else if (target == GL_TEXTURE_CUBE_MAP_NEGATIVE_Y)  g.setColor(ground);
      else                                                g.setPaint(paint);
      g.fillRect(0, 0, dim, dim);
      g.dispose();
      glTexImage2D(target, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, 
          createTextureBuffer(img));
      target++;
      int err = glGetError();
      if (err != 0) System.out.println("GLERROR:" + Integer.toHexString(err));
      //else          System.out.println();
    }
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
  }
  static void loadSkyboxTextures(String path) {
    int target = GL_TEXTURE_CUBE_MAP_POSITIVE_X;
    for (String n : SKYBOX_NAMES) {
      String name = path + File.separator + n;
      BufferedImage img = AppSystem.loadImage(name);
      //System.out.print("loading img " + name + " " + img.getWidth() + "x" + img.getHeight());
      glTexImage2D(target, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, 
          createTextureBuffer(img));
      target++;
      int err = glGetError();
      if (err != 0) System.out.println("GLERROR:" + Integer.toHexString(err));
      //else          System.out.println();
    }
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
  }

  static final String[] SKYBOX_NAMES = {
      "posx.jpg", "negx.jpg", "posy.jpg", "negy.jpg", "posz.jpg", "negz.jpg"
  };

  List<RenderSpec> deadRenderSpecs = new ArrayList<RenderSpec>();
  public void registerForCleaning(RenderSpec renderSpec) {
    synchronized (deadRenderSpecs) {
      if (!deadRenderSpecs.contains(renderSpec)) deadRenderSpecs.add(renderSpec);
    }
  }
  
  static class GLError extends Error {
    public GLError(int err) {super("GL error 0x" + Integer.toHexString(err)); }
  }
}
