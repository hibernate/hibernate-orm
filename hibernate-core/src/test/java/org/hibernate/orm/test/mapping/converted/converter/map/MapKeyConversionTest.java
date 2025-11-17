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

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8529" )
@DomainModel(annotatedClasses = {MapKeyConversionTest.Customer.class, ColorTypeConverter.class})
@SessionFactory
public class MapKeyConversionTest {

	@Test
	public void testElementCollectionConversion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Customer customer = new Customer( 1 );
			customer.colors.put( ColorType.BLUE, "favorite" );
			session.persist( customer );
		} );

		scope.inTransaction(  session -> assertEquals( 1, session.find( Customer.class, 1 ).colors.size() ) );

		scope.dropData();
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
