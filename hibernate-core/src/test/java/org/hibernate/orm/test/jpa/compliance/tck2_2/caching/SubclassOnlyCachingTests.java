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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SubclassOnlyCachingTests.Person.class,
				SubclassOnlyCachingTests.Employee.class,
				SubclassOnlyCachingTests.Customer.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry( settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.JPA_SHARED_CACHE_MODE, value = "ENABLE_SELECTIVE")
}
)
public class SubclassOnlyCachingTests {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Person.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Employee.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class ).hasCache(),
				CoreMatchers.is( true )
		);
	}

	@Test
	public void testOnlySubclassIsCached(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction(
				s -> s.persist( new Customer( 1, "Acme Corp", "123" ) )
		);

		assertTrue( scope.getSessionFactory().getCache().contains( Customer.class, 1 ) );

		scope.inTransaction(
				s -> {
					statistics.clear();

					final Customer customer = s.find( Customer.class, 1 );

					assertTrue( Hibernate.isInitialized( customer ) );

					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(1L) );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class Person {
		@Id
		public Integer id;
		public String name;

		public Person() {
		}

		public Person(Integer id, String name) {
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

		public Employee(Integer id, String name, String employeeCode, String costCenter) {
			super( id, name );
			this.employeeCode = employeeCode;
			this.costCenter = costCenter;
		}
	}

	@Entity( name = "Customer" )
	@Cacheable()
	public static class Customer extends Person {
		public String erpCode;

		public Customer() {
		}

		public Customer(Integer id, String name, String erpCode) {
			super( id, name );
			this.erpCode = erpCode;
		}
	}

}
