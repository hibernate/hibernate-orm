/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.map;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8529" )
public class MapKeyConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Customer.class, ColorTypeConverter.class };
	}

	@Test
	public void testElementCollectionConversion() {
		Session session = openSession();
		session.getTransaction().begin();
		Customer customer = new Customer( 1 );
		customer.colors.put( ColorType.BLUE, "favorite" );
		session.persist( customer );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		assertEquals( 1, session.get( Customer.class, 1 ).colors.size() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		customer = session.get( Customer.class, 1 );
		session.remove( customer );
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "Customer" )
	@Table( name = "CUST" )
	public static class Customer {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable( name = "cust_color", joinColumns = @JoinColumn( name = "cust_fk" ) )
		private Map<ColorType, String> colors = new HashMap<ColorType, String>();

		public Customer() {
		}

		public Customer(Integer id) {
			this.id = id;
		}
	}
}
