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

package org.hibernate.shards.strategy.exit;

import junit.framework.TestCase;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.criterion.Projections;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.shards.defaultmock.EntityPersisterDefaultMock;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.util.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Maulik Shah
 */
public class ProjectionExitOperationFactoryTest extends TestCase {

  public void testReturnedOperations() throws Exception {
    ProjectionExitOperationFactory factory = ProjectionExitOperationFactory.getFactory();

    assertTrue(factory.getProjectionExitOperation(Projections.rowCount(), new SessionFactoryMock()) instanceof RowCountExitOperation);
    assertTrue(factory.getProjectionExitOperation(Projections.max("foo"), new SessionFactoryMock()) instanceof AggregateExitOperation);
    assertTrue(factory.getProjectionExitOperation(Projections.min("foo"), new SessionFactoryMock()) instanceof AggregateExitOperation);
    assertTrue(factory.getProjectionExitOperation(Projections.sum("foo"), new SessionFactoryMock()) instanceof AggregateExitOperation);
    try {
      factory.getProjectionExitOperation(Projections.avg("foo"), new SessionFactoryMock());
      fail("example of one that we don't yet support");
    } catch (IllegalArgumentException e) {
      // good
    }
  }

  static class SessionFactoryMock extends SessionFactoryDefaultMock {

    public ClassMetadata getClassMetadata(Class persistentClass)
        throws HibernateException {
      return null;
    }

    public EntityPersister getEntityPersister(String entityName)
        throws MappingException {
      return new EntityPersisterMock();
    }
  }

  static class EntityPersisterMock extends EntityPersisterDefaultMock {

    public Object getPropertyValue(Object object, String propertyName,
        EntityMode entityMode) throws HibernateException {
      Class clazz = object.getClass();
      propertyName = StringUtil.capitalize(propertyName);
      try {
        Method m = clazz.getMethod("get" + propertyName);
        return m.invoke(object);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
