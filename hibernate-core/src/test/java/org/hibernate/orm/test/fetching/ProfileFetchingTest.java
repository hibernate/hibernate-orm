package org.hibernate.orm.test.fetching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.EnabledFetchProfile;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		ProfileFetchingTest.Department.class,
		ProfileFetchingTest.Employee.class,
		ProfileFetchingTest.Project.class
})
@SessionFactory
public class ProfileFetchingTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Department department = new Department();
			department.id = 1L;
			entityManager.persist(department);

			Employee employee1 = new Employee();
			employee1.id = 1L;
			employee1.username = "user1";
			employee1.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee1.accessLevel = 0;
			employee1.department = department;
			entityManager.persist(employee1);

			Employee employee2 = new Employee();
			employee2.id = 2L;
			employee2.username = "user2";
			employee2.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee2.accessLevel = 1;
			employee2.department = department;
			entityManager.persist(employee2);

			Project project = new Project();
			project.id = 1L;
			project.employees.add( employee1 );
			employee1.projects.add( project );
			entityManager.persist( project );
		});

		factoryScope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";

			Session session = entityManager;

			//tag::fetching-strategies-dynamic-fetching-profile-example[]
			Employee employee = session.bySimpleNaturalId( Employee.class )
					.enableFetchProfile( "employee.projects" )
					.load( username );
			//end::fetching-strategies-dynamic-fetching-profile-example[]
			assertNotNull( employee );
			assertThat( Hibernate.isInitialized( employee.projects ) ).isTrue();
			assertThat( employee.projects ).hasSize( 1 );
			assertThat( session.isFetchProfileEnabled( "employee.projects" ) ).isFalse();

			session.clear();
			Employee plain = session.bySimpleNaturalId( Employee.class ).load( username );
			assertThat( Hibernate.isInitialized( plain.projects ) ).isFalse();

			session.clear();
			//tag::fetching-natural-id-graph[]
			var graph = session.createEntityGraph( Employee.class );
			graph.addAttributeNode( "projects" );
			Employee graphed = session.bySimpleNaturalId( Employee.class )
					.withFetchGraph( graph )
					.load( username );
			//end::fetching-natural-id-graph[]
			assertThat( Hibernate.isInitialized( graphed.projects ) ).isTrue();
			assertThat( graphed.projects ).hasSize( 1 );

			session.clear();
			Employee byNaturalId = session.byNaturalId( Employee.class )
					.withLoadGraph( graph )
					.using( "username", username )
					.load();
			assertThat( Hibernate.isInitialized( byNaturalId.projects ) ).isTrue();

			session.clear();
			//tag::fetching-profile-find-option[]
			boolean profileAlreadyEnabled = session.isFetchProfileEnabled( "employee.projects.alternative" );
			try {
				Employee withProfile = session.find( Employee.class, 1L,
						new EnabledFetchProfile( "employee.projects.alternative" ) );
				//end::fetching-profile-find-option[]
				assertThat( Hibernate.isInitialized( withProfile.projects ) ).isTrue();
				//tag::fetching-profile-find-option[]
			}
			finally {
				if ( !profileAlreadyEnabled ) {
					session.disableFetchProfile( "employee.projects.alternative" );
				}
			}
			//end::fetching-profile-find-option[]
			assertThat( session.isFetchProfileEnabled( "employee.projects.alternative" ) ).isFalse();

			session.clear();
			//tag::fetching-profile-query[]
			List<Employee> employees = session.createSelectionQuery( "from Employee", Employee.class )
					.enableFetchProfile( "employee.projects.alternative" )
					.getResultList();
			//end::fetching-profile-query[]
			assertThat( employees ).allSatisfy(
					e -> assertThat( Hibernate.isInitialized( e.projects ) ).isTrue() );
			assertThat( session.isFetchProfileEnabled( "employee.projects.alternative" ) ).isFalse();

			session.clear();
			//tag::fetching-profile-session-scope[]
			boolean previouslyEnabled = session.isFetchProfileEnabled( "employee.projects.alternative" );
			session.enableFetchProfile( "employee.projects.alternative" );
			try {
				Employee loaded = session.find( Employee.class, 1L );
				//end::fetching-profile-session-scope[]
				assertThat( Hibernate.isInitialized( loaded.projects ) ).isTrue();
				//tag::fetching-profile-session-scope[]
			}
			finally {
				if ( !previouslyEnabled ) {
					session.disableFetchProfile( "employee.projects.alternative" );
				}
			}
			//end::fetching-profile-session-scope[]
			assertThat( session.isFetchProfileEnabled( "employee.projects.alternative" ) ).isFalse();

			session.clear();
			session.enableFetchProfile( "employee.projects" );
			try {
				Employee withoutProjects = session.find(
						session.createEntityGraph( Employee.class ), 1L, GraphSemantic.FETCH );
				assertThat( Hibernate.isInitialized( withoutProjects.projects ) ).isFalse();
			}
			finally {
				session.disableFetchProfile( "employee.projects" );
			}
		});

	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}

	//tag::fetching-strategies-dynamic-fetching-profile-mapping-example[]
	@Entity(name = "Employee")
	@FetchProfile(
		name = "employee.projects",
		fetchOverrides = {
			@FetchProfile.FetchOverride(
				entity = Employee.class,
				association = "projects",
				mode = FetchMode.JOIN
			)
		}
	)
	//end::fetching-strategies-dynamic-fetching-profile-mapping-example[]
	//tag::fetching-profile-attribute-override[]
	@FetchProfile(name = "employee.projects.alternative")
	public static class Employee {
		//end::fetching-profile-attribute-override[]

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "pswd")
		@ColumnTransformer(
			read = "decrypt('AES', '00', pswd )",
			write = "encrypt('AES', '00', ?)"
		)
		private String password;

		private int accessLevel;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		//tag::fetching-profile-attribute-override[]
		@ManyToMany(mappedBy = "employees")
		@FetchProfileOverride(profile = "employee.projects.alternative", mode = FetchMode.JOIN)
		private List<Project> projects = new ArrayList<>();
		//end::fetching-profile-attribute-override[]

		//Getters and setters omitted for brevity
	//tag::fetching-profile-attribute-override[]
	}
	//end::fetching-profile-attribute-override[]

	@Entity(name = "Project")
	public static class Project {

		@Id
		private Long id;

		@ManyToMany
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}
}
