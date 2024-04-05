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
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				LoadANonExistingEntityTest.Employee.class,
				LoadANonExistingEntityTest.Employer.class
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
public class LoadANonExistingEntityTest {

	private static int NUMBER_OF_ENTITIES = 20;

	@Test
	@JiraKey("HHH-11147")
	public void testInitilaizeNonExistingEntity(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
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
	@JiraKey("HHH-11147")
	public void testSetFieldNonExistingEntity(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
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
	@JiraKey("HHH-11147")
	public void testGetFieldNonExistingEntity(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
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

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

	@AfterEach
	public void cleanupDate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					session.createQuery( "delete from Employee" ).executeUpdate();
					session.createQuery( "delete from Employer" ).executeUpdate();
				}
		);
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
