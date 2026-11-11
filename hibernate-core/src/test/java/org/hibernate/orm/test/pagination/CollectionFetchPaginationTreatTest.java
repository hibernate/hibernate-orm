package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for HHH-21099: pagination with collection fetch and treat()
 * in the WHERE clause. The predicate walker must unwrap treated paths to visit
 * the underlying join, otherwise the reference goes undetected and the page
 * offset is applied to the cartesian product instead of the parent rows.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationTreatTest.Project.class,
		CollectionFetchPaginationTreatTest.ProjectError.class,
		CollectionFetchPaginationTreatTest.SpecialError.class
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationTreatTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// Create 3 projects, each with 2 special errors
			for ( int i = 1; i <= 3; i++ ) {
				final Project project = new Project( (long) i, "Project " + i );
				project.addError( new SpecialError( i * 10L + 1, "Error " + i + ".1", "x" ) );
				project.addError( new SpecialError( i * 10L + 2, "Error " + i + ".2", "x" ) );
				s.persist( project );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * With treat() in the WHERE clause, the second page (offset 1, limit 1)
	 * should return project 2 with both of its special errors. Before the fix,
	 * the treated path was not unwrapped during the predicate walk, so the
	 * reference to the fetched join went undetected and the offset/limit were
	 * applied to the joined rows instead of the parent.
	 */
	@Test
	void treatOnFetchedJoinIsDetectedInPredicate(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final List<Project> projects = s.createSelectionQuery(
					"from Project p join fetch p.errors e "
							+ "where treat(e as SpecialError).code = 'x' "
							+ "order by p.id",
					Project.class
			).setFirstResult( 1 ).setMaxResults( 1 ).list();

			// Should get project 2 (the second matching project)
			assertEquals( 1, projects.size() );
			final Project project = projects.get( 0 );
			assertEquals( 2L, project.getId() );
			assertEquals( "Project 2", project.getName() );

			// And it should have both of its special errors fetched
			assertEquals( 2, project.getErrors().size() );
			for ( ProjectError error : project.getErrors() ) {
				assertTrue( error instanceof SpecialError );
				assertEquals( "x", ((SpecialError) error).getCode() );
				assertTrue( error.getMessage().contains( "Error 2" ) );
			}
		} );
	}

	/**
	 * When the strict setting is enabled, the same query should throw because
	 * it has pagination over a collection fetch.
	 */
	@Test
	@ServiceRegistry(settings = @Setting(
			name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
			value = "true"
	))
	void strictSettingDetectsTreatReferenceToo(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			try {
				s.createSelectionQuery(
						"from Project p join fetch p.errors e "
								+ "where treat(e as SpecialError).code = 'x' "
								+ "order by p.id",
						Project.class
				).setFirstResult( 1 ).setMaxResults( 1 ).list();
				throw new AssertionError(
						"Expected exception when strict pagination check is enabled"
				);
			}
			catch ( IllegalArgumentException expected ) {
				// The check should fire because the WHERE clause references the
				// fetched collection join (through the treated path)
				assertTrue(
						expected.getMessage().contains( "fetch" )
								|| expected.getMessage().contains( "pagination" ),
						"Expected message about pagination over collection fetch, got: "
								+ expected.getMessage()
				);
			}
		} );
	}

	@Entity(name = "Project")
	@Table(name = "project")
	public static class Project {
		@Id
		private Long id;
		private String name;

		@OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
		private List<ProjectError> errors = new ArrayList<>();

		public Project() {
		}

		public Project(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<ProjectError> getErrors() {
			return errors;
		}

		public void addError(ProjectError error) {
			errors.add( error );
			error.setProject( this );
		}
	}

	@Entity(name = "ProjectError")
	@Table(name = "project_error")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "DTYPE")
	@DiscriminatorValue("ProjectError")
	public static class ProjectError {
		@Id
		private Long id;
		private String message;

		@ManyToOne
		@JoinColumn(name = "project_id")
		private Project project;

		public ProjectError() {
		}

		public ProjectError(Long id, String message) {
			this.id = id;
			this.message = message;
		}

		public Long getId() {
			return id;
		}

		public String getMessage() {
			return message;
		}

		public Project getProject() {
			return project;
		}

		public void setProject(Project project) {
			this.project = project;
		}
	}

	@Entity(name = "SpecialError")
	@DiscriminatorValue("SpecialError")
	public static class SpecialError extends ProjectError {
		private String code;

		public SpecialError() {
		}

		public SpecialError(Long id, String message, String code) {
			super( id, message );
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}
