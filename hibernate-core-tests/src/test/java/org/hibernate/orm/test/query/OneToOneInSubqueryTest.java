package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.Expression;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		OneToOneInSubqueryTest.Project.class,
		OneToOneInSubqueryTest.Audit.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-20179" )
public class OneToOneInSubqueryTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var wantedAudit = new Audit( 91L, "wanted" );
			final var otherAudit = new Audit( 17L, "other" );
			session.persist( wantedAudit );
			session.persist( otherAudit );
			// Comparing audit_id with project IDs would select the other project.
			session.persist( new Project( 17L, wantedAudit ) );
			session.persist( new Project( 91L, otherAudit ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void testRootInSubquerySelectingInverseAssociation(boolean explicitJoin, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Project.class );
			final var project = query.from( Project.class );
			final var subquery = query.subquery( Project.class );
			final var audit = subquery.from( Audit.class );
			final Expression<Project> selection = explicitJoin ? audit.join( "project" ) : audit.get( "project" );
			subquery.select( selection ).where( cb.equal( audit.get( "name" ), "wanted" ) );
			query.select( project ).where( project.in( subquery ) );

			assertThat( session.createQuery( query ).getResultList() )
					.extracting( result -> result.id )
					.containsExactly( 17L );
		} );
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void testRootInSubquerySelectingOwningAssociation(boolean explicitJoin, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Audit.class );
			final var audit = query.from( Audit.class );
			final var subquery = query.subquery( Audit.class );
			final var project = subquery.from( Project.class );
			final Expression<Audit> selection = explicitJoin ? project.join( "audit" ) : project.get( "audit" );
			subquery.select( selection ).where( cb.equal( project.get( "id" ), 17L ) );
			query.select( audit ).where( audit.in( subquery ) );

			assertThat( session.createQuery( query ).getResultList() )
					.extracting( result -> result.id )
					.containsExactly( 91L );
		} );
	}

	@Test
	void testIdInSubquerySelectingId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Project.class );
			final var project = query.from( Project.class );
			final var subquery = query.subquery( Long.class );
			final var audit = subquery.from( Audit.class );
			subquery.select( audit.join( "project" ).get( "id" ) )
					.where( cb.equal( audit.get( "name" ), "wanted" ) );
			query.select( project ).where( project.get( "id" ).in( subquery ) );

			assertThat( session.createQuery( query ).getResultList() )
					.extracting( result -> result.id )
					.containsExactly( 17L );
		} );
	}

	@Test
	void testHqlRootEqualsInverseAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select p from Project p, Audit a where p = a.project and a.name = :name",
				Project.class
		).setParameter( "name", "wanted" ).getResultList() )
				.extracting( result -> result.id )
				.containsExactly( 17L ) );
	}

	@Test
	void testHqlRootInSubquerySelectingInverseAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"from Project p where p in (select a.project from Audit a where a.name = :name)",
				Project.class
		).setParameter( "name", "wanted" ).getResultList() )
				.extracting( result -> result.id )
				.containsExactly( 17L ) );
	}

	@MappedSuperclass
	public static abstract class BaseModel {
		@Id
		Long id;
	}

	@Entity( name = "Audit" )
	@Table( name = "audits" )
	public static class Audit extends BaseModel {
		String name;

		@OneToOne( mappedBy = "audit" )
		Project project;

		public Audit() {
		}

		Audit(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Project" )
	@Table( name = "projects" )
	public static class Project extends BaseModel {
		@OneToOne
		@JoinColumn( name = "audit_id" )
		Audit audit;

		public Project() {
		}

		Project(Long id, Audit audit) {
			this.id = id;
			this.audit = audit;
			audit.project = this;
		}
	}
}
