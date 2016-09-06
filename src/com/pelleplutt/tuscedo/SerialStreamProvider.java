package com.pelleplutt.tuscedo;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerialStreamProvider {
  InputStream getSerialInputStream();
  OutputStream getSerialOutputStream();
}
