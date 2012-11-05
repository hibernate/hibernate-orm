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
import org.hibernate.Session;
import org.hibernate.shards.defaultmock.SessionDefaultMock;

/**
 * @author maxr@google.com (Max Ross)
 */
public class CriteriaFactoryImplTest extends TestCase {

  public void testOnOpenSessionAssocPath() {
    CriteriaFactoryImpl cfi = new CriteriaFactoryImpl("entity name");
    final boolean[] called = {false};
    Session session = new SessionDefaultMock() {
      @Override
      public Criteria createCriteria(String entityName)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    cfi.createCriteria(session);
    assertTrue(called[0]);
  }

  public void testOnOpenSessionAssocPathAndJoinType() {
    CriteriaFactoryImpl cfi = new CriteriaFactoryImpl("entity name", "alias");
    final boolean[] called = {false};
    Session session = new SessionDefaultMock() {
      @Override
      public Criteria createCriteria(String entityName, String alias)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    cfi.createCriteria(session);
    assertTrue(called[0]);
  }

  public void testOnOpenSessionAssocPathAndAlias() {
    CriteriaFactoryImpl cfi = new CriteriaFactoryImpl(String.class);
    final boolean[] called = {false};
    Session session = new SessionDefaultMock() {
      @Override
      public Criteria createCriteria(Class pc)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    cfi.createCriteria(session);
    assertTrue(called[0]);
  }

  public void testOnOpenSessionAssocPathAndAliasAndJoinType() {
    CriteriaFactoryImpl cfi = new CriteriaFactoryImpl(String.class, "alias");
    final boolean[] called = {false};
    Session session = new SessionDefaultMock() {
      @Override
      public Criteria createCriteria(Class pc, String alias)
          throws HibernateException {
        called[0] = true;
        return null;
      }
    };
    cfi.createCriteria(session);
    assertTrue(called[0]);
  }
}
