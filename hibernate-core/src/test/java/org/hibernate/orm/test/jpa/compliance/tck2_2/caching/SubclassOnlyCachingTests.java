/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.caching;

import java.util.Map;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SubclassOnlyCachingTests extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testMapping() {
		assertThat(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( Person.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( Employee.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class ).hasCache(),
				CoreMatchers.is( true )
		);
	}

	@Test
	public void testOnlySubclassIsCached() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();

		inTransaction(
				s -> s.persist( new Customer( 1, "Acme Corp", "123" ) )
		);

		assertTrue( sessionFactory().getCache().contains( Customer.class, 1 ) );

		inTransaction(
				s -> {
					statistics.clear();

					final Customer customer = s.get( Customer.class, 1 );

					assertTrue( Hibernate.isInitialized( customer ) );

					assertThat( statistics.getSecondLevelCacheHitCount(), CoreMatchers.is(1L) );
				}
		);
	}

	@After
	public void cleanupData() {
		inTransaction(
				s -> s.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Person.class );
		sources.addAnnotatedClass( Employee.class );
		sources.addAnnotatedClass( Customer.class );
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
