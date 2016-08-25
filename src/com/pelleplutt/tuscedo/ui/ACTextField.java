package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class ACTextField extends JTextPane implements CaretListener {
  List<String> model = new ArrayList<String>();
  String userString = "";
  AttributeSet attr;
  volatile int lastSuggestionIndex = -1;
  boolean lastKnownSuggestion = false;
  static final String PROGRAMMATICALLY_TRIGGER_STRING = "@@||££HOPE_NO_OnE_EnTeR-s@this_T3xT-ultraFULT";
  static final int PROGRAMMATICALLY_TRIGGER_OFFSET = -314159265;
  
  public ACTextField() {
    setDocument(new DefaultStyledDocument());
    ((AbstractDocument)getDocument()).setDocumentFilter(filter);

    setSuggestionColor(Color.gray);
    
    addCaretListener(this);
    
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "prevsug");
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "nextsug");
    getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "acceptsug");

    ActionMap actionMap = getActionMap();
    actionMap.put("prevsug", actionPrevSuggestion);
    actionMap.put("nextsug", actionNextSuggestion);
    actionMap.put("acceptsug", actionAcceptSuggestion);

    model.add("apple");
    model.add("klipp");
    model.add("klirr");
    model.add("kloster");
    model.add("knippe");
    model.add("argantay");
    model.add("kullagulla");
    model.add("kullafjulla");
    model.add("kullaulla");
    model.add("kullalulla");
    model.add("ullaulla");
    model.add("ullalulla");
    model.add("alkis");
  }
  
  public void setSuggestions(List<String> s) {
    lastSuggestionIndex = -1;
    model = s;
  }
  
  public void setSuggestionColor(Color c) {
    final StyleContext cont = StyleContext.getDefaultStyleContext();
    attr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, c);
  }
  
  void prevSuggestion() {
    final int suggestions = model.size();
    if (suggestions == 0) return;
    if (userString == null || userString.length() == 0) {
      if (lastSuggestionIndex < 0) {
        lastSuggestionIndex = suggestions-1; 
      } else {
        lastSuggestionIndex = (suggestions + lastSuggestionIndex - 1) % suggestions;
      }
      //TODO trigger update
      try {
        getDocument().insertString(0, PROGRAMMATICALLY_TRIGGER_STRING, null);
      } catch (BadLocationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    } else {
      if (lastSuggestionIndex < 0) return;
      for (int i = 0; i < suggestions; i++) {
        int index = (i + lastSuggestionIndex + 1) % suggestions;
        String s = model.get(index);
        if (s.startsWith(userString)) {
          lastSuggestionIndex = index;
          break;
        }
      }
      setText(userString);
    }
  }
  
  void nextSuggestion() {
    final int suggestions = model.size();
    if (userString == null || userString.length() == 0) {
      if (lastSuggestionIndex < 0) {
        lastSuggestionIndex = 0; 
      } else {
        lastSuggestionIndex = (lastSuggestionIndex + 1) % suggestions;
      }
      // TODO trigger update
      try {
        getDocument().insertString(0, PROGRAMMATICALLY_TRIGGER_STRING, null);
      } catch (BadLocationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      if (lastSuggestionIndex < 0) return;
      for (int i = 0; i < suggestions; i++) {
        int index = (2*suggestions + lastSuggestionIndex - (i + 1)) % suggestions;
        String s = model.get(index);
        if (s.startsWith(userString)) {
          lastSuggestionIndex = index;
          break;
        }
      }
      setText(userString);
    }
  }
  
  static int strcmp(final String a, final String b) {
    int i = 0;
    final int alen = a.length();
    final int blen = b.length();
    while (i < alen && i < blen &&a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }
  
  public void acceptSuggestionWholly() {
    if (lastSuggestionIndex < 0) return;
    String s = super.getText();
    setText(s);
  }
  
  public void acceptSuggestion() {
    if (lastSuggestionIndex < 0) return;
    // find smallest common offset of string amongst model suggestions
    final int suggestions = model.size();
    final String ref = userString;
    String curSug = null;
    int minOffs = super.getText().length();
    for (int i = 0; i < suggestions; i++) {
      String sug = model.get(i);
      if (!sug.startsWith(ref)) continue;
      if (curSug == null) {
        curSug = sug; 
      } else {
        // find common string
        minOffs = Math.min(strcmp(sug, curSug), minOffs);
        curSug = sug;
      }
      
    }
    //String s = super.getText();
    String s = curSug.substring(0, minOffs);
    setText(s);
    lastKnownSuggestion = true;
  }
  
  public void accept() {
    if (lastKnownSuggestion) {
      acceptSuggestionWholly();
      lastKnownSuggestion = false;
    } else {
      acceptSuggestion();
    }
  }
  
  void handleUpdate(FilterBypass fb, int offset) throws BadLocationException {
    lastKnownSuggestion = false;
    fb.remove(0, fb.getDocument().getLength());
    fb.insertString(0, userString, null);
    boolean progTrig = offset == PROGRAMMATICALLY_TRIGGER_OFFSET;
    if (progTrig) {
      offset = 0;
    }
    if (progTrig || (userString != null && userString.length() > 0)) {
      String suggestion = null;
      
      if (lastSuggestionIndex >= 0 && lastSuggestionIndex < model.size()) {
        String lastSuggestion = model.get(lastSuggestionIndex);
        if (lastSuggestion.startsWith(userString)) {
          suggestion = lastSuggestion;
        }
      }
      
      if (suggestion == null) {
        for (int i = 0; i < model.size(); i++) {
          String s = model.get(i);
          if (s.startsWith(userString)) {
            lastSuggestionIndex = i;
            suggestion = s;
            break;
          }
        }
      }
      
      if (suggestion != null) {
        fb.insertString(userString.length(), suggestion.substring(userString.length()), attr);
      } else {
        lastSuggestionIndex = -1;
      }
    }
    setCaretPosition(offset);
  }
  
  @Override
  public String getText() {
    if (userString == null) userString = "";
    return userString;
  }
  
  @Override
  public void setText(String s) {
    super.setText(s);
    userString = s;
  }
  
  DocumentFilter filter = new DocumentFilter() {
    @Override
    public void remove(FilterBypass fb, int offset, int length)
        throws BadLocationException {
      if (userString == null) userString = "";
      int usLen = userString.length();
      if (offset >= usLen) return;
      length = Math.min(usLen-offset, length);
      if (length > 0) {
        userString = userString.substring(0, offset) + userString.substring(offset + length);
        handleUpdate(fb, offset);
      }
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string,
        AttributeSet attr) throws BadLocationException {
      if (userString == null) userString = "";
      string = string.replaceAll("\\r?\\n", "");
      if (string.equals(PROGRAMMATICALLY_TRIGGER_STRING)) {
        handleUpdate(fb, PROGRAMMATICALLY_TRIGGER_OFFSET);
      } else {
        userString = userString.substring(0, offset) + string + userString.substring(offset);
        handleUpdate(fb, offset + string.length());
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text,
        AttributeSet attrs) throws BadLocationException {
      if (userString == null) userString = "";
      text = text.replaceAll("\\r?\\n", "");
      int usLen = userString.length();
      String refString = userString;
      if (offset >= usLen) refString = ACTextField.super.getText();
      userString = refString.substring(0, offset) +
          text + 
          ((offset + length >= usLen) ? "" : refString.substring(offset+length));
      handleUpdate(fb, offset + text.length());
    }
  };
  
  AbstractAction actionPrevSuggestion = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      prevSuggestion();
    }
  };
  
  AbstractAction actionNextSuggestion = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      nextSuggestion();
    }
  };
  
  AbstractAction actionAcceptSuggestion = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      accept();
    }
  };
  
  @Override
  public void caretUpdate(CaretEvent e) {
    int l = userString == null ? 0 : userString.length();
    if (getCaretPosition() > l) {
      setCaretPosition(l);
    }
  }
}