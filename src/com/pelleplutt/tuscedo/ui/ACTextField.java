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
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class ACTextField extends JTextPane implements CaretListener {
  /** Suggestion model */
  List<String> model = new ArrayList<String>();
  /** Currently user input, without suggestion */
  String userString = "";
  /** If user input is suggested from empty string */
  boolean userStringFullySuggested;
  /** Current suggestion model index */
  volatile int lastSuggestionIndex = -1;
  /** User has selected a suggestion */
  boolean lastKnownSuggestion = false;
  /** Suggestion listener */
  SuggestionListener suggestionListener;
  /** If user must enter strings comprised in model only */
  boolean forced;
  /** Attribute set for suggetions */
  AttributeSet suggestAttr;
  /** ID offset for triggering a document change without changing document */
  static final int PROGRAMMATICALLY_TRIGGER_OFFSET = -314159265;
  /** ID string for triggering a document change without changing document */
  static final String PROGRAMMATICALLY_TRIGGER_STRING = "@@||££HOPE_NO_OnE_EnTeR-s@this_T3xT-ultraFULT";
  
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
  }
  
  /**
   * If enabled, this will only let user enter text that is comprised by the
   * suggestion model.
   * @param forced
   */
  public void setForcedModel(boolean forced) {
    this.forced = forced;
  }
  
  /**
   * Returns if input is forced to model.
   * @return Wether forced or not
   */
  public boolean isForcedModel() {
    return this.forced;
  }
  
  /**
   * Sets the suggestion model.
   * @param s
   */
  public void setSuggestions(List<String> s) {
    lastSuggestionIndex = -1;
    model = s;
  }
  
  public void addSuggestion(String text) {
    if (model != null) {
      if (model.size() > 0 && model.get(model.size()-1).equals(text)) {
        return;
      }
      model.add(text);
    }
    lastSuggestionIndex = -1;
  }

  /**
   * Sets colour of suggested text.
   * @param c
   */
  public void setSuggestionColor(Color c) {
    final StyleContext cont = StyleContext.getDefaultStyleContext();
    suggestAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, c);
  }
  
  /**
   * Makes ACTextField forget where last suggestion was in model.
   */
  public void resetLastSuggestionIndex() {
    lastSuggestionIndex = -1;
  }
  
  void prevSuggestion() {
    final int suggestions = model.size();
    if (suggestions == 0) return;
    if (userString == null || userString.length() == 0 || userStringFullySuggested) {
      userString = "";
      if (lastSuggestionIndex < 0) {
        lastSuggestionIndex = suggestions-1; 
      } else {
        lastSuggestionIndex = (suggestions + lastSuggestionIndex - 1) % suggestions;
      }

      try {
        getDocument().insertString(0, PROGRAMMATICALLY_TRIGGER_STRING, null);
      } catch (BadLocationException e) {}

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
    lastKnownSuggestion = true;
  }
  
  void nextSuggestion() {
    final int suggestions = model.size();
    if (suggestions == 0) return;
    if (userString == null || userString.length() == 0 || userStringFullySuggested) {
      userString = "";
      if (lastSuggestionIndex < 0) {
        lastSuggestionIndex = 0; 
      } else {
        lastSuggestionIndex = (lastSuggestionIndex + 1) % suggestions;
      }

      try {
        getDocument().insertString(0, PROGRAMMATICALLY_TRIGGER_STRING, null);
      } catch (BadLocationException e) {}
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
    lastKnownSuggestion = true;
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
  
  /**
   * Accepts current suggestion wholly.
   */
  public void acceptSuggestionWholly() {
    if (lastSuggestionIndex < 0) return;
    String s = super.getText();
    setText(s);
  }
  
  /**
   * Takes current input and accepts the least common prefix of suggestions.
   * @return list of possible suggestions
   */
  public List<String> acceptSuggestion() {
    if (lastSuggestionIndex < 0) return null;
    // find smallest common offset of string amongst model suggestions
    final int suggestions = model.size();
    final String ref = userString;
    String curSug = null;
    int minOffs = super.getText().length();
    List<String> found = new ArrayList<String>();
    for (int i = 0; i < suggestions; i++) {
      String sug = model.get(i);
      if (!sug.startsWith(ref)) continue;
      found.add(curSug);
      if (curSug == null) {
        curSug = sug; 
      } else {
        // find common string
        minOffs = Math.min(strcmp(sug, curSug), minOffs);
        curSug = sug;
      }
    }

    String s = curSug.substring(0, minOffs);
    setText(s);
    lastKnownSuggestion = true;
    return found;
  }
  
  /**
   * Accepts current suggestion. Accepts wholly if previous suggestion was accepted.
   */
  public void accept() {
    if (lastKnownSuggestion) {
      acceptSuggestionWholly();
      if (suggestionListener != null) suggestionListener.gotSuggestions(null);
      lastKnownSuggestion = false;
    } else {
      List<String> foundSuggestions = acceptSuggestion();
      if (suggestionListener != null) suggestionListener.gotSuggestions(foundSuggestions);
    }
  }
  
  void handleUpdate(FilterBypass fb, int offset, String newUserString) throws BadLocationException {
    lastKnownSuggestion = false;
    if (suggestionListener != null) suggestionListener.gotSuggestions(null);
    if (!forced || newUserString.length() == 0) {
      userString = newUserString;
      fb.remove(0, fb.getDocument().getLength());
      fb.insertString(0, userString, null);
    }
    boolean progTrig = offset == PROGRAMMATICALLY_TRIGGER_OFFSET;
    if (progTrig) {
      offset = 0;
    } else {
      userStringFullySuggested = false;
    }
    if (progTrig || (newUserString != null && newUserString.length() > 0)) {
      String suggestion = null;
      
      if (lastSuggestionIndex >= 0 && lastSuggestionIndex < model.size()) {
        String lastSuggestion = model.get(lastSuggestionIndex);
        if (lastSuggestion.startsWith(newUserString)) {
          suggestion = lastSuggestion;
        }
      }
      
      if (suggestion == null) {
        for (int i = 0; i < model.size(); i++) {
          String s = model.get(i);
          if (s.startsWith(newUserString)) {
            lastSuggestionIndex = i;
            suggestion = s;
            break;
          }
        }
      }
      
      if (suggestion != null) {
        if (forced) {
          userString = newUserString;
          fb.remove(0, fb.getDocument().getLength());
          fb.insertString(0, userString, null);
        }
        if (progTrig) {
          fb.remove(0, fb.getDocument().getLength());
          fb.insertString(0, suggestion, null);
          offset = suggestion.length();
          userString = suggestion;
          userStringFullySuggested = true;
        } else {
          fb.insertString(userString.length(), suggestion.substring(userString.length()), suggestAttr);
        }
      } else {
        lastSuggestionIndex = -1;
      }
    }
    setCaretPosition(Math.min(super.getText().length(), offset));
  }
  
  @Override
  public String getText() {
    if (forced) {
      return super.getText();
    } else {
      if (userString == null) userString = "";
      return userString;
    }
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
        String newUserString = userString.substring(0, offset) + userString.substring(offset + length);
        handleUpdate(fb, offset, newUserString);
      }
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string,
        AttributeSet attr) throws BadLocationException {
      if (userString == null) userString = "";
      string = string.replaceAll("\\r?\\n", "");
      if (string.equals(PROGRAMMATICALLY_TRIGGER_STRING)) {
        handleUpdate(fb, PROGRAMMATICALLY_TRIGGER_OFFSET, userString);
      } else {
        String newUserString = userString.substring(0, offset) + string + userString.substring(offset);
        handleUpdate(fb, offset + string.length(), newUserString);
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
      String newUserString = refString.substring(0, offset) +
          text + 
          ((offset + length >= usLen) ? "" : refString.substring(offset+length));
      handleUpdate(fb, offset + text.length(), newUserString);
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
  
  /**
   * Sets a suggestion listener. This listener will be called with list of suggestions, or with
   * null if there are no suggestions.
   * @param sl
   */
  public void setSuggestionListener(SuggestionListener sl) {
    this.suggestionListener = sl;
  }
  
  public static interface SuggestionListener {
    public void gotSuggestions(List<String> suggestions);
  }
}