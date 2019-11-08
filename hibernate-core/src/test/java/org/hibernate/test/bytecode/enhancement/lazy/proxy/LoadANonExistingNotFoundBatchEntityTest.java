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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class LoadANonExistingNotFoundBatchEntityTest extends BaseNonConfigCoreFunctionalTestCase {

	private static int NUMBER_OF_ENTITIES = 20;

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void loadEntityWithNotFoundAssociation() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					List<Employee> employees = new ArrayList<>( NUMBER_OF_ENTITIES );
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						employees.add( session.load( Employee.class, i + 1 ) );
					}
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						Hibernate.initialize( employees.get( i ) );
						assertNull( employees.get( i ).employer );
					}
				}
		);

		// A "not found" association cannot be batch fetched because
		// Employee#employer must be initialized immediately.
		// Enhanced proxies (and HibernateProxy objects) should never be created
		// for a "not found" association.
		assertEquals( 2 * NUMBER_OF_ENTITIES, statistics.getPrepareStatementCount() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void getEntityWithNotFoundAssociation() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						Employee employee = session.get( Employee.class, i + 1 );
						assertNull( employee.employer );
					}
				}
		);

		// A "not found" association cannot be batch fetched because
		// Employee#employer must be initialized immediately.
		// Enhanced proxies (and HibernateProxy objects) should never be created
		// for a "not found" association.
		assertEquals( 2 * NUMBER_OF_ENTITIES, statistics.getPrepareStatementCount() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void updateNotFoundAssociationWithNew() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						Employee employee = session.get( Employee.class, i + 1 );
						Employer employer = new Employer();
						employer.id = 2 * employee.id;
						employer.name = "Employer #" + employer.id;
						employee.employer = employer;
					}
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						Employee employee = session.get( Employee.class, i + 1 );
						assertTrue( Hibernate.isInitialized( employee.employer ) );
						assertEquals( employee.id * 2, employee.employer.id );
						assertEquals( "Employer #" + employee.employer.id, employee.employer.name );
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
						session.persist( employee );
					}
				}
		);


		doInHibernate(
				this::sessionFactory, session -> {
					// Add "not found" associations
					session.createQuery( "update Employee set employer_id = id" ).executeUpdate();
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
		@JoinColumn(foreignKey = @ForeignKey(value= ConstraintMode.NO_CONSTRAINT))
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@NotFound(action=NotFoundAction.IGNORE)
		private Employer employer;
	}

	@Entity(name = "Employer")
	@BatchSize(size = 10)
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
