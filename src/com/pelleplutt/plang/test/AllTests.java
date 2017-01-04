package com.pelleplutt.plang.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BasicEval.class, BasicEval2.class, Eval.class, Scope.class, VisitorMutator.class,
  InstanceMe.class})
public class AllTests {

}
