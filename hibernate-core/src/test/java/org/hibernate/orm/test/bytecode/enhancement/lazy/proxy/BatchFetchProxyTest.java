/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */

@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				BatchFetchProxyTest.Employee.class,
				BatchFetchProxyTest.Employer.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.SHOW_SQL, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class BatchFetchProxyTest {

	private static int NUMBER_OF_ENTITIES = 20;

	@Test
	@JiraKey("HHH-11147")
	public void testBatchAssociationFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
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
					assertThat( statistics.getEntityLoadCount() ).isEqualTo( 40L );
					// but assert that it only took 3 queries (the initial plus the 2 batch fetches)
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 3L );
				}
		);
	}

	@Test
	@JiraKey("HHH-11147")
	public void testBatchAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
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
	@JiraKey("HHH-11147")
	public void testBatchEntityLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
					statistics.clear();

					List<Employer> employers = new ArrayList<>();
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						employers.add( session.getReference( Employer.class, i + 1) );
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
	@JiraKey("HHH-11147")
	public void testBatchEntityLoadThenModify(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
					statistics.clear();

					List<Employer> employers = new ArrayList<>();
					for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
						employers.add( session.getReference( Employer.class, i + 1) );
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

		scope.inTransaction(
				session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						final Employer employer = session.get( Employer.class, i + 1 );
						assertEquals( "Employer #" + employer.id + " new", employer.name );
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-11147")
	public void testBatchEntityRemoval(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
					statistics.clear();

					List<Employer> employers = new ArrayList<>();
					for ( int i = 0 ; i < 5 ; i++ ) {
						employers.add( session.getReference( Employer.class, i + 1) );
					}

					assertEquals( 0, statistics.getPrepareStatementCount() );

					session.find( Employer.class, 0 );
					session.find( Employer.class, 1 );
					session.find( Employer.class, 2 );
					session.find( Employer.class, 5 );

					assertEquals( 1, statistics.getPrepareStatementCount() );

					session.find( Employer.class, 6 );
					session.find( Employer.class, 7 );

					assertEquals( 3, statistics.getPrepareStatementCount() );

					for ( Employer employer : employers ) {
						assertTrue( Hibernate.isInitialized( employer ) );
					}
				}
		);
	}

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction(
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

	@AfterEach
	public void cleanupDate(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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
