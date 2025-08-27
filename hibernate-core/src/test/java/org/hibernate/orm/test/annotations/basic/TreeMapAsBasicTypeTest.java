/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.basic;

import java.util.TreeMap;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-17320")
@DomainModel(
		annotatedClasses = {
				TreeMapAsBasicTypeTest.Customer.class
		}
)
@SessionFactory
public class TreeMapAsBasicTypeTest {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		Long cutomerId = 1l;
		scope.inTransaction(
				session -> {
					TreeMap<String, Integer> data = new TreeMap<>();
					data.put( "key", 1 );
					TreeMap<String, String> data2 = new TreeMap<>();
					data2.put( "key2", "2" );
					Customer c = new Customer( cutomerId, data, data2 );
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Customer customer = session.find( Customer.class, cutomerId );
					TreeMap<String, Integer> data = customer.getData();
					assertThat( data ).isNotNull();
					assertThat( data.get( "key" ) ).isEqualTo( 1 );
					TreeMap<String, String> data2 = customer.getData2();
					assertThat( data2 ).isNotNull();
					assertThat( data2.get( "key2" ) ).isEqualTo( "2" );
				}
		);
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private TreeMap<String, Integer> data;

		private TreeMap<String, String> data2;

		public Customer() {
		}

		public Customer(Long id, TreeMap<String, Integer> data, TreeMap<String, String> data2) {
			this.id = id;
			this.data = data;
			this.data2 = data2;
		}

		public Long getId() {
			return id;
		}

		public TreeMap<String, Integer> getData() {
			return data;
		}

		public TreeMap<String, String> getData2() {
			return data2;
		}
	}
}
