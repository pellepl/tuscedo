package com.pelleplutt.tuscedo.math;

public class Misc {

  public static float sumSquaredDiff(float[] d1, int offset1, float[] d2, int offset2, int length) {
    float sum = 0;
    for (int i = 0;i  < length; i++) {
      float d = d1[i + offset1] - d2[i + offset2];
      sum += d*d;
    }
    return sum;
  }
  
  public static float correlation(float[] d1, int offset1, float[] d2, int offset2, int length) {
    double sum1, sum2, sum11, sum22, sum12;
    sum1 = sum2 = sum11 = sum22 = sum12 = 0;
    for (int i = 0; i < length; i++) {
      float s1 = d1[i + offset1];
      float s2 = d2[i + offset2];
      sum1 += s1;
      sum2 += s2;
      sum11 += s1*s1;
      sum22 += s2*s2;
      sum12 += s1*s2;
    }
    double n = (double)length;
    return (float)((n * sum12 - sum1 * sum2) / (Math.sqrt(n * sum11 - sum1*sum1) * Math.sqrt(n * sum22 - sum2*sum2)));
  }

  public static void normalize(float[] src, int srcOffset, float[] dst, int dstOffset, int length) {
    float max = 0;
    for (int i = srcOffset; i < srcOffset + length; i++) {
      float a = Math.abs(src[i]);
      if (a > max) max = a;
    }
    float imax = 1.f / max;
    for (int i = 0; i < length; i++) {
      dst[i + dstOffset] = src[i + srcOffset] * imax;
    }
  }

  public static void temporalLinearRubber(float[] src, int srcOffset, int srcLength, float[] dst, int dstOffset, int dstLength) {
    double stride = (double)srcLength / (double)dstLength;
    if (stride < 1.0) {
      stride = (double)srcLength / ((double)dstLength+1);
      double near = src[srcOffset];
      double far = srcOffset < src.length-1 ? src[srcOffset + 1] : near;
      double ix = 0;
      for (int x = dstOffset; x < dstOffset + dstLength; x++) {
        double f = ix - (long)ix;
        dst[x] = (float)(f * (far - near) + near);
        ix += stride;
        if (ix >= 1.0) {
          ix -= 1.0;
          srcOffset++;
          near = far;
          if (srcOffset < src.length-1)  
            far = src[srcOffset + 1];
        }
      }
    } else if(stride > 1.0) {
      double ix = 0;
      for (int x = dstOffset; x < dstOffset + dstLength; x++) {
        double f = ix - (long)ix;
        double rem = stride - (1.0 - f);
        double sum = src[(int)ix + srcOffset] * (1.0 - f);
        ix++;
        while (rem > 1.0) {
          sum += src[(int)ix + srcOffset];
          ix++;
          rem--;
        }
        if (rem > 0 && ix < srcLength) {
          sum += src[(int)ix + srcOffset] * rem;
        } else {
          stride -= rem;
        }
        ix += rem - f;
        dst[x] = (float)(sum / stride);
      }
    } else {
      for (int i = 0; i < srcLength; i++) {
        dst[dstOffset + i] = src[srcOffset + i];
      }
    }
  }
}
