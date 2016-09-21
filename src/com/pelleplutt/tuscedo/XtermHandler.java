package com.pelleplutt.tuscedo;

public interface XtermHandler {
  void setTextFgColor(int palette);

  void setTextBgColor(int palette);

  void setTextBold(boolean b);

  void setTextDefault();

  void setTextInverse(boolean b);

  void setCursorPosition(int col, int row);

  void setCursorRow(int row);
  
  void eraseDisplayBelow();
  void eraseDisplayAbove();
  void eraseDisplayAll();
  void eraseDisplaySavedLines();

  void cursorRow(int d);
  void cursorCol(int d);

}
