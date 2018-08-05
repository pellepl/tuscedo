package com.pelleplutt.tuscedo.ui;

import org.joml.*;

public class RenderSpec {
  public static final int PRIMITIVE_SOLID = 0;
  public static final int PRIMITIVE_WIREFRAME = 1;
  public static final int PRIMITIVE_DOTS = 2;
  boolean depthTest;
  boolean cullFaces;
  int primitive;

  final Vector3f playerPos = new Vector3f();
  final Quaternionf qdir = new Quaternionf();

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
  
  final Matrix4f modelMatrix = new Matrix4f();

  public Object model; // float[][] or float[][][]
  
  public boolean modelDataDirty;
  public boolean modelDirty;
  
  private static int __id = 1;
  public final int id = __id++;

  public void cameraUpdate(float dx, float dy, float droll)  {
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

  final Vector3f vmove = new Vector3f();
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
