/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class LoadANonExistingEntityTest extends BaseNonConfigCoreFunctionalTestCase {

	private static int NUMBER_OF_ENTITIES = 20;

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testInitilaizeNonExistingEntity() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					Employer nonExisting = session.load( Employer.class, -1 );
					assertEquals( 0, statistics.getPrepareStatementCount() );
					assertFalse( Hibernate.isInitialized( nonExisting ) );
					try {
						Hibernate.initialize( nonExisting );
						fail( "should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException ex) {
						// expected
						assertEquals( 1, statistics.getPrepareStatementCount() );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testSetFieldNonExistingEntity() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		doInHibernate(
				this::sessionFactory, session -> {
					Employer nonExisting = session.load( Employer.class, -1 );
					assertEquals( 0, statistics.getPrepareStatementCount() );
					assertFalse( Hibernate.isInitialized( nonExisting ) );
					try {
						nonExisting.setName( "Fab" );
						fail( "should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException ex) {
						// expected
						assertEquals( 1, statistics.getPrepareStatementCount() );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testGetFieldNonExistingEntity() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		doInHibernate(
				this::sessionFactory, session -> {
					Employer nonExisting = session.load( Employer.class, -1 );
					assertEquals( 0, statistics.getPrepareStatementCount() );
					assertFalse( Hibernate.isInitialized( nonExisting ) );
					try {
						nonExisting.getName();
						fail( "should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException ex) {
						// expected
						assertEquals( 1, statistics.getPrepareStatementCount() );
					}
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
				Employer.class
		};
	}

	@Before
	public void setUpData() {
		doInHibernate(
				this::sessionFactory, session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						final Employee employee = new Employee();
						employee.id = i + 1;
						employee.name = "Employee #" + employee.id;
						final Employer employer = new Employer();
						employer.id = i + 1;
						employer.name = "Employer #" + employer.id;
						employee.employer = employer;
						session.persist( employee );
					}
				}
		);
	}

	@After
	public void cleanupDate() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Employee" ).executeUpdate();
					session.createQuery( "delete from Employer" ).executeUpdate();
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Employer employer;
	}

	@Entity(name = "Employer")
	public static class Employer {
		@Id
		private int id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
