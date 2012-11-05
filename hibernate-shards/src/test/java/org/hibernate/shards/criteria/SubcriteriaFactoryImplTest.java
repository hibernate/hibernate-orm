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

package org.hibernate.shards.criteria;

import junit.framework.TestCase;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.shards.defaultmock.CriteriaDefaultMock;
import org.hibernate.shards.util.Lists;

/**
 * @author maxr@google.com (Max Ross)
 */
public class SubcriteriaFactoryImplTest extends TestCase {

  private static final Iterable<CriteriaEvent> NO_EVENTS = Lists.newArrayList();

  public void testOnOpenSessionAssocPath() {
    SubcriteriaFactoryImpl sfi = new SubcriteriaFactoryImpl(null);
    final boolean[] called = {false};
    Criteria crit = new CriteriaDefaultMock() {
      @Override
      public Criteria createCriteria(String associationPath)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    sfi.createSubcriteria(crit, NO_EVENTS);
    assertTrue(called[0]);
    called[0] = false;

    MyCriteriaEvent mce1 = new MyCriteriaEvent();
    MyCriteriaEvent mce2 = new MyCriteriaEvent();
    sfi.createSubcriteria(crit, Lists.<CriteriaEvent>newArrayList(mce1, mce2));
    assertTrue(called[0]);
    assertEquals(1, mce1.numOnEventCalls);
    assertEquals(1, mce2.numOnEventCalls);
  }

  public void testOnOpenSessionAssocPathAndJoinType() {
    SubcriteriaFactoryImpl sfi = new SubcriteriaFactoryImpl(null, 0);
    final boolean[] called = {false};
    Criteria crit = new CriteriaDefaultMock() {

      @Override
      public Criteria createCriteria(String associationPath, int joinType)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    sfi.createSubcriteria(crit, NO_EVENTS);
    assertTrue(called[0]);
    called[0] = false;

    MyCriteriaEvent mce1 = new MyCriteriaEvent();
    MyCriteriaEvent mce2 = new MyCriteriaEvent();
    sfi.createSubcriteria(crit, Lists.<CriteriaEvent>newArrayList(mce1, mce2));
    assertTrue(called[0]);
    assertEquals(1, mce1.numOnEventCalls);
    assertEquals(1, mce2.numOnEventCalls);
  }

  public void testOnOpenSessionAssocPathAndAlias() {
    SubcriteriaFactoryImpl sfi = new SubcriteriaFactoryImpl(null, null);
    final boolean[] called = {false};
    Criteria crit = new CriteriaDefaultMock() {
      @Override
      public Criteria createCriteria(String associationPath, String alias)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    sfi.createSubcriteria(crit, NO_EVENTS);
    assertTrue(called[0]);
    called[0] = false;

    MyCriteriaEvent mce1 = new MyCriteriaEvent();
    MyCriteriaEvent mce2 = new MyCriteriaEvent();
    sfi.createSubcriteria(crit, Lists.<CriteriaEvent>newArrayList(mce1, mce2));
    assertTrue(called[0]);
    assertEquals(1, mce1.numOnEventCalls);
    assertEquals(1, mce2.numOnEventCalls);
  }

  public void testOnOpenSessionAssocPathAndAliasAndJoinType() {
    SubcriteriaFactoryImpl sfi = new SubcriteriaFactoryImpl(null, null, 0);
    final boolean[] called = {false};
    Criteria crit = new CriteriaDefaultMock() {
      @Override
      public Criteria createCriteria(String associationPath, String alias, int joinType)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    sfi.createSubcriteria(crit, NO_EVENTS);
    assertTrue(called[0]);
    called[0] = false;

    MyCriteriaEvent mce1 = new MyCriteriaEvent();
    MyCriteriaEvent mce2 = new MyCriteriaEvent();
    sfi.createSubcriteria(crit, Lists.<CriteriaEvent>newArrayList(mce1, mce2));
    assertTrue(called[0]);
    assertEquals(1, mce1.numOnEventCalls);
    assertEquals(1, mce2.numOnEventCalls);
  }

  private static final class MyCriteriaEvent implements CriteriaEvent {
    private int numOnEventCalls;
    public void onEvent(Criteria crit) {
      numOnEventCalls++;
    }
  }

}
