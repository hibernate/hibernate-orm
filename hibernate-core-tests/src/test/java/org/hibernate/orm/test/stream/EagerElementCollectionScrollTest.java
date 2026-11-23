package org.hibernate.orm.test.stream;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies an eager element collection is available after reading the first scroll result and closing the session.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = EagerElementCollectionScrollTest.Employee.class)
@SessionFactory
@JiraKey("HHH-15506")
public class EagerElementCollectionScrollTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var employee = new Employee();
			employee.id = 1;
			employee.projectCodes.addAll( Set.of( "UI", "SERVER" ) );
			session.persist( employee );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ScrollEmployee" ).executeUpdate() );
	}

	@Test
	void testEagerElementCollectionAfterScrollAndClose(SessionFactoryScope scope) {
		final Employee employee = scope.fromTransaction( session -> {
			try ( var results = session.createQuery( "from ScrollEmployee", Employee.class ).scroll() ) {
				assertTrue( results.next() );
				return results.get();
			}
		} );

		assertThat( employee.getId() ).isEqualTo( 1 );
		assertTrue( Hibernate.isInitialized( employee.getProjectCodes() ) );
		assertThat( employee.getProjectCodes() ).containsExactlyInAnyOrder( "UI", "SERVER" );
	}

	@Entity(name = "ScrollEmployee")
	@Table(name = "hhh15506_employee")
	public static class Employee {
		private Integer id;
		private Set<String> projectCodes = new HashSet<>();

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable(name = "hhh15506_project_code", joinColumns = @JoinColumn(name = "employee_id"))
		@Column(name = "project_code")
		public Set<String> getProjectCodes() {
			return projectCodes;
		}

		public void setProjectCodes(Set<String> projectCodes) {
			this.projectCodes = projectCodes;
		}
	}
}
