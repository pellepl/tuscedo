package com.pelleplutt;

public interface Essential {
  /** App name */
  String name = "TUSCEDO";
  String longname = "The Ultimate Serial Console for Engineers, Developers, and Others";
  /** Version major */
  int vMaj = 0;
  /** Version minor */
  int vMin = 0;
  /** Version micro */
  int vMic = 0;
  /** Path for app data */
  String userSettingPath = ".tuscedo";
  /** Name of history file */
  String historyFile = "history";
  /** Name of history file */
  String settingsFile = "settings";
  
  /** Logging */
  static final boolean LOG_C = true;
  static final boolean LOG_CLASS = true;
  static final boolean LOG_METHOD = true;
  static final boolean LOG_LINE = true;
  static final String LOG_SETTING_FILE_NAME = ".tuscedo-log.cfg";
}
