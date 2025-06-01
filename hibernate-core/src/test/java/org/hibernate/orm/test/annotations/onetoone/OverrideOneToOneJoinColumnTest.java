/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aresnii Skvortsov
 */
@JiraKey(value = "HHH-4384")
@BaseUnitTest
public class OverrideOneToOneJoinColumnTest {

	@Test
	public void allowIfJoinColumnIsAbsent() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Person.class )
					.addAnnotatedClass( State.class )
					.buildMetadata();

			final Table personTable = metadata.getDatabase().getDefaultNamespace().locateTable(
					Identifier.toIdentifier( "PERSON_TABLE" ) );
			final Collection<ForeignKey> foreignKeys = personTable.getForeignKeyCollection();
			assertThat( foreignKeys.size(), is( 1 ) );
			final Optional<ForeignKey> foreignKey = foreignKeys.stream().findFirst();

			assertEquals(
					"PERSON_ADDRESS_STATE",
					foreignKey.get().getColumn( 0 ).getName(),
					"Overridden join column name should be applied"
			);
		}
	}

	@Test
	public void disallowOnSideWithMappedBy() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final AnnotationException ex = assertThrows(
					AnnotationException.class, () ->
							new MetadataSources( ssr )
									.addAnnotatedClass( Employee.class )
									.addAnnotatedClass( PartTimeEmployee.class )
									.addAnnotatedClass( Desk.class )
									.buildMetadata()
			);

			assertTrue(
					ex.getMessage().contains( "is 'mappedBy' a different entity and may not explicitly specify the '@JoinColumn'" ),
					"Should disallow exactly because of @JoinColumn override on side with mappedBy"
			);
		}
	}

	@Entity(name = "Person")
	@jakarta.persistence.Table(name = "PERSON_TABLE")
	public static class Person {

		private String id;

		private Address address;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Embedded
		@AssociationOverride(name = "state", joinColumns = { @JoinColumn(name = "PERSON_ADDRESS_STATE") })
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Embeddable
	public static class Address {

		private String street;

		private String city;

		private State state;

		@OneToOne
		public State getState() {
			return state;
		}

		public void setState(State state) {
			this.state = state;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}

	@Entity(name = "State")
	@jakarta.persistence.Table(name = "STATE_TABLE")
	public static class State {

		private String id;

		private String name;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	public static class Employee {

		@Id
		private Long id;

		private String name;

		@OneToOne(mappedBy = "employee")
		protected Desk desk;
	}

	@Entity
	@AssociationOverride(name = "desk",
			joinColumns = @JoinColumn(name = "PARTTIMEEMPLOYEE_DESK"))
	public static class PartTimeEmployee extends Employee {

	}

	@Entity(name = "Desk")
	public static class Desk {
		@Id
		private Long id;

		@OneToOne
		private PartTimeEmployee employee;

		private String location;
	}


}
