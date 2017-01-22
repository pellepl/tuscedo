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
  /** Currently user input, without suggestion */
  String userString = "";
  /** If user input is suggested from empty string */
  boolean userStringFullySuggested;
  /** Suggestion model */
  Model model;
  /** History */
  Model history;
  /** User suggestions */
  Model userModel;
  String lastUserModelQuery;
  /** User has selected a suggestion */
  boolean userSelectedSuggestion = false;
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
  /** Indicates if setText was called, set during removal */
  volatile boolean temporaryStringRemove;
  /** Indicates if setText was called, set during update */
  volatile boolean temporaryStringUpdate;
  
  volatile boolean filterNewLine;
  
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
    
    model = new Model();
    model.suggestions = new ArrayList<String>();
    model.index = -1;
    history = new Model();
    history.suggestions = new ArrayList<String>();
    history.index = -1;
    userModel = new Model();
    userModel.index = -1;
    filterNewLine = true;
  }
  
  /**
   * Disable/enable newline filtering
   * @param f
   */
  public void setFilterNewLine(boolean f) {
    filterNewLine = f;
  }
  
  /**
   * If enabled, this will only let user enter text that is comprised by the
   * suggestion model.
   * @param forced
   */
  public void setForcedModel(boolean forced) {
    this.forced = forced;
    forceSuggest();
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
    history.index = -1;
    history.suggestions = s;
    model.index = -1;
    model.suggestions = new ArrayList<String>();
    for (String t : s) {
      if (!model.suggestions.contains(t)) model.suggestions.add(t);
    }
    forceSuggest();
  }
  
  /**
   * Adds a suggestion to the suggestion model
   * @param text
   */
  public void addSuggestion(String text) {
    if (history.size() == 0 || !history.get(history.size()-1).equals(text)) {
      history.add(text);
    }
    if (!model.suggestions.contains(text)) {
      model.add(text);
    }
    history.index = -1;
    model.index = -1;
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
    model.index = -1;
    history.index = -1;
  }
  
  void stepSuggestion(boolean prevElseNext) {
    Model m = userString == null || userString.length() == 0 || userStringFullySuggested ? history : model;
    if (userModel.size() > 0) m = userModel;
    final int suggestions = m.size();

    if (suggestions == 0) return;
    
    if (userString == null || userString.length() == 0 || userStringFullySuggested) {
      userString = "";
      if (m.index < 0) {
        m.index = prevElseNext ? suggestions-1 : 0; 
      } else {
        m.index = prevElseNext ? (suggestions + m.index - 1) % suggestions : (m.index + 1) % suggestions;
      }
      try {
        getDocument().insertString(0, PROGRAMMATICALLY_TRIGGER_STRING, null);
      } catch (BadLocationException e) {}
    } else {
      for (int i = 0; i < suggestions; i++) {
        int index = prevElseNext ? 
            ((2*suggestions + m.index - (i+1)) % suggestions) : 
              ((i + m.index + 1) % suggestions);
        String s = m.get(index);
        if (s.startsWith(userString)) {
          m.index = index;
          break;
        }
      }
      setText(userString);
    }
    userSelectedSuggestion = true;
  }
  
  void prevSuggestion() {
    stepSuggestion(true);
  }
  
  void nextSuggestion() {
    stepSuggestion(false);
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
    String s = super.getText();
    setText(s);
    userSelectedSuggestion = false;
  }
  
  /**
   * Takes current input and accepts the least common prefix of suggestions.
   * @return list of possible suggestions
   */
  public List<String> acceptSuggestion() {
    final String ref = userString;
    Model m = userStringFullySuggested ? history : model;
    if (!userString.equals(lastUserModelQuery) && suggestionListener != null) {
      lastUserModelQuery = userString;
      userModel.suggestions = suggestionListener.giveSuggestions(userString);
      userModel.index = -1;
    }
    
    if (m.index < 0 && userModel.size() == 0) return null;
    if (userModel.size() > 0) m = userModel;
        
    // find smallest common offset of string amongst model suggestions
    final int suggestions = m.size();
    String curSug = null;
    int minOffs = super.getText().length();
    List<String> found = new ArrayList<String>();
    for (int i = 0; i < suggestions; i++) {
      String sug = m.get(i);
      if (!sug.startsWith(ref)) continue;
      if (curSug == null) {
        curSug = sug; 
      } else {
        // find common string
        minOffs = Math.min(strcmp(sug, curSug), minOffs);
        curSug = sug;
      }
      found.add(curSug);
    }

    if (curSug != null) {
      String s = curSug.substring(0, minOffs);
      setText(s);
    }

    return found;
  }
  
  public void forceSuggest() {
    Model m = model;
    // find smallest common offset of string amongst model suggestions
    if (m == null) return;
    final int suggestions = m.size();
    
    // check current userstring
    boolean force = true;
    if (userString != null && !userString.isEmpty()) {
      for (int i = 0; i < suggestions; i++) {
        String sug = m.get(i);
        if (sug.startsWith(userString)) {
          force = false;
          break;
        }
      }
    }
    
    if (!force) {
      return;
    }
    
    // force a valid start input
    String curSug = null;
    int minOffs = Integer.MAX_VALUE;
    for (int i = 0; i < suggestions; i++) {
      String sug = m.get(i);
      if (curSug == null) {
        curSug = sug; 
      } else {
        // find common string
        int common = strcmp(sug, curSug);
        if (common > 0) {
          minOffs = Math.min(common, minOffs);
          curSug = sug;
        }
      }
    }
    if (curSug != null) {
      setText(curSug.substring(0, Math.min(curSug.length(),minOffs)));
      userSelectedSuggestion = true;
    }
  }
  
  /**
   * Accepts current suggestion. Accepts wholly if previous suggestion was accepted.
   */
  public void accept() {
    if (userSelectedSuggestion) {
      acceptSuggestionWholly();
      if (suggestionListener != null) suggestionListener.gotSuggestions(null);
      userSelectedSuggestion = false;
    } else {
      List<String> foundSuggestions = acceptSuggestion();
      if (suggestionListener != null) {
        suggestionListener.gotSuggestions(foundSuggestions);
      }
      if (foundSuggestions != null && foundSuggestions.size() > 1) {
        userSelectedSuggestion = true;
      } else {
        userSelectedSuggestion = false;
      }
    }
  }
  
  public List<String> getCurrentSuggestions() {
    List<String> s = new ArrayList<String>();
    Model m = (userString.isEmpty() || userStringFullySuggested) ? history : model;
    if (!userString.equals(lastUserModelQuery) && suggestionListener != null) {
      lastUserModelQuery = userString;
      userModel.suggestions = suggestionListener.giveSuggestions(userString);
    }
    if (userModel.size() > 0) m = userModel;
    if (m == history) {
      s.addAll(m.suggestions);
    } else {
      if (m != null) {
        for (String sug : m.suggestions) {
          if (sug.startsWith(userString)) {
            s.add(sug);
          }
        }
      }
    }
    return s;
  }
  
  void handleUpdate(FilterBypass fb, int offset, String newUserString) throws BadLocationException {
    userSelectedSuggestion = false;
    if (!temporaryStringRemove && suggestionListener != null) {
      suggestionListener.gotSuggestions(null);
    }
    if (!forced || newUserString.length() == 0  || temporaryStringUpdate) {
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
    Model m = progTrig ? history : model;

    if (progTrig || (newUserString != null && newUserString.length() > 0)) {
      String suggestion = null;

      if (suggestionListener != null) {
        if (!userString.equals(lastUserModelQuery)) {
          lastUserModelQuery = userString;
          if (!temporaryStringRemove) {
            userModel.suggestions = suggestionListener.giveSuggestions(userString);
          }
          userModel.index = -1;
        }
        if (userModel.size() > 0) {
          m = userModel;
        }
      }
      
      if (m.index >= 0 && m.index < m.size()) {
        String lastSuggestion = m.get(m.index);
        if (lastSuggestion.startsWith(newUserString)) {
          suggestion = lastSuggestion;
        }
      }

      if (suggestion == null) {
        for (int i = m.size() - 1; i >= 0; i--) {
          String s = m.get(i);
          if (s.startsWith(newUserString)) {
            m.index = i;
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
        m.index = -1;
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
    temporaryStringRemove = true;
    try {
      getDocument().remove(0, super.getText().length());
    } catch (BadLocationException e) {
    }
    temporaryStringRemove = false;
    temporaryStringUpdate = true;
    try {
      getDocument().insertString(0, s, null);
    } catch (BadLocationException e) {
    }
    temporaryStringUpdate = false;
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
        String newUserString;
        if (forced) {
          newUserString = userString.substring(0, offset); 
        } else {
          newUserString = userString.substring(0, offset) + userString.substring(offset + length); 
        }
        handleUpdate(fb, offset, newUserString);
      }
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string,
        AttributeSet attr) throws BadLocationException {
      if (userString == null) userString = "";
      if (filterNewLine) {
        string = string.replaceAll("\\r?\\n", "");
      }
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
      if (filterNewLine) {
        text = text.replaceAll("\\r?\\n", "");
      }
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
    public List<String> giveSuggestions(String userInput);
  }
  
  class Model {
    List<String> suggestions;
    int index;
    public int size() {
      return suggestions == null ? 0 : suggestions.size();
    }
    String get(int ix) {
      return suggestions == null ? null : suggestions.get(ix);
    }
    void add(String t) {
      if (suggestions == null) {
        suggestions = new ArrayList<String>();
      }
      suggestions.add(t);
    }
  }
}