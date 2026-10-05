package org.hibernate.orm.test.stateless.fetching;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Stateless queries reuse entity instances within a single result set.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		StatelessSessionIdentityReuseTest.Document.class,
		StatelessSessionIdentityReuseTest.Chapter.class
})
@SessionFactory
@JiraKey("HHH-2564")
public class StatelessSessionIdentityReuseTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Document document = new Document();
			document.id = 1L;
			session.persist( document );
			for ( long id = 1; id <= 2; id++ ) {
				Chapter chapter = new Chapter();
				chapter.id = id;
				chapter.document = document;
				document.chapters.add( chapter );
				session.persist( chapter );
			}
		} );
	}

	@Test
	void testJoinWithoutFetch(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			List<Document> documents = session.createQuery(
					"select d from IdentityDocument d left join d.chapters c order by c.id", Document.class
			).getResultList();
			assertEquals( 1, documents.size() );
			assertEquals( 1L, documents.get( 0 ).id );
			assertFalse( Hibernate.isInitialized( documents.get( 0 ).chapters ) );
		} );
	}

	@Test
	void testIdentityReuseAcrossTupleRows(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			List<Object[]> rows = session.createQuery(
					"select d, c.id from IdentityDocument d left join d.chapters c order by c.id", Object[].class
			).getResultList();
			assertEquals( 2, rows.size() );
			assertEquals( 1L, rows.get( 0 )[1] );
			assertEquals( 2L, rows.get( 1 )[1] );
			assertEquals( 1L, ( (Document) rows.get( 0 )[0] ).id );
			assertSame( rows.get( 0 )[0], rows.get( 1 )[0] );
		} );
	}

	@Test
	void testCollectionFetch(SessionFactoryScope scope) {
		assertFetchedGraph( scope, "select d from IdentityDocument d left join fetch d.chapters c" );
	}

	@Test
	void testCollectionAndOwnerFetch(SessionFactoryScope scope) {
		assertFetchedGraph( scope,
				"select d from IdentityDocument d left join fetch d.chapters c left join fetch c.document" );
	}

	private void assertFetchedGraph(SessionFactoryScope scope, String hql) {
		scope.inStatelessTransaction( session -> {
			List<Document> documents = session.createQuery( hql, Document.class ).getResultList();
			assertEquals( 1, documents.size() );
			Document document = documents.get( 0 );
			assertEquals( 1L, document.id );
			assertTrue( Hibernate.isInitialized( document.chapters ) );
			assertEquals( 2, document.chapters.size() );
			assertEquals( List.of( 1L, 2L ), document.chapters.stream().map( chapter -> chapter.id ).sorted().toList() );
			for ( Chapter chapter : document.chapters ) {
				assertTrue( Hibernate.isInitialized( chapter ) );
				assertSame( document, chapter.document );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "IdentityDocument")
	@Table(name = "stateless_identity_document")
	public static class Document {
		@Id
		Long id;

		@OneToMany(mappedBy = "document")
		List<Chapter> chapters = new ArrayList<>();
	}

	@Entity(name = "IdentityChapter")
	@Table(name = "stateless_identity_chapter")
	public static class Chapter {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Document document;
	}
}
