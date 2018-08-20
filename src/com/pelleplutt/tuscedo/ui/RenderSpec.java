package com.pelleplutt.tuscedo.ui;

import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderSpec {
  public static final int PRIMITIVE_SOLID = 0;
  public static final int PRIMITIVE_WIREFRAME = 1;
  public static final int PRIMITIVE_DOTS = 2;
  
  public static final int MODEL_HEIGHTMAP = 0;        // float[w][d]=h
  public static final int MODEL_HEIGHTMAP_COLOR = 1;  // float[w][d]=[h,r,g,b]
  public static final int MODEL_POINTCLOUD = 2;       // float[w][d][h]=v
  
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


  
  private static int __id = 1;
  public final int id = __id++;

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
  
}
