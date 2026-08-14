/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests lazy many-to-one association to a {@link ConcreteProxy} entity.
 */
@DomainModel(annotatedClasses = {
		ConcreteProxyLazyManyToOneTest.Vehicle.class,
		ConcreteProxyLazyManyToOneTest.Owner.class,
		ConcreteProxyLazyManyToOneTest.Person.class,
		ConcreteProxyLazyManyToOneTest.Company.class,
})
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
@JiraKey("HHH-18911")
public class ConcreteProxyLazyManyToOneTest {

	@Test
	public void testManyToOneWithInnerJoin(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getStatementInspector( SQLStatementInspector.class );
		inspector.clear();
		scope.inSession( session -> {
			final List<Vehicle> vehicles =
					session.createQuery( "select v from Vehicle v join v.owner o", Vehicle.class ).getResultList();
			assertOwnerProxy( vehicles );
			inspector.assertExecutedCount( 1 );
		} );
	}

	@Test
	public void testManyToOneWithLeftJoin(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getStatementInspector( SQLStatementInspector.class );
		inspector.clear();
		scope.inSession( session -> {
			final List<Vehicle> vehicles =
					session.createQuery( "select v from Vehicle v left join v.owner o", Vehicle.class ).getResultList();
			assertOwnerProxy( vehicles );
			inspector.assertExecutedCount( 1 );
		} );
	}

	private static void assertOwnerProxy(List<Vehicle> vehicles) {
		assertThat( vehicles.size(), is( 1 ) );
		final Owner owner = vehicles.get(0).getOwner();
		assertThat( Hibernate.isInitialized( owner ), is( false ) );
		assertThat( owner, instanceOf( Person.class ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Vehicle vehicle = new Vehicle( 1L, new Person( 1L, "John" ) );
			session.persist( vehicle );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Vehicle")
	public static class Vehicle {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Owner owner;

		public Vehicle() {
		}

		public Vehicle(Long id, Owner owner) {
			this.id = id;
			this.owner = owner;
			owner.getVehicles().add( this );
		}

		public Owner getOwner() {
			return owner;
		}
	}

	@Entity(name = "Owner")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type")
	@ConcreteProxy
	public static abstract class Owner {
		@Id
		private Long id;

		@OneToMany(mappedBy = "owner")
		private List<Vehicle> vehicles = new ArrayList<>();

		public Owner() {
		}

		public Owner(Long id) {
			this.id = id;
		}

		public List<Vehicle> getVehicles() {
			return vehicles;
		}
	}

	@Entity(name = "Person")
	public static class Person extends Owner {
		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			super( id );
			this.name = name;
		}
	}

	@Entity(name = "Company")
	public static class Company extends Owner {
		private String companyName;

		public Company() {
		}

		public Company(Long id, String companyName) {
			super( id );
			this.companyName = companyName;
		}
	}
}
