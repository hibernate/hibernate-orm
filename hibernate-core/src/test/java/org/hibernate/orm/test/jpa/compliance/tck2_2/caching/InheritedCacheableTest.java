/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				InheritedCacheableTest.Person.class,
				InheritedCacheableTest.Employee.class,
				InheritedCacheableTest.Customer.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry( settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.JPA_SHARED_CACHE_MODE, value = "ENABLE_SELECTIVE")
		}
)
public class InheritedCacheableTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Person.class ).hasCache(),
				CoreMatchers.is( true )
		);
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Employee.class ).hasCache(),
				CoreMatchers.is( true )
		);
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class ).hasCache(),
				CoreMatchers.is( false )
		);
	}


	@Test
	public void testOnlySubclassIsCached(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction(
				s -> {
					s.persist( new Employee( "1", "John Doe", "987", "engineering") );
					s.persist( new Customer( "2", "Acme Corp", "123" ) );
				}
		);

		assertTrue( scope.getSessionFactory().getCache().contains( Employee.class, "1" ) );
		assertTrue( scope.getSessionFactory().getCache().contains( Person.class, "1" ) );

		assertFalse( scope.getSessionFactory().getCache().contains( Customer.class, "2" ) );
		assertFalse( scope.getSessionFactory().getCache().contains( Person.class, "2" ) );

		scope.inTransaction(
				s -> {
					statistics.clear();

					final Customer customer = s.find( Customer.class, "2" );
					assertTrue( Hibernate.isInitialized( customer ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(0L) );
					assertThat( statistics.getSecondLevelCachePutCount(), CoreMatchers.is(0L) );

					statistics.clear();

					final Employee emp = s.find( Employee.class, "1" );
					assertTrue( Hibernate.isInitialized( emp ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(1L) );
					assertThat( statistics.getSecondLevelCachePutCount(), CoreMatchers.is(0L) );
				}
		);

		scope.inTransaction(
				s -> {
					statistics.clear();

					final Person customer = s.find( Person.class, "2" );
					assertTrue( Hibernate.isInitialized( customer ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(0L) );
					assertThat( statistics.getSecondLevelCachePutCount(), CoreMatchers.is(0L) );

					statistics.clear();

					final Person emp = s.find( Person.class, "1" );
					assertTrue( Hibernate.isInitialized( emp ) );
					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(1L) );
					assertThat( statistics.getSecondLevelCachePutCount(), CoreMatchers.is(0L) );
				}
		);
	}


	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Cacheable()
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class Person {
		@Id
		public String id;
		public String name;

		public Person() {
		}

		public Person(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Employee" )
	public static class Employee extends Person {
		public String employeeCode;
		public String costCenter;

		public Employee() {
		}

		public Employee(String id, String name, String employeeCode, String costCenter) {
			super( id, name );
			this.employeeCode = employeeCode;
			this.costCenter = costCenter;
		}
	}

	@Entity( name = "Customer" )
	@Cacheable(false)
	public static class Customer extends Person {
		public String erpCode;

		public Customer() {
		}

		public Customer(String id, String name, String erpCode) {
			super( id, name );
			this.erpCode = erpCode;
		}
	}

}
