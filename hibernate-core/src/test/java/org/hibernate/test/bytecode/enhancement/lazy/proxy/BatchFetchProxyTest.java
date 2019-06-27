/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */

@TestForIssue(jiraKey = "HHH-11147")
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class BatchFetchProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	private static int NUMBER_OF_ENTITIES = 20;

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testBatchAssociationFetch() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();
					List<Employee> employees = session.createQuery( "from Employee", Employee.class ).getResultList();

					assertEquals( 1, statistics.getPrepareStatementCount() );
					assertEquals( NUMBER_OF_ENTITIES, employees.size() );

					for ( int i = 0; i < employees.size(); i++ ) {
						final Employer employer = employees.get( i ).employer;
						if ( i % 10 == 0 ) {
							assertFalse( Hibernate.isInitialized( employer ) );
							Hibernate.initialize( employer );
						}
						else {
							assertTrue( Hibernate.isInitialized( employer ) );
						}
						assertEquals( "Employer #" + employer.id, employer.name );
					}

					// assert that all 20 Employee and all 20 Employers have been loaded
					assertThat( statistics.getEntityLoadCount(), is( 40L ) );
					// but assert that it only took 3 queries (the initial plus the 2 batch fetches)
					assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testBatchAssociation() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();
					List<Employee> employees = session.createQuery( "from Employee", Employee.class ).getResultList();

					assertEquals( 1, statistics.getPrepareStatementCount() );
					assertEquals( NUMBER_OF_ENTITIES, employees.size() );

					for ( int i = 0 ; i < employees.size() ; i++ ) {
						final Employer employer = employees.get( i ).employer;
						if ( i % 10 == 0 ) {
							assertFalse( Hibernate.isInitialized( employer ) );
							Hibernate.initialize( employer );
						}
						else {
							assertTrue( Hibernate.isInitialized( employer ) );
						}
						assertEquals( "Employer #" + employer.id, employer.name );
					}

					assertEquals( 3, statistics.getPrepareStatementCount() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testBatchEntityLoad() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();

					List<Employer> employers = new ArrayList<>();
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						employers.add( session.load( Employer.class, i + 1) );
					}

					assertEquals( 0, statistics.getPrepareStatementCount() );

					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						final Employer employer = employers.get( i );
						if ( i % 10 == 0 ) {
							assertFalse( Hibernate.isInitialized( employer ) );
							Hibernate.initialize( employer );
						}
						else {
							assertTrue( Hibernate.isInitialized( employer ) );
						}
						assertEquals( "Employer #" + employer.id, employer.name );
					}

					assertEquals( 2, statistics.getPrepareStatementCount() );
				}
		);
	}


	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testBatchEntityLoadThenModify() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();

					List<Employer> employers = new ArrayList<>();
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						employers.add( session.load( Employer.class, i + 1) );
					}

					assertEquals( 0, statistics.getPrepareStatementCount() );

					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						final Employer employer = employers.get( i );
						if ( i % 10 == 0 ) {
							assertFalse( Hibernate.isInitialized( employer ) );
							Hibernate.initialize( employer );
						}
						else {
							assertTrue( Hibernate.isInitialized( employer ) );
						}
						assertEquals( "Employer #" + employer.id, employer.name );
						employer.name = employer.name + " new";
					}

					assertEquals( 2, statistics.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						final Employer employer = session.get( Employer.class, i + 1 );
						assertEquals( "Employer #" + employer.id + " new", employer.name );
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
		inTransaction(
				session -> {
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
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
		inTransaction(
				session -> {
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
		ssrb.applySetting( AvailableSettings.SHOW_SQL, true );
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
	@BatchSize(size = 10)
	public static class Employer {
		@Id
		private int id;

		private String name;
	}
}