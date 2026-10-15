package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedSecondaryTableTest.Project.class,
		RestrictedSecondaryTableTest.Document.class })
@SessionFactory
@JiraKey("HHH-12016")
class RestrictedSecondaryTableTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long projectId = 1; projectId <= 2; projectId++ ) {
				final Project project = new Project();
				project.id = projectId;
				session.persist( project );
				for ( long id = projectId == 1 ? 1 : 4; id <= (projectId == 1 ? 3 : 4); id++ ) {
					final Document document = new Document();
					document.id = id;
					document.project = project;
					// Document 3 has no optional secondary-table row.
					if ( id != 3 ) {
						document.active = id == 1 ? 1 : 0;
						document.published = 1;
					}
					session.persist( document );
					project.documents.add( document );
					project.linkedDocuments.add( document );
				}
			}
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select count(*) from restricted_document_details where id = 3", Long.class )
				.getSingleResult() ).isZero() );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "query", "fetch", "graph" })
	void secondaryTableColumnRestriction(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Project> projects;
			switch ( loading ) {
				case "find" -> projects = List.of(
						session.find( Project.class, 1L ), session.find( Project.class, 2L ) );
				case "query" -> projects = session.createQuery(
						"from Project order by id", Project.class ).getResultList();
				case "fetch" -> projects = session.createQuery(
						"from Project p left join fetch p.documents left join fetch p.linkedDocuments order by p.id",
						Project.class ).getResultList();
				case "graph" -> {
					final var graph = session.createEntityGraph( Project.class );
					graph.addAttributeNodes( "documents", "linkedDocuments" );
					projects = List.of( session.find( graph, 1L ), session.find( graph, 2L ) );
				}
				default -> throw new AssertionError( loading );
			}
			assertThat( projects ).extracting( p -> p.id ).containsExactly( 1L, 2L );
			assertThat( projects.get( 0 ).documents ).extracting( d -> d.id ).containsExactly( 1L );
			assertThat( projects.get( 0 ).linkedDocuments ).extracting( d -> d.id ).containsExactly( 1L );
			assertThat( projects.get( 1 ).documents ).isEmpty();
			assertThat( projects.get( 1 ).linkedDocuments ).isEmpty();
		} );
	}

	@Entity(name = "Project")
	@Table(name = "restricted_secondary_project")
	static class Project {
		@Id Long id;
		@OneToMany(mappedBy = "project")
		@SQLRestriction("ACTIVE = 1 and `published document` = 1")
		List<Document> documents = new ArrayList<>();
		@ManyToMany
		@JoinTable(name = "restricted_document_link")
		@SQLRestriction("ACTIVE = 1 and `published document` = 1")
		Set<Document> linkedDocuments = new HashSet<>();
	}

	@Entity(name = "Document")
	@Table(name = "restricted_secondary_document")
	@SecondaryTable(name = "restricted_document_details")
	static class Document {
		@Id Long id;
		@ManyToOne(fetch = FetchType.LAZY) Project project;
		@Column(table = "restricted_document_details")
		Integer active;
		@Column(name = "`published document`", table = "restricted_document_details")
		Integer published;
	}
}
