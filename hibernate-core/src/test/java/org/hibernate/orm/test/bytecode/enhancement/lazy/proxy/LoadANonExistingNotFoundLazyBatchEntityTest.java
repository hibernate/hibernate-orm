/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

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

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 * @author Gail Badner
 */
@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				LoadANonExistingNotFoundLazyBatchEntityTest.Employee.class,
				LoadANonExistingNotFoundLazyBatchEntityTest.Employer.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class LoadANonExistingNotFoundLazyBatchEntityTest {

	private static final int NUMBER_OF_ENTITIES = 20;

	@Test
	@JiraKey("HHH-11147")
	public void loadEntityWithNotFoundAssociation(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( (session) -> {
			List<Employee> employees = new ArrayList<>( NUMBER_OF_ENTITIES );
			for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
				employees.add( session.getReference( Employee.class, i + 1 ) );
			}
			for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
				Hibernate.initialize( employees.get( i ) );
				assertNull( employees.get( i ).employer );
			}
		} );

		// A "not found" association cannot be batch fetched because
		// Employee#employer must be initialized immediately.
		// Enhanced proxies (and HibernateProxy objects) should never be created
		// for a "not found" association.
		assertEquals( 2 * NUMBER_OF_ENTITIES, statistics.getPrepareStatementCount() );
	}

	@Test
	@JiraKey("HHH-11147")
	public void getEntityWithNotFoundAssociation(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( (session) -> {
			for ( int i = 0 ; i < NUMBER_OF_ENTITIES ; i++ ) {
				Employee employee = session.get( Employee.class, i + 1 );
				assertNull( employee.employer );
			}
		} );

		// A "not found" association cannot be batch fetched because
		// Employee#employer must be initialized immediately.
		// Enhanced proxies (and HibernateProxy objects) should never be created
		// for a "not found" association.
		assertEquals( 2 * NUMBER_OF_ENTITIES, statistics.getPrepareStatementCount() );
	}

	@Test
	@JiraKey("HHH-11147")
	public void updateNotFoundAssociationWithNew(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( (session) -> {
			for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
				Employee employee = session.get( Employee.class, i + 1 );
				Employer employer = new Employer();
				employer.id = 2 * employee.id;
				employer.name = "Employer #" + employer.id;
				employee.employer = employer;
			}
		} );

		scope.inTransaction( (session) -> {
			for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
				Employee employee = session.get( Employee.class, i + 1 );
				assertTrue( Hibernate.isInitialized( employee.employer ) );
				assertEquals( employee.id * 2, employee.employer.id );
				assertEquals( "Employer #" + employee.employer.id, employee.employer.name );
			}
		} );
	}

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					for ( int i = 0; i < NUMBER_OF_ENTITIES; i++ ) {
						final Employee employee = new Employee();
						employee.id = i + 1;
						employee.name = "Employee #" + employee.id;
						session.persist( employee );
					}
				}
		);


		scope.inTransaction( session -> {
					// Add "not found" associations
					session.createNativeQuery( "update Employee set employer_id = id" ).executeUpdate();
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
		@JoinColumn(name = "employer_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
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
