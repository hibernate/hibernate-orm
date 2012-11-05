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
import org.hibernate.criterion.AvgProjection;
import org.hibernate.criterion.Projections;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.shards.defaultmock.ClassMetadataDefaultMock;
import org.hibernate.shards.defaultmock.EntityPersisterDefaultMock;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;


/**
 * @author Maulik Shah
 */
public class AggregateExitOperationTest extends TestCase {

  private List<Object> data;

  private class MyInt implements Comparable {
    private final Integer i;

    public MyInt(int i) {
      this.i = i;
    }

    public Number getValue() {
      return i;
    }

    public int compareTo(Object o) {
      MyInt i = (MyInt) o;
      return (Integer)this.getValue() - (Integer)i.getValue();
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    data = Lists.<Object>newArrayList();
    for(int i=0; i<6; i++) {
      if (i == 4) {
        data.add(null);
      } else {
        data.add(new MyInt(i));
      }
    }
  }

  public void testCtor() throws Exception {
    try {
      new AggregateExitOperation(new AvgProjection("foo"));
      fail();
    } catch (IllegalArgumentException e) {
      // good
    }
    try {
      new AggregateExitOperation(new AvgProjection("foo"));
      fail();
    } catch (IllegalArgumentException e) {
      // good
    }
    new AggregateExitOperation(Projections.max("foo"));
    new AggregateExitOperation(Projections.min("foo"));
    new AggregateExitOperation(Projections.sum("foo"));
  }

  public void testSum() throws Exception {
    AggregateExitOperation exitOp = new AggregateExitOperation(Projections.sum("value"));

    List<Object> result = exitOp.apply(data);
    assertEquals(new BigDecimal(11.0), (BigDecimal)result.get(0));
  }

  public void testMax() throws Exception {
    AggregateExitOperation exitOp = new AggregateExitOperation(Projections.max("value"));

    List<Object> result = exitOp.apply(data);
    assertEquals(5, ((MyInt)result.get(0)).getValue());
  }

  public void testMin() throws Exception {
    AggregateExitOperation exitOp = new AggregateExitOperation(Projections.min("value"));

    List<Object> result = exitOp.apply(data);
    assertEquals(0, ((MyInt)result.get(0)).getValue());
  }

  static class SessionFactoryMock extends SessionFactoryDefaultMock {

    public ClassMetadata getClassMetadata(Class persistentClass)
        throws HibernateException {
      return new ClassMetadataMock();
    }

    public EntityPersister getEntityPersister(String entityName)
        throws MappingException {
      return new EntityPersisterMock();
    }
  }

  static class ClassMetadataMock extends ClassMetadataDefaultMock {

    public String getEntityName() {
      return "";
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
