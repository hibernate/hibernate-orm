/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import java.math.BigDecimal;
import java.util.List;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid.Item;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@FailureExpectedWithNewMetamodel // i think the whole SpecJ "partially generated id" thing causes problems in the new code
public class CompositeKeyDeleteTest extends BaseCoreFunctionalTestCase {

   public String[] getMappings() {
      return new String[] { "annotations/derivedidentities/e1/b/specjmapid/lazy/order_orm.xml" };
   }

   public CompositeKeyDeleteTest() {
      System.setProperty( "hibernate.enable_specj_proprietary_syntax", "true" );
   }
   /**
    * This test checks to make sure the non null column is not updated with a
    * null value when a CustomerInventory is removed.
    */
   @Test
   public void testRemove() {
      Session s = openSession();
      Transaction tx = s.beginTransaction();

      CustomerTwo c1 = new CustomerTwo(
            "foo", "bar", "contact1", "100", new BigDecimal( 1000 ), new BigDecimal( 1000 ), new BigDecimal( 1000 )
      );

      s.persist( c1 );
      s.flush();
      s.clear();

      Item boat = new Item();
      boat.setId( "1" );
      boat.setName( "cruiser" );
      boat.setPrice( new BigDecimal( 500 ) );
      boat.setDescription( "a boat" );
      boat.setCategory( 42 );

      s.persist( boat );


      Item house = new Item();
      house.setId( "2" );
      house.setName( "blada" );
      house.setPrice( new BigDecimal( 5000 ) );
      house.setDescription( "a house" );
      house.setCategory( 74 );

      s.persist( house );
      s.flush();
      s.clear();

      c1.addInventory( boat, 10, new BigDecimal( 5000 ) );

      c1.addInventory( house, 100, new BigDecimal( 50000 ) );
      s.merge( c1 );
      Integer id = c1.getId();
      tx.commit();
      s.close();
      
      s = openSession();
      tx = s.beginTransaction();

      CustomerTwo c12 = ( CustomerTwo) s.createQuery( "select c from CustomerTwo c" ).uniqueResult();
      Assert.assertNotNull(c12);
      List<CustomerInventoryTwo> list = c12.getInventories();
      Assert.assertNotNull(list);
      Assert.assertEquals(2, list.size());
      CustomerInventoryTwo ci = list.get(1);
      list.remove(ci);
      s.delete(ci);
      s.flush();
      
      tx.commit();//fail
      s.close();

   }

   @Override
   protected Class[] getAnnotatedClasses() {
      return new Class[] {
            CustomerTwo.class,
            CustomerInventoryTwo.class,
            CustomerInventoryTwoPK.class,
            Item.class

      };
   }
}
