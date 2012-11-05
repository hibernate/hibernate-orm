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
import org.hibernate.criterion.Order;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.shards.defaultmock.ClassMetadataDefaultMock;
import org.hibernate.shards.defaultmock.EntityPersisterDefaultMock;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Maulik Shah
 */
public class OrderExitOperationTest extends TestCase {

  private List<Object> data;
  private ArrayList<Object> shuffledList;
  private List<Object> nonNullData;

  private class MyInt {
    private final Integer i;

    private final String name;

    private MyInt innerMyInt;

    public MyInt(int i, String name) {
      this.i = i;
      this.name = name;
    }

    public MyInt getInnerMyInt() {
      return innerMyInt;
    }

    public void setInnerMyInt(MyInt innerMyInt) {
      this.innerMyInt = innerMyInt;
    }

    public Number getValue() {
      return i;
    }

    public String getName() {
      return name;
    }

    public boolean equals(Object obj) {
      MyInt myInt = (MyInt) obj;
      return
          this.getName().equals(myInt.getName()) &&
          this.getValue().equals(myInt.getValue());
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    String[] names = {"tomislav", "max", "maulik", "gut", "null", "bomb"};
    data = Lists.newArrayList();
    for(int i=0; i<6; i++) {
      if (i == 4) {
        data.add(null);
      } else {
        data.add(new MyInt(i, names[i]));
      }
    }

    nonNullData = ExitOperationUtils.getNonNullList(data);

    shuffledList = Lists.newArrayList(data);
    Collections.shuffle(shuffledList);
  }

  public void testApply() throws Exception {
    Order order = Order.asc("value");
    OrderExitOperation oeo = new OrderExitOperation(order);
    List unShuffledList = oeo.apply(shuffledList);

    assertTrue(nonNullData.equals(unShuffledList));
  }

  public void testMultipleOrderings() throws Exception {
    Order orderValue = Order.asc("value");
    Order orderName = Order.desc("name");

    OrderExitOperation oeoValue = new OrderExitOperation(orderValue);
    OrderExitOperation oeoName = new OrderExitOperation(orderName);

    List<MyInt> answer =
        Lists.newArrayList(
            new MyInt(0, "tomislav"),
            new MyInt(1, "max"),
            new MyInt(2, "maulik"),
            new MyInt(3, "gut"),
            new MyInt(5, "bomb"));
    List unShuffledList = oeoName.apply(oeoValue.apply(shuffledList));


    assertEquals(answer, unShuffledList);

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
