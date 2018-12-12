package com.pelleplutt.tuscedo.ui;

import java.lang.Math;
import java.util.*;

import org.joml.*;

import com.pelleplutt.tuscedo.*;

public class RenderSpec implements UIO {
  public static final int PRIMITIVE_SOLID = 0;
  public static final int PRIMITIVE_WIREFRAME = 1;
  public static final int PRIMITIVE_DOTS = 2;
  
  public static final int MODEL_HEIGHTMAP = 0;        // float[w][d]=h
  public static final int MODEL_HEIGHTMAP_COLOR = 1;  // float[w][d]=[h,r,g,b]
  public static final int MODEL_POINTCLOUD = 2;       // float[w][d][h]=v
  public static final int MODEL_POINTCLOUD_COLOR = 3; // float[w][d][h]=[v,r,g,b]
  
  boolean depthTest;
  boolean cullFaces;
  int primitive;
  public int width = 400;
  public int height = 300;
  public boolean dimensionDirty;

  final Vector3f lightPos = new Vector3f();
  final Vector3f playerPos = new Vector3f();
  final Quaternionf qdir = new Quaternionf();

  private final AxisAngle4f aayaw = new AxisAngle4f();
  private final AxisAngle4f aapitch = new AxisAngle4f();
  private final AxisAngle4f aaroll = new AxisAngle4f();
  private final Quaternionf qyaw = new Quaternionf();
  private final Quaternionf qpitch = new Quaternionf();
  private final Quaternionf qroll = new Quaternionf();
  private final Matrix3f qrotm = new Matrix3f();
  final Vector3f vdirx = new Vector3f(1,0,0);
  final Vector3f vdiry = new Vector3f(0,1,0);
  final Vector3f vdirz = new Vector3f(0,0,1);
  static final Vector3f VUP = new Vector3f(0,1,0);
  static final Vector3f VDOWN = new Vector3f(0,-1,0);
  
  final Matrix4f modelMatrix = new Matrix4f();

  public int modeltype;
  public Object model;
  
  public boolean modelDataDirty;
  public boolean modelDirty;
  
  public float gridMul = 1;
  public float gridContrast = 0.25f;
  
  public int smoothOrFlat = 1;
  public float isolevel;
  public boolean faceted;
  public boolean disableShadows;
  public boolean checkered;

  int vao_sculptureGL, vbo_sculptureGL, vbo_sculptureArrIxGL;
  int numSculptureVertices;
  int numSculptureNormals;
  int numSculptureIndices;
  
  List<Marker> markers = new ArrayList<Marker>(); 

  private static int __id = 1;
  public final int id = __id++;
  
  private volatile int __iter = 0;

  public void cameraUpdate(float dx, float dy, float droll)  {
    if (dx != 0 || dy != 0 || droll != 0) {
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
    
    if (droll != 0) return; // no autolevel when rolling
    
    // auto leveling
    float s = (float)Math.atan2(vdirx.y, vdirx.x);
    float adj = 0;
    if (s > 0 && s < Math.PI) adj = -s;              //  0..90    -> 0
    else if  (s > Math.PI) adj = (float)Math.PI-s;   //  91..180  -> 180
    else if (s < 0 && s > -Math.PI) adj = -s;        // -90..0    -> 0
    else if  (s < -Math.PI) adj = (float)-Math.PI-s; // -180..-91 -> -180
    
    //if (Math.abs(adj) < Math.PI/64f || Math.abs(adj) > 63f*Math.PI/64f) return;
    float adjFact = Math.max(0, Math.abs(vdiry.y)*2f - 1f);
    aaroll.set(adj*0.0013f*adjFact, 0,0,1); // 0.0013 derived heuristically
    qroll.set(aaroll);
    qdir.mul(qroll);
    qdir.get(qrotm);
    qrotm.getColumn(0, vdirx);
    qrotm.getColumn(1, vdiry);
    qrotm.getColumn(2, vdirz);
  }

  private final Vector3f vmove = new Vector3f();
  public void cameraWalk(float step) {
    vmove.set(vdirz).mul(step);
    playerPos.sub(vmove);
  }
  
  public void cameraStrafe(float step) {
    vmove.set(vdirx).mul(step);
    playerPos.add(vmove);
  }

  public void cameraDescend(float step) {
    vmove.set(vdiry).mul(step);
    playerPos.add(vmove);
  }
  
  boolean glfinalized = false;
  public void glfinalize() {
    if (!glfinalized) {
      glfinalized = true;
      Tuscedo.scene3d.registerForCleaning(this);
    }
  }

  final UIInfo uiinfo;
  public RenderSpec() {
    uiinfo = new UIInfo(this, "3drenderspec" + __id, "renderspec" + __id);
    UIInfo.fireEventOnCreated(uiinfo);
  }

  @Override
  public UIInfo getUIInfo() {
    return uiinfo;
  }

  @Override
  public void repaint() {
  }

  @Override
  public void decorateUI() {
  }

  @Override
  public void onClose() {
  }
  
  public void modelRotate(float a, float x, float y, float z) {
    modelMatrix.rotate(a, x, y, z);
    for (RenderSpec rs : children) {
      rs.modelMatrix.rotate(a, x, y, z);
    }
  }
  
  List<RenderSpec> children = new ArrayList<RenderSpec>();
  RenderSpec next() {
    if (__iter >= children.size()) {
      return null;
    }
    int i = __iter;
    __iter++;
    return children.get(i);
  }
  RenderSpec first() {
    __iter = 0;
    return this;
  }

  public Matrix4f getModelMatrix() {
    return modelMatrix;
  }
  
  public Marker addMarker(float x, float y, float z, float scale, float r, float g, float b) {
    Marker m = new Marker(x,y,z,scale,r,g,b);
    markers.add(m);
    return m;
  }
  
  public void removeMarker(Marker m) {
    markers.remove(m);
  }
  
  public static class Marker {
    float scale = 1f;
    Vector3f pos = new Vector3f();
    Vector4f color = new Vector4f(1,1,0,1);
    public Marker(float x, float y, float z, float scale, float r, float g, float b) {
      pos.x = x; pos.y = y; pos.z = z;
      this.scale = scale;
      color.x = r;
      color.y = g;
      color.z = b;
    }
    public void setPos(float x, float y, float z) {
      pos.x = x; pos.y = y; pos.z = z;
    }
    public void setColor(float r, float g, float b) {
      color.x = r; color.y = g; color.z = b;
    }
    public void setScale(float s) {
      scale = s;
    }
    public float x() {
      return pos.x;
    }
    public float y() {
      return pos.y;
    }
    public float z() {
      return pos.z;
    }
    public float r() {
      return color.x;
    }
    public float g() {
      return color.y;
    }
    public float b() {
      return color.z;
    }
    public float scale() {
      return scale;
    }
  }
  
  public static class Vector extends Marker {
    Vector3f dir = new Vector3f();
    public Vector(float x, float y, float z, float dx, float dy, float dz, float scale, float r, float g, float b) {
      super(x,y,z,scale,r,g,b);
      dir.x = dx;
      dir.y = dy;
      dir.z = dz;
    }
    public void setPos(float x, float y, float z) {
      dir.x = x; dir.y = y; dir.z = z;
    }
    public float dx() {
      return dir.x;
    }
    public float dy() {
      return dir.y;
    }
    public float dz() {
      return dir.z;
    }
  }
}
