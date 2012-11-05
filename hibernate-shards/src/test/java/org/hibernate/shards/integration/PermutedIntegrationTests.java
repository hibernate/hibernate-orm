/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.integration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hibernate.shards.integration.id.IdGeneratorPermutedIntegrationTest;
import org.hibernate.shards.integration.model.InterceptorBehaviorPermutedIntegrationTest;
import org.hibernate.shards.integration.model.ModelCriteriaPermutedIntegrationTest;
import org.hibernate.shards.integration.model.ModelPermutedIntegrationTest;
import org.hibernate.shards.integration.model.ModelQueryPermutedIntegrationTest;
import org.hibernate.shards.util.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class PermutedIntegrationTests extends TestSuite {

  public static final List<Class<? extends TestCase>> CLASSES = Collections.unmodifiableList(buildListOfPermutedClasses());

  private static List<Class<? extends TestCase>> buildListOfPermutedClasses() {
    List<Class<? extends TestCase>> classes = new ArrayList<Class<? extends TestCase>>();
    classes.add(BaseShardingIntegrationTestCasePermutedIntegrationTest.class);
    classes.add(IdGeneratorPermutedIntegrationTest.class);
    classes.add(ModelPermutedIntegrationTest.class);
    classes.add(InterceptorBehaviorPermutedIntegrationTest.class);
    classes.add(ModelCriteriaPermutedIntegrationTest.class);
    classes.add(ModelQueryPermutedIntegrationTest.class);
    classes.add(ConfigPermutedIntegrationTest.class);
    classes.add(DbAccessPermutedIntegrationTest.class);
    return classes;
  }

  public static Test suite() {
    TestSuite suiteOfTestsNeedingPermutation = buildSuiteOfTestsNeedingPermutation();
    TestSuite permutedSuite = new TestSuite("Permuted Integration Tests");
    /**
     * Build a separate test suite for each permutation.  We are permuting
     * along type of id generation, shard access strategy, and number of shards
     */
    for(Permutation perm : buildPermutationList()) {
      TestSuite permutationSuite = new TestSuite(perm.toString());
      permutedSuite.addTest(permutationSuite);
      addPermutations(suiteOfTestsNeedingPermutation, perm, permutationSuite);
    }
    return permutedSuite;
  }

  private static TestSuite buildSuiteOfTestsNeedingPermutation() {
    TestSuite suite = new TestSuite();
    for(Class<? extends TestCase> testClass : CLASSES) {
      suite.addTestSuite(testClass);
    }
    return suite;
  }

  private static final int MIN_SHARDS = 1;
  private static final int MAX_SHARDS = 3;

  private static List<Permutation> buildPermutationList() {
    List<Permutation> list = Lists.newArrayList();
    for(IdGenType idGenType : IdGenType.values()) {
      for(ShardAccessStrategyType sast : ShardAccessStrategyType.values()) {
        for(int i = MIN_SHARDS; i <= MAX_SHARDS; i++) {
          list.add(new Permutation(idGenType, sast, i));
          if (idGenType.getSupportsVirtualSharding()) {
            list.add(new Permutation(idGenType, sast, i, 9, true));
            list.add(new Permutation(idGenType, sast, i, i, true));
          }
        }
      }
    }
    return list;
  }

  private static void addPermutations(Test t, Permutation perm, TestSuite permutationSuite) {
    if(t instanceof TestCase) {
      addActualPermutation((TestCase)t, perm, permutationSuite);
    } else if (t instanceof TestSuite) {
      TestSuite ts = (TestSuite) t;
      TestSuite subSuite = new TestSuite(ts.getName());
      permutationSuite.addTest(subSuite);
      for(Test test : asIterable(ts)) {
        addPermutations(test, perm, subSuite);
      }
    } else {
      throw new RuntimeException("wuzzat?");
    }
  }

  private static void addActualPermutation(
      TestCase testCase,
      Permutation perm,
      TestSuite permutationSuite) {
      permutationSuite.addTest(createTest(testCase, perm));
  }

  private static Test createTest(
      TestCase prototype,
      Permutation permutation) {
    Test t = TestSuite.createTest(prototype.getClass(), prototype.getName());
    ((HasPermutation)t).setPermutation(permutation);
    return t;
  }

  private static Iterable<Test> asIterable(TestSuite ts) {
    List<Test> list = new ArrayList<Test>();
    for(Enumeration testEnum = ts.tests(); testEnum.hasMoreElements(); ) {
      list.add((Test) testEnum.nextElement());
    }
    return list;
  }
}
