package com.pelleplutt.tuscedo;

public interface XtermTerminalIfc {
  void setTextFgColor(int palette);
  void setTextBgColor(int palette);
  void setTextBold(boolean b);
  void setTextDefault();
  void setTextInverse(boolean b);

  void saveCursor();
  void restoreCursor();
  void setCursorPosition(int col, int row);
  void setCursorRow(int row);
  void setCursorCol(int col);
  void cursorRow(int relative);
  void cursorCol(int relative);
  void cursorNewline(int x);
  void cursorPrevline(int x);

  void eraseLineRight();
  void eraseLineLeft();
  void eraseLineAll();

  void eraseDisplayBelow();
  void eraseDisplayAbove();
  void eraseDisplayAll();
  void eraseDisplaySavedLines();

  void setScrollRegion(int startRow, int endRow);
  void scroll(int relative);
  
  void setAlternateScreenBuffer(boolean b);
  void delete(int x);
  
}
