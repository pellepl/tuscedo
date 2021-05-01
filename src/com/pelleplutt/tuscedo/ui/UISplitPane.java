package com.pelleplutt.tuscedo.ui;

import java.awt.Component;

import javax.swing.JSplitPane;

import com.pelleplutt.tuscedo.ui.UIInfo.UIListener;
import com.pelleplutt.util.Log;

public class UISplitPane extends JSplitPane implements UIO, UIListener {
  static int __id = 0;
  final UIInfo uiinfo;
  UIO primeUI = null;
  UIO secondUI = null;

  void update() {
    if (primeUI != null && secondUI != null) {
      UICommon.decorateSplitPane(this, true);
    } else if (primeUI == null && secondUI == null) {
      if (getUIInfo().getParent() != null) getUIInfo().getParent().removeChild(this);
    } else {
      setBorder(null);
      setDividerSize(0);
    }
  }
  
  public UISplitPane(int orientation) {
    super(orientation);
    uiinfo = new UIInfo(this, "splitter" + (__id++), "");
    //UIInfo.fireEventOnCreated(uiinfo);
  }

  @Override
  public UIInfo getUIInfo() {
    return uiinfo;
  }

  public void onClose() {
  }

  @Override
  public void decorateUI() {
    update();
  }

  public void setTopUI(UIO c) {
    Component prevC = getTopComponent();
    if (prevC != null && prevC instanceof UIO) {
      getUIInfo().removeChild((UIO) prevC);
      ((UIO)prevC).getUIInfo().removeListener(this);
    }
    if (c != null) {
      getUIInfo().addChild(c);
      c.getUIInfo().addListener(this);
    }
    primeUI = c;
    super.setTopComponent((Component) c);
    update();
  }

  public void setBottomUI(UIO c) {
    Component prevC = getBottomComponent();
    if (prevC != null && prevC instanceof UIO) {
      getUIInfo().removeChild((UIO) prevC);
      ((UIO)prevC).getUIInfo().removeListener(this);
    }
    if (c != null) {
      getUIInfo().addChild(c);
      c.getUIInfo().addListener(this);
    }
    secondUI = c;
    super.setBottomComponent((Component) c);
    update();
  }

  public void setLeftUI(UIO c) {
    Component prevC = getLeftComponent();
    if (prevC != null && prevC instanceof UIO) {
      getUIInfo().removeChild((UIO) prevC);
      ((UIO)prevC).getUIInfo().removeListener(this);
    }
    if (c != null) {
      getUIInfo().addChild(c);
      c.getUIInfo().addListener(this);
    }
    primeUI = c;
    super.setLeftComponent((Component) c);
    update();
  }

  public void setRightUI(UIO c) {
    Component prevC = getRightComponent();
    if (prevC != null && prevC instanceof UIO) {
      getUIInfo().removeChild((UIO) prevC);
      ((UIO)prevC).getUIInfo().removeListener(this);
    }
    if (c != null) {
      getUIInfo().addChild(c);
      c.getUIInfo().addListener(this);
    }
    secondUI = c;
    super.setRightComponent((Component) c);
    update();
  }
  
  public UIO getTopUI() {
    return primeUI;
  }
  public UIO getLeftUI() {
    return primeUI;
  }
  public UIO getBottomUI() {
    return secondUI;
  }
  public UIO getRightUI() {
    return secondUI;
  }

  @Override
  public void onRemoved(UIO parent, UIO child) {
    Log.println("parent " + parent.getUIInfo().asString() + ", child " + child.getUIInfo().asString());
    if (child == primeUI) setTopUI(null);
    if (child == secondUI) setBottomUI(null);
    update();
  }

  @Override
  public void onAdded(UIO parent, UIO child) {
  }

  @Override
  public void onClosed(UIO parent, UIO child) {
    Log.println("parent " + parent.getUIInfo().asString() + ", child " + child.getUIInfo().asString());
    if (child == primeUI) setTopUI(null);
    if (child == secondUI) setBottomUI(null);
    update();
  }

  @Override
  public void onCreated(UIInfo obj) {
  }

  @Override
  public void onEvent(UIO obj, Object event) {
  }
}
