package com.pelleplutt.tuscedo.ui;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

public abstract class Modeller3D {
  public List<Vector3f> vertices = new ArrayList<Vector3f>();
  public List<Vector3f> normals = new ArrayList<Vector3f>();
  public List<Float> colors = new ArrayList<Float>();
  public List<Integer> indices= new ArrayList<Integer>();
  public abstract void build();
  
  public static class Grid extends Modeller3D {
    int w, h;
    float[][] map;
    float[][][] mapc;
    public Grid(float[][] map) {
      this.w = map.length-1;
      this.h = map[0].length-1;
      this.map = map;
    }
    public Grid(float[][][] map) {
      this.w = map.length-1;
      this.h = map[0].length-1;
      this.mapc = map;
    }
    
    public void build() {
      float offsX = -(float)w / 2;
      float offsZ = -(float)h / 2;
      for (int z = 0; z < h; z++) {
        for (int x = 0; x < w; x++) {
          float h00, h01, h10;
          h00 = map == null ? mapc[x][z][0] : map[x][z]; 
          h01 = map == null ? mapc[x][z+1][0] : map[x][z+1]; 
          h10 = map == null ? mapc[x+1][z][0] : map[x+1][z]; 
          vertices.add(new Vector3f(x+offsX, h00, z+offsZ));
          
          Vector3f nx = new Vector3f(1f, h10-h00, 0f);
          Vector3f nz = new Vector3f(0f, h01-h00, 1f);
          nx.normalize();
          nz.normalize();
          normals.add(nx.cross(nz));
          
          if (map == null) {
            colors.add(mapc[x][z][1]);
          }
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
  
  public static class Sphere extends Modeller3D {
    float r;
    int div;

    public Sphere(float radius, int div) {
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
  
  private static void vec3ffill(Vector3f v[]) {
    for (int i = 0; i < v.length; i++) v[i] = new Vector3f();
  }
  private static void vec3ffill(Vector3f v[][]) {
    for (int i = 0; i < v.length; i++) {
      for (int j = 0; j < v[i].length; j++) {
        v[i][j] = new Vector3f();
      }
    }
  }
  
  public static class Cloud extends Modeller3D {
    // courtesy of http://paulbourke.net/geometry/polygonise/
    float[][][] data;
    float isolevel;
    private Vector3f vertList[] = new Vector3f[4*3];
    private Vector3f triangles[][] = new Vector3f[16][3];
    private Vector3f onormals[][] = new Vector3f[16][3];
    private Vector3f cubeVerts[] = new Vector3f[8];
    private float cubeVals[] = new float[8+12];
    private Vector3f norms[] = new Vector3f[8];
    private Vector3f normList[] = new Vector3f[4*3];
    
    public Cloud(float[][][] cloud, float isolevel) {
      this.data = cloud;
      this.isolevel = isolevel;
      vec3ffill(vertList);
      vec3ffill(triangles);
      vec3ffill(onormals);
      vec3ffill(cubeVerts);
      vec3ffill(norms);
      vec3ffill(normList);
    }
    
    public void build() {
      final int width = data.length-1;
      final int height = data[0].length-1;
      final int depth = data[0][0].length-1;
      VertexLUT lut[] = new VertexLUT[width * height];
      for (int i = 0; i < lut.length; i++) lut[i] = new VertexLUT();
      VertexLUT tlut = new VertexLUT();
      Vector3f n = new Vector3f();
      Vector3f t = new Vector3f();
      VertexLUT lutx, luty, lutz;
      for (int z = 0; z < depth; z++) {
        int zz = z - depth/2;
        for (int y = 0; y < height; y++) {
          int yy = y - height/2;
          for (int x = 0; x < width; x++) {
            int xx = x - width/2;
            cubeVerts[0].set(xx  , yy  , zz  ); cubeVals[0] = data[x  ][y  ][z  ];
            cubeVerts[1].set(xx+1, yy  , zz  ); cubeVals[1] = data[x+1][y  ][z  ];
            cubeVerts[2].set(xx+1, yy  , zz+1); cubeVals[2] = data[x+1][y  ][z+1];
            cubeVerts[3].set(xx  , yy  , zz+1); cubeVals[3] = data[x  ][y  ][z+1];
            cubeVerts[4].set(xx  , yy+1, zz  ); cubeVals[4] = data[x  ][y+1][z  ];
            cubeVerts[5].set(xx+1, yy+1, zz  ); cubeVals[5] = data[x+1][y+1][z  ];
            cubeVerts[6].set(xx+1, yy+1, zz+1); cubeVals[6] = data[x+1][y+1][z+1];
            cubeVerts[7].set(xx  , yy+1, zz+1); cubeVals[7] = data[x  ][y+1][z+1];
            cubeVals[8]  = x < width-1  ? data[x+2][y  ][z  ] : 0;
            cubeVals[9]  = x < width-1  ? data[x+2][y+1][z  ] : 0;
            cubeVals[10] = x < width-1  ? data[x+2][y+1][z+1] : 0;
            cubeVals[11] = x < width-1  ? data[x+2][y  ][z+1] : 0;
            cubeVals[12] = y < height-1 ? data[x  ][y+2][z  ] : 0;
            cubeVals[13] = y < height-1 ? data[x+1][y+2][z  ] : 0;
            cubeVals[14] = y < height-1 ? data[x+1][y+2][z+1] : 0;
            cubeVals[15] = y < height-1 ? data[x  ][y+2][z+1] : 0;
            cubeVals[16] = z < depth-1  ? data[x  ][y  ][z+2] : 0;
            cubeVals[17] = z < depth-1  ? data[x+1][y  ][z+2] : 0;
            cubeVals[18] = z < depth-1  ? data[x+1][y+1][z+2] : 0;
            cubeVals[19] = z < depth-1  ? data[x  ][y+1][z+2] : 0;
            //  n(0)  f(1) - f(0), f(4) - f(0), f(3) - f(0)
            //  n(1)  f(8) - f(1), f(2) - f(1), f(5) - f(1)
            //  n(2)  f(9) - f(2), f(13)- f(2), f(6) - f(2)
            //  n(3)  f(2) - f(3), f(12)- f(3), f(7) - f(3)
            //  n(4)  f(5) - f(4), f(7) - f(4), f(16)- f(4)
            //  n(5)  f(11)- f(5), f(6) - f(5), f(17)- f(5)
            //  n(6)  f(10)- f(6), f(14)- f(6), f(18)- f(6)
            //  n(7)  f(6) - f(7), f(15)- f(7), f(19)- f(7)
            norms[0].set(cubeVals[1] -cubeVals[0], cubeVals[4] -cubeVals[0], cubeVals[3] -cubeVals[0]);
            norms[1].set(cubeVals[8] -cubeVals[1], cubeVals[5] -cubeVals[1], cubeVals[2] -cubeVals[1]);
            norms[2].set(cubeVals[9] -cubeVals[2], cubeVals[13]-cubeVals[2], cubeVals[6] -cubeVals[2]);
            norms[3].set(cubeVals[2] -cubeVals[3], cubeVals[12]-cubeVals[3], cubeVals[7] -cubeVals[3]);
            norms[4].set(cubeVals[5] -cubeVals[4], cubeVals[7] -cubeVals[4], cubeVals[16]-cubeVals[4]);
            norms[5].set(cubeVals[11]-cubeVals[5], cubeVals[6] -cubeVals[5], cubeVals[17]-cubeVals[5]);
            norms[6].set(cubeVals[10]-cubeVals[6], cubeVals[14]-cubeVals[6], cubeVals[18]-cubeVals[6]);
            norms[7].set(cubeVals[6] -cubeVals[7], cubeVals[15]-cubeVals[7], cubeVals[19]-cubeVals[7]);
            
            for (int i = 0; i < 7; i++) {
              norms[i].normalize(); //set(norms[0]);
            }
            
            int triCnt = polygoniseCube(cubeVerts, cubeVals, isolevel, triangles, onormals);

            int lutix = (x + width * (y + height * z)) % lut.length;
            lutx = luty = lutz = null;
            if (x > 0) lutx = lut[(lut.length + lutix - 1) % lut.length];
            if (y > 0) luty = lutx = lut[(lut.length + lutix - width) % lut.length];
            if (z > 0) lutz = lut[(lut.length + lutix - width * height) % lut.length];
            
            tlut.localVertCnt = 0;
            for (int tri = 0; tri < triCnt; tri++) {
              // calculate triangle normal
              // Apparently the derivative of a cloud surface is the normal. Cool..!
              n.set(
                  data[x+1][y  ][z  ] - data[x][y][z],
                  data[x  ][y+1][z  ] - data[x][y][z],
                  data[x  ][y  ][z+1] - data[x][y][z]
                  );
              if (n.lengthSquared() < 0.00001) {
                // that derivative seemed a bit off, so try cross product of the triangle instead
                n.set(triangles[tri][1]).sub(triangles[tri][0]);
                t.set(triangles[tri][2]).sub(triangles[tri][0]);
                n.cross(t);
              }
              n.normalize();
              // check each triangle vertex if already defined in a previous cube (x-1, y-1 and z-1)
              for (int j = 0; j < 3; j++) {
                Vector3f triv = triangles[tri][2-j];
                int tix = findVertexInLuts(triv, lutx, luty, lutz);
                if (tix < 0) {
                  // new vertex
                  indices.add(vertices.size());
                  vertices.add(new Vector3f(triv));
                  n = new Vector3f(triv);
                  normals.add(new Vector3f(onormals[tri][2-j].normalize()));
                  // register this new vector for lut update below
                  tlut.verts[tlut.localVertCnt].set(triv);
                  tlut.localVertCnt++;
                } else {
                  // vertex coherent with a previously defined vertex
                  indices.add(tix);
                }
              }
            }
            // set the new LUT entry here at current index
            lut[lutix].globalVertIx = vertices.size() - tlut.localVertCnt;
            lut[lutix].localVertCnt = tlut.localVertCnt;
            for (int i = 0; i < tlut.localVertCnt; i++) lut[lutix].verts[i].set(tlut.verts[i]);
          }
        }
      }
    }
    
    int findVertexInLuts(Vector3f v, VertexLUT lutx, VertexLUT luty, VertexLUT lutz) {
      int r = -1;
      if (lutx != null) r = findVertexInLut(v, lutx);
      if (r >= 0) return r;
      if (luty != null) r = findVertexInLut(v, luty);
      if (r >= 0) return r;
      if (lutz != null) r = findVertexInLut(v, lutz);
      return r;
    }
    
    int findVertexInLut(Vector3f v, VertexLUT lut) {
      for (int i = 0; i < lut.localVertCnt; i++) {
        if (vecMatch(v, lut.verts[i])) {
          return i + lut.globalVertIx;
        }
      }
      return -1;
    }
    
    static final int EQ_DEC = 100;
    boolean vecMatch(Vector3f a, Vector3f b) {
      return (int)(a.x*EQ_DEC) == (int)(b.x*EQ_DEC)
          && (int)(a.y*EQ_DEC) == (int)(b.y*EQ_DEC)
          && (int)(a.z*EQ_DEC) == (int)(b.z*EQ_DEC);
    }
    
    //                 4 ___________________  5   
    //                  /|                 /|
    //                 / |                / |
    //                /  |               /  |
    //             7 /___|______________/ 6 |
    //              |    |              |   |
    //              |    |              |   |
    //              |  0 |______________|___| 1  
    //              |   /               |   /
    //              |  /                |  /
    //              | /                 | /
    //              |/__________________|/
    //             3                      2
    //
    //  0     x   y   z
    //  1     x+1 y   z
    //  2     x+1 y+1 z
    //  3     x   y+1 z
    //  4     x   y   z+1
    //  5     x+1 y   z+1
    //  6     x+1 y+1 z+1
    //  7     x   y+1 z+1
    //  n(0)  f(1)-          f(0), f(3)-          f(0), f(4)-          f(0)
    //  n(1)  f(x+2,y,z)-    f(1), f(2)-          f(1), f(5)-          f(1)
    //  n(2)  f(x+2,y+1,z)-  f(2), f(x+1,y+2,z)-  f(2), f(6)-          f(2)
    //  n(3)  f(2)-          f(3), f(x,y+2,z)-    f(3), f(7)-          f(3)
    //  n(4)  f(5)-          f(4), f(7)-          f(4), f(x,y,z+2)-    f(4)
    //  n(5)  f(x+2,y,z+1)-  f(5), f(6)-          f(5), f(x+1,y,z+2)-  f(5)
    //  n(6)  f(x+2,y+1,z+1)-f(6), f(x+1,y+2,z+1)-f(6), f(x+1,y+1,z+2)-f(6)
    //  n(7)  f(6)-          f(7), f(x,y+2,z+1)-  f(7), f(x,y+1,z+2)-  f(7)
    //  8     x+2 y   z 
    //  9     x+2 y+1 z
    //  10    x+2 y+1 z+1
    //  11    x+2 y   z+1
    //  12    x   y+2 z
    //  13    x+1 y+2 z
    //  14    x+1 y+2 z+1
    //  15    x   y+2 z+1
    //  16    x   y   z+2
    //  17    x+1 y   z+2
    //  18    x+1 y+1 z+2
    //  19    x   y+1 z+2
    //  n(0)  f(1) - f(0), f(3) - f(0), f(4) - f(0)
    //  n(1)  f(8) - f(1), f(2) - f(1), f(5) - f(1)
    //  n(2)  f(9) - f(2), f(13)- f(2), f(6) - f(2)
    //  n(3)  f(2) - f(3), f(12)- f(3), f(7) - f(3)
    //  n(4)  f(5) - f(4), f(7) - f(4), f(16)- f(4)
    //  n(5)  f(11)- f(5), f(6) - f(5), f(17)- f(5)
    //  n(6)  f(10)- f(6), f(14)- f(6), f(18)- f(6)
    //  n(7)  f(6) - f(7), f(15)- f(7), f(19)- f(7)
int polygoniseCube(Vector3f cubeVerts[], float cubeVals[], float isolevel, Vector3f triangles[][], Vector3f normals[][]) {
      int triCnt = 0;
      int cubeIx = 0;

      if (cubeVals[0] < isolevel) cubeIx |= 1;
      if (cubeVals[1] < isolevel) cubeIx |= 2;
      if (cubeVals[2] < isolevel) cubeIx |= 4;
      if (cubeVals[3] < isolevel) cubeIx |= 8;
      if (cubeVals[4] < isolevel) cubeIx |= 16;
      if (cubeVals[5] < isolevel) cubeIx |= 32;
      if (cubeVals[6] < isolevel) cubeIx |= 64;
      if (cubeVals[7] < isolevel) cubeIx |= 128;
      
      final int edge = EDGE_TABLE[cubeIx]; 
      
      // sample cube is completely within or outside
      if (edge == 0) return 0;
      
      // find the vertices where the surface intersects the cube
      if ((edge & 1) != 0) {
        vertexLerp(vertList[0],  isolevel,cubeVerts[0],cubeVerts[1],cubeVals[0],cubeVals[1]);
        vertexLerp(normList[0],  isolevel,norms[0],    norms[1],    cubeVals[0],cubeVals[1]);
      }
      if ((edge & 2) != 0) {
        vertexLerp(vertList[1],  isolevel,cubeVerts[1],cubeVerts[2],cubeVals[1],cubeVals[2]);
        vertexLerp(normList[1],  isolevel,norms[1],    norms[2],    cubeVals[1],cubeVals[2]);
      }
      if ((edge & 4) != 0) {
        vertexLerp(vertList[2],  isolevel,cubeVerts[2],cubeVerts[3],cubeVals[2],cubeVals[3]);
        vertexLerp(normList[2],  isolevel,norms[2],    norms[3],    cubeVals[2],cubeVals[3]);
      }
      if ((edge & 8) != 0) {
        vertexLerp(vertList[3],  isolevel,cubeVerts[3],cubeVerts[0],cubeVals[3],cubeVals[0]);
        vertexLerp(normList[3],  isolevel,norms[3],    norms[0],    cubeVals[3],cubeVals[0]);
      }
      if ((edge & 16) != 0) {
        vertexLerp(vertList[4],  isolevel,cubeVerts[4],cubeVerts[5],cubeVals[4],cubeVals[5]);
        vertexLerp(normList[4],  isolevel,norms[4],    norms[5],    cubeVals[4],cubeVals[5]);
      }
      if ((edge & 32) != 0) {
        vertexLerp(vertList[5],  isolevel,cubeVerts[5],cubeVerts[6],cubeVals[5],cubeVals[6]);
        vertexLerp(normList[5],  isolevel,norms[5],    norms[6],    cubeVals[5],cubeVals[6]);
      }
      if ((edge & 64) != 0) {
        vertexLerp(vertList[6],  isolevel,cubeVerts[6],cubeVerts[7],cubeVals[6],cubeVals[7]);
        vertexLerp(normList[6],  isolevel,norms[6],    norms[7],    cubeVals[6],cubeVals[7]);
      }
      if ((edge & 128) != 0) {
        vertexLerp(vertList[7],  isolevel,cubeVerts[7],cubeVerts[4],cubeVals[7],cubeVals[4]);
        vertexLerp(normList[7],  isolevel,norms[7],    norms[4],    cubeVals[7],cubeVals[4]);
      }
      if ((edge & 256) != 0) {
        vertexLerp(vertList[8],  isolevel,cubeVerts[0],cubeVerts[4],cubeVals[0],cubeVals[4]);
        vertexLerp(normList[8],  isolevel,norms[0],    norms[4],    cubeVals[0],cubeVals[4]);
      }
      if ((edge & 512) != 0) {
        vertexLerp(vertList[9],  isolevel,cubeVerts[1],cubeVerts[5],cubeVals[1],cubeVals[5]);
        vertexLerp(normList[9],  isolevel,norms[1],    norms[5],    cubeVals[1],cubeVals[5]);
      }
      if ((edge & 1024) != 0) {
        vertexLerp(vertList[10], isolevel,cubeVerts[2],cubeVerts[6],cubeVals[2],cubeVals[6]);
        vertexLerp(normList[10], isolevel,norms[2],    norms[6],    cubeVals[2],cubeVals[6]);
      }
      if ((edge & 2048) != 0) {
        vertexLerp(vertList[11], isolevel,cubeVerts[3],cubeVerts[7],cubeVals[3],cubeVals[7]);
        vertexLerp(normList[11], isolevel,norms[3],    norms[7],    cubeVals[3],cubeVals[7]);
      }
      
      // create the triangles 
      // TODO Here, we should be able to get the normals as well - from looking at vertexlerp 
      //      args we know what points each edge is computed from. 
      //      Calculate normal for each point by derivating the density data per point and lerp
      //      between these.
      //      Thus, need more cubeVals. Example one dimension:
      //      cubeVal0=x0 -- cubeVal1=x1 -- cubeVal2=x2
      //           --normal01--   |   --normal12--
      //      n = lerp(normal01, normal12, (isolevel - cubeVal0) / (cubeVal1 - cubeVal0)).normalize
      //      n = lerp(x1-x0, x2-x1, (i - x0) / (x1 - x0)).normalize
      //      n = x1-x0 + (i-x0)/(x1-x0)*(x2-x1 - x1+x0) =
      //          x1-x0 + (i-x0)/(x1-x0)*(x2+x0-2x1) 
      //          
      for (int i = 0; TRI_TABLE[cubeIx][i] != -1; i += 3) {
        triangles[triCnt][0] = vertList[TRI_TABLE[cubeIx][i  ]];
        triangles[triCnt][1] = vertList[TRI_TABLE[cubeIx][i+1]];
        triangles[triCnt][2] = vertList[TRI_TABLE[cubeIx][i+2]];
        normals[triCnt][0] = normList[TRI_TABLE[cubeIx][i  ]];
        normals[triCnt][1] = normList[TRI_TABLE[cubeIx][i+1]];
        normals[triCnt][2] = normList[TRI_TABLE[cubeIx][i+2]];
        triCnt++;
      }
      return triCnt;
    }
    
    void vertexLerp(Vector3f dst, float isolevel, Vector3f p1, Vector3f p2, float v1, float v2) {
      if (Math.abs(isolevel - v1) < 0.0001) {
        dst.set(p1); return;
      }
      if (Math.abs(isolevel - v2) < 0.0001) {
        dst.set(p2); return;
      }
      if (Math.abs(v1 - v2) < 0.0001) {
        dst.set(p1); return;
      }
      float a = (isolevel - v1) / (v2 - v1);
      dst.x = p1.x + a * (p2.x - p1.x);
      dst.y = p1.y + a * (p2.y - p1.y);
      dst.z = p1.z + a * (p2.z - p1.z);
    }
    
    class VertexLUT {
      int globalVertIx;
      int localVertCnt;
      Vector3f verts[] = new Vector3f[3*5];
      public VertexLUT() {
        vec3ffill(verts);
      }
    }
    
    static final int EDGE_TABLE[] = {
        0x0  , 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
        0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
        0x190, 0x99 , 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
        0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
        0x230, 0x339, 0x33 , 0x13a, 0x636, 0x73f, 0x435, 0x53c,
        0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
        0x3a0, 0x2a9, 0x1a3, 0xaa , 0x7a6, 0x6af, 0x5a5, 0x4ac,
        0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
        0x460, 0x569, 0x663, 0x76a, 0x66 , 0x16f, 0x265, 0x36c,
        0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
        0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff , 0x3f5, 0x2fc,
        0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
        0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55 , 0x15c,
        0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
        0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc ,
        0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
        0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
        0xcc , 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
        0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
        0x15c, 0x55 , 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
        0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
        0x2fc, 0x3f5, 0xff , 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
        0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
        0x36c, 0x265, 0x16f, 0x66 , 0x76a, 0x663, 0x569, 0x460,
        0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
        0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa , 0x1a3, 0x2a9, 0x3a0,
        0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
        0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33 , 0x339, 0x230,
        0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
        0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99 , 0x190,
        0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
        0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0   };
        static final int TRI_TABLE[][] =
        {{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1},
        {3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1},
        {3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1},
        {3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1},
        {9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1},
        {9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
        {2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1},
        {8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1},
        {9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
        {4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1},
        {3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1},
        {1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1},
        {4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1},
        {4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1},
        {9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
        {5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1},
        {2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1},
        {9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
        {0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
        {2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1},
        {10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1},
        {4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1},
        {5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1},
        {5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1},
        {9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1},
        {0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1},
        {1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1},
        {10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1},
        {8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1},
        {2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1},
        {7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1},
        {9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1},
        {2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1},
        {11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1},
        {9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1},
        {5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1},
        {11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1},
        {11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
        {1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1},
        {9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1},
        {5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1},
        {2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
        {0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
        {5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1},
        {6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1},
        {3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1},
        {6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1},
        {5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1},
        {1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
        {10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1},
        {6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1},
        {8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1},
        {7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1},
        {3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
        {5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1},
        {0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1},
        {9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1},
        {8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1},
        {5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1},
        {0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1},
        {6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1},
        {10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1},
        {10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1},
        {8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1},
        {1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1},
        {3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1},
        {0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1},
        {10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1},
        {3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1},
        {6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1},
        {9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1},
        {8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1},
        {3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1},
        {6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1},
        {0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1},
        {10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1},
        {10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1},
        {2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1},
        {7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1},
        {7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1},
        {2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1},
        {1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1},
        {11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1},
        {8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1},
        {0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1},
        {7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
        {10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
        {2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
        {6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1},
        {7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1},
        {2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1},
        {1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1},
        {10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1},
        {10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1},
        {0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1},
        {7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1},
        {6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1},
        {8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1},
        {9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1},
        {6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1},
        {4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1},
        {10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1},
        {8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1},
        {0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1},
        {1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1},
        {8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1},
        {10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1},
        {4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1},
        {10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
        {5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
        {11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1},
        {9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
        {6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1},
        {7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1},
        {3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1},
        {7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1},
        {9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1},
        {3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1},
        {6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1},
        {9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1},
        {1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1},
        {4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1},
        {7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1},
        {6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1},
        {3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1},
        {0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1},
        {6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1},
        {0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1},
        {11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1},
        {6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1},
        {5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1},
        {9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1},
        {1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1},
        {1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1},
        {10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1},
        {0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1},
        {5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1},
        {10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1},
        {11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1},
        {9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1},
        {7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1},
        {2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1},
        {8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1},
        {9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1},
        {9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1},
        {1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1},
        {9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1},
        {9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1},
        {5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1},
        {0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1},
        {10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1},
        {2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1},
        {0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1},
        {0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1},
        {9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1},
        {5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1},
        {3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1},
        {5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1},
        {8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1},
        {0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1},
        {9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1},
        {0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1},
        {1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1},
        {3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1},
        {4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1},
        {9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1},
        {11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1},
        {11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1},
        {2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1},
        {9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1},
        {3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1},
        {1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1},
        {4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1},
        {4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1},
        {0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1},
        {3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1},
        {3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1},
        {0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1},
        {9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1},
        {1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};
  }
}
