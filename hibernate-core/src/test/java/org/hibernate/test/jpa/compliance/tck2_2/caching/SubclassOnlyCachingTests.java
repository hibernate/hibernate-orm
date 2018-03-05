/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2.caching;

import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SubclassOnlyCachingTests extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testMapping() {
		assertThat(
				sessionFactory().getMetamodel().entityPersister( Person.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				sessionFactory().getMetamodel().entityPersister( Employee.class ).hasCache(),
				CoreMatchers.is( false )
		);
		assertThat(
				sessionFactory().getMetamodel().entityPersister( Customer.class ).hasCache(),
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
	protected void addSettings(Map settings) {
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
