package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(annotatedClasses = {
		CollectionFetchPaginationWithFiltersTest.Project.class,
		CollectionFetchPaginationWithFiltersTest.ProjectError.class,
		CollectionFetchPaginationWithFiltersTest.Participant.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "false"
))
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationWithFiltersTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			for ( int i = 1; i <= 3; i++ ) {
				final Project project = new Project();
				project.setId( (long) i );
				project.setName( "Project " + i );

				final ProjectError processedError = new ProjectError();
				processedError.setId( (long) (i * 10 + 1) );
				processedError.setProcessed( true );
				processedError.setProject( project );

				final ProjectError unprocessedError = new ProjectError();
				unprocessedError.setId( (long) (i * 10 + 2) );
				unprocessedError.setProcessed( false );
				unprocessedError.setProject( project );

				project.getErrors().add( processedError );
				project.getErrors().add( unprocessedError );

				s.persist( project );
				s.persist( processedError );
				s.persist( unprocessedError );

				if ( i <= 2 ) {
					final Participant participant = new Participant();
					participant.setId( (long) (i * 10) );
					participant.setProject( project );
					s.persist( participant );
				}
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "delete from ProjectError" ).executeUpdate();
			s.createMutationQuery( "delete from Participant" ).executeUpdate();
			s.createMutationQuery( "delete from Project" ).executeUpdate();
		} );
	}

	@Test
	void testPaginationWithCollectionFilter(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT DISTINCT p FROM Project p " +
					"JOIN FETCH p.errors e " +
					"WHERE e.processed = false " +
					"ORDER BY p.id";

			final List<Project> results = s.createQuery( jpql, Project.class )
					.setFirstResult( 1 )
					.setMaxResults( 1 )
					.getResultList();

			assertEquals( 1, results.size(), "Should return 1 result with offset=1, max=1" );
			final Project project = results.get( 0 );
			assertEquals( 2L, project.getId(), "Should return project with ID=2" );

			assertEquals( 1, project.getErrors().size(), "Should have 1 error (unprocessed)" );
			final ProjectError error = project.getErrors().iterator().next();
			assertEquals( 22L, error.getId(), "Should fetch exact child ID=22" );
			assertFalse( error.isProcessed(), "Error should be unprocessed" );

			final List<String> sqlQueries = sql.getSqlQueries();
			assertFalse( sqlQueries.get( 0 ).toLowerCase().contains( "offset" ),
					"Query with collection filter should use in-memory pagination, not SQL OFFSET" );
		} );
	}

	@Test
	void testPaginationWithoutCollectionFilter(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT p FROM Project p " +
					"JOIN FETCH p.errors e " +
					"WHERE p.name LIKE 'Project%' " +
					"ORDER BY p.id";

			s.createQuery( jpql, Project.class )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			final List<String> sqlQueries = sql.getSqlQueries();
			final String generated = sqlQueries.get( 0 ).toLowerCase();

			assertEquals( generated.contains( "from (select" ) || generated.contains( "offset" ),
					true,
					"Query without collection filter should use SQL pagination" );
		} );
	}

	@Test
	void testPaginationWithMixedFilter(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT DISTINCT p FROM Project p " +
					"JOIN FETCH p.errors e " +
					"WHERE e.processed = false AND p.name LIKE 'Project%' " +
					"ORDER BY p.id";

			s.createQuery( jpql, Project.class )
					.setFirstResult( 0 )
					.setMaxResults( 1 )
					.getResultList();

			final List<String> sqlQueries = sql.getSqlQueries();
			assertFalse( sqlQueries.get( 0 ).toLowerCase().contains( "offset" ),
					"Query with mixed filter (collection AND root) should use in-memory pagination" );
		} );
	}

	@Test
	void testPaginationWithLeftJoinFetchAndChildPredicate(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT p FROM Project p " +
					"LEFT JOIN FETCH p.errors e " +
					"WHERE e.processed = false " +
					"ORDER BY p.id";

			final List<Project> results = s.createQuery( jpql, Project.class )
					.setFirstResult( 1 )
					.setMaxResults( 1 )
					.getResultList();

			assertEquals( 1, results.size(), "Should return 1 result with offset=1, max=1" );
			final Project project = results.get( 0 );
			assertEquals( 2L, project.getId(), "Should return project with ID=2" );

			assertEquals( 1, project.getErrors().size(), "Should have 1 error (unprocessed)" );
			final ProjectError error = project.getErrors().iterator().next();
			assertEquals( 22L, error.getId(), "Should fetch exact child ID=22" );
			assertFalse( error.isProcessed(), "Error should be unprocessed" );

			final List<String> sqlQueries = sql.getSqlQueries();
			assertFalse( sqlQueries.get( 0 ).toLowerCase().contains( "offset" ),
					"LEFT JOIN FETCH with child predicate should use in-memory pagination" );
		} );
	}

	@Test
	void testPaginationWithDirectCollectionReference(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT p FROM Project p " +
					"JOIN FETCH p.errors e " +
					"WHERE e is not null " +
					"ORDER BY p.id";

			final List<Project> results = s.createQuery( jpql, Project.class )
					.setFirstResult( 1 )
					.setMaxResults( 1 )
					.getResultList();

			assertEquals( 1, results.size(), "Should return 1 result with offset=1, max=1" );
			final Project project = results.get( 0 );
			assertEquals( 2L, project.getId(), "Should return project with ID=2" );

			assertEquals( 2, project.getErrors().size(), "Should have both errors" );

			final List<String> sqlQueries = sql.getSqlQueries();
			assertFalse( sqlQueries.get( 0 ).toLowerCase().contains( "offset" ),
					"Direct collection reference should use in-memory pagination" );
		} );
	}

	@Test
	void testPaginationWithCorrelatedSubquery(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( s -> {
			sql.clear();

			final String jpql = "SELECT p FROM Project p " +
					"JOIN FETCH p.errors e " +
					"WHERE exists (" +
					"  select 1 from Participant pt " +
					"  where pt.project = p and pt.id <= e.id" +
					") " +
					"ORDER BY p.id";

			final List<Project> results = s.createQuery( jpql, Project.class )
					.setFirstResult( 1 )
					.setMaxResults( 1 )
					.getResultList();

			assertEquals( 1, results.size(), "Should return 1 result with offset=1, max=1" );
			final Project project = results.get( 0 );
			assertEquals( 2L, project.getId(), "Should return project with ID=2" );

			assertEquals( 2, project.getErrors().size(), "Should have both errors" );

			final List<String> sqlQueries = sql.getSqlQueries();
			assertFalse( sqlQueries.get( 0 ).toLowerCase().contains( "offset" ),
					"Correlated subquery referencing collection should use in-memory pagination" );
		} );
	}

	@Entity(name = "Project")
	public static class Project {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "project")
		private Set<ProjectError> errors = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<ProjectError> getErrors() {
			return errors;
		}

		public void setErrors(Set<ProjectError> errors) {
			this.errors = errors;
		}
	}

	@Entity(name = "ProjectError")
	public static class ProjectError {
		@Id
		private Long id;

		private boolean processed;

		@ManyToOne
		private Project project;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean isProcessed() {
			return processed;
		}

		public void setProcessed(boolean processed) {
			this.processed = processed;
		}

		public Project getProject() {
			return project;
		}

		public void setProject(Project project) {
			this.project = project;
		}
	}

	@Entity(name = "Participant")
	public static class Participant {
		@Id
		private Long id;

		@ManyToOne
		private Project project;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Project getProject() {
			return project;
		}

		public void setProject(Project project) {
			this.project = project;
		}
	}
}
