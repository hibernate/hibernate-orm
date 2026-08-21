/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
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
				LoadANonExistingNotFoundEntityTest.Employee.class,
				LoadANonExistingNotFoundEntityTest.Employer.class
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
public class LoadANonExistingNotFoundEntityTest {

	@Test
	@JiraKey("HHH-11147")
	public void loadEntityWithNotFoundAssociation(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
					Employee employee = session.getReference( Employee.class, 1 );
					Hibernate.initialize( employee );
					assertNull( employee.employer );
				}
		);

		// we should get 1 query for the Employee with join
		assertEquals( 1, statistics.getPrepareStatementCount() );
	}

	@Test
	@JiraKey("HHH-11147")
	public void getEntityWithNotFoundAssociation(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
					Employee employee = session.get( Employee.class, 1 );
					assertNull( employee.employer );
				}
		);

		// we should get 1 query for the Employee with join
		assertEquals( 1, statistics.getPrepareStatementCount() );
	}

	@Test
	@JiraKey("HHH-11147")
	public void updateNotFoundAssociationWithNew(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
					Employee employee = session.get( Employee.class, 1 );
					Employer employer = new Employer();
					employer.id = 2 * employee.id;
					employer.name = "Employer #" + employer.id;
					employee.employer = employer;
				}
		);

		scope.inTransaction( session -> {
					Employee employee = session.get( Employee.class, 1 );
					assertTrue( Hibernate.isInitialized( employee.employer ) );
					assertEquals( employee.id * 2, employee.employer.id );
					assertEquals( "Employer #" + employee.employer.id, employee.employer.name );
				}
		);
	}

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					final Employee employee = new Employee();
					employee.id = 1;
					employee.name = "Employee #" + employee.id;
					session.persist( employee );
				}
		);


		scope.inTransaction( session -> {
					// Add "not found" associations
					session.createNativeQuery( "update Employee set employer_id = 0 ").executeUpdate();
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

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name = "employer_id",foreignKey = @ForeignKey(value= ConstraintMode.NO_CONSTRAINT))
		@NotFound(action=NotFoundAction.IGNORE)
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
