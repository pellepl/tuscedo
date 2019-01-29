package com.pelleplutt.tuscedo.math;

/**
 *
 * @author Krusty
 */
public class Emd {
  private void emdSetup(EmdData emd, int order, int iterations, int locality) {
    emd.iterations = iterations;
    emd.order = order;
    emd.locality = locality;
    emd.size = 0;
    emd.imfs = null;
    emd.residue = null;
    emd.minPoints = null;
    emd.maxPoints = null;
    emd.min = null;
    emd.max = null;
  }

  private void emdResize(EmdData emd, int size) {
    int i;
    // emdClear(emd);

    emd.size = size;
    emd.imfs = new float[emd.order][]; // cnew(float*, emd->order);
    for (i = 0; i < emd.order; i++)
      emd.imfs[i] = new float[size]; // cnew(float, size);
    emd.residue = new float[size]; // cnew(float, size);
    emd.minPoints = new int[size / 2]; // cnew(int, size / 2);
    emd.maxPoints = new int[size / 2]; // cnew(int, size / 2);
    emd.min = new float[size]; // cnew(float, size);
    emd.max = new float[size]; // cnew(float, size);
  }

  private void emdCreate(EmdData emd, int size, int order, int iterations, int locality) {
    emdSetup(emd, order, iterations, locality);
    emdResize(emd, size);
  }

  private void emdDecompose(EmdData emd, float[] signal) {
    int i, j;
    System.arraycopy(signal, 0, emd.imfs[0], 0, emd.size); // memcpy(emd->imfs[0], signal, emd->size * sizeof(float));
    System.arraycopy(signal, 0, emd.residue, 0, emd.size); // memcpy(emd->residue, signal, emd->size * sizeof(float));

    for (i = 0; i < emd.order - 1; i++) {
      float[] curImf = emd.imfs[i]; // float* curImf = emd->imfs[i];
      for (j = 0; j < emd.iterations; j++) {
        emdMakeExtrema(emd, curImf);

        if (emd.minSize < 4 || emd.maxSize < 4)
          break; // can't fit splines

        emdInterpolate(emd, curImf, emd.min, emd.minPoints, emd.minSize);
        emdInterpolate(emd, curImf, emd.max, emd.maxPoints, emd.maxSize);
        emdUpdateImf(emd, curImf);
      }

      emdMakeResidue(emd, curImf);
      System.arraycopy(emd.residue, 0, emd.imfs[i + 1], 0, emd.size); // memcpy(emd->imfs[i + 1], emd->residue,
                                                                      // emd->size * sizeof(float));
    }
  }

  // Currently, extrema within (locality) of the boundaries are not allowed.
  // A better algorithm might be to collect all the extrema, and then assume
  // that extrema near the boundaries are valid, working toward the center.

  private void emdMakeExtrema(EmdData emd, float[] curImf) {
    int i, lastMin = 0, lastMax = 0;
    emd.minSize = 0;
    emd.maxSize = 0;

    for (i = 1; i < emd.size - 1; i++) {
      if (curImf[i - 1] < curImf[i]) {
        if (curImf[i] > curImf[i + 1] && (i - lastMax) > emd.locality) {
          emd.maxPoints[emd.maxSize++] = i;
          lastMax = i;
        }
      } else {
        if (curImf[i] < curImf[i + 1] && (i - lastMin) > emd.locality) {
          emd.minPoints[emd.minSize++] = i;
          lastMin = i;
        }
      }
    }
  }

  private void emdInterpolate(EmdData emd, float[] in, float[] out, int[] points, int pointsSize) {
    int size = emd.size;
    int i, j, i0, i1, i2, i3, start, end;
    float a0, a1, a2, a3;
    float y0, y1, y2, y3, muScale, mu;
    for (i = -1; i < pointsSize; i++) {
      i0 = points[mirrorIndex(i - 1, pointsSize)];
      i1 = points[mirrorIndex(i, pointsSize)];
      i2 = points[mirrorIndex(i + 1, pointsSize)];
      i3 = points[mirrorIndex(i + 2, pointsSize)];

      y0 = in[i0];
      y1 = in[i1];
      y2 = in[i2];
      y3 = in[i3];

      a0 = y3 - y2 - y0 + y1;
      a1 = y0 - y1 - a0;
      a2 = y2 - y0;
      a3 = y1;

      // left boundary
      if (i == -1) {
        start = 0;
        i1 = -i1;
      } else
        start = i1;

      // right boundary
      if (i == pointsSize - 1) {
        end = size;
        i2 = size + size - i2;
      } else
        end = i2;

      muScale = 1.f / (i2 - i1);
      for (j = start; j < end; j++) {
        mu = (j - i1) * muScale;
        out[j] = ((a0 * mu + a1) * mu + a2) * mu + a3;
      }
    }
  }

  private void emdUpdateImf(EmdData emd, float[] imf) {
    int i;
    for (i = 0; i < emd.size; i++)
      imf[i] -= (emd.min[i] + emd.max[i]) * .5f;
  }

  private void emdMakeResidue(EmdData emd, float[] cur) {
    int i;
    for (i = 0; i < emd.size; i++)
      emd.residue[i] -= cur[i];
  }

  private int mirrorIndex(int i, int size) {
    if (i < size) {
      if (i < 0)
        return -i - 1;
      return i;
    }

    return (size - 1) + (size - i);
  }

  /**
  * This code implements empirical mode decomposition. Required paramters
  * include
  * @param order the number of IMFs to return
  * @parma iterations the number of iterations per IMF
  * @param locality in samples, the nearest two extrema may be, if it is not specified, there is no limit (locality = 0).
  */
  public static EmdData decompose(float[] data, int order, int iterations, int locality) {
    Emd emd = new Emd();
    EmdData emdData = new EmdData();
    emd.emdCreate(emdData, data.length, order, iterations, locality);
    emd.emdDecompose(emdData, data);
    return emdData;
  }

  public static class EmdData {
    public int iterations, order, locality;
    public int[] minPoints, maxPoints;
    public float[] min, max, residue;
    public float[][] imfs;
    public int size, minSize, maxSize;
  }
}