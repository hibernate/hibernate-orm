package org.hibernate.orm.test.query.criteria;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.orm.test.query.criteria.QueryGuideTests_.Book_;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.sql.ResultSetMapping.entity;
import static jakarta.persistence.sql.ResultSetMapping.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Executable examples for the User Guide query chapters.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = QueryGuideTests.Book.class)
@SessionFactory
public class QueryGuideTests {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createStatement( "delete GuideBook" ).execute();
			for ( long id = 1; id <= 3; id++ ) {
				Book book = new Book();
				book.id = id;
				book.title = "Hibernate " + id;
				session.persist( book );
			}
		} );
	}

	@Test
	void selection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::query-selection[]
			List<Book> books = entityManager.createQuery(
					"from GuideBook where title like :pattern order by id", Book.class )
					.setParameter( "pattern", "Hibernate%" )
					.getResultList();
			//end::query-selection[]
			assertEquals( 3, books.size() );
		} );
	}

	@Test
	void mutationSynchronizesManagedState(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			Book managed = entityManager.find( Book.class, 1L );
			//tag::query-mutation[]
			entityManager.flush();
			int affected = entityManager.createStatement(
					"update GuideBook set title = :title where id = :id" )
					.setParameter( "title", "Updated" )
					.setParameter( "id", 1L )
					.execute();
			entityManager.clear();
			//end::query-mutation[]
			assertEquals( 1, affected );
			assertEquals( "Hibernate 1", managed.title );
			assertEquals( "Updated", entityManager.find( Book.class, 1L ).title );
		} );
	}

	@Test
	void criteriaMutations(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::criteria-mutations[]
			var builder = entityManager.getCriteriaBuilder();
			var update = builder.createCriteriaUpdate( Book.class );
			var target = update.from( Book.class );
			update.set( Book_.title, "Updated" )
					.where( builder.equal( target.get( Book_.id ), 1L ) );
			int updated = entityManager.createStatement( update ).execute();

			var delete = builder.createCriteriaDelete( Book.class );
			var deletedRoot = delete.from( Book.class );
			delete.where( builder.greaterThan( deletedRoot.get( Book_.id ), 1L ) );
			int deleted = entityManager.createStatement( delete ).execute();
			//end::criteria-mutations[]
			assertEquals( 1, updated );
			assertEquals( 2, deleted );
			assertEquals( "Updated", entityManager.find( Book.class, 1L ).title );
		} );
	}

	@Test
	void keyPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::query-key-pagination[]
			var page = Page.first( 2 ).keyedBy( Order.asc( Book_.id ) );
			var query = session.createSelectionQuery( "from GuideBook", Book.class );
			var first = query.getKeyedResultList( page );
			var second = query.getKeyedResultList( first.getNextPage() );
			//end::query-key-pagination[]
			assertEquals( List.of( 1L, 2L ), first.getResultList().stream().map( book -> book.id ).toList() );
			assertEquals( List.of( 3L ), second.getResultList().stream().map( book -> book.id ).toList() );
			assertFalse( first.isLastPage() );
			assertTrue( second.isLastPage() );
		} );
	}

	@Test
	void specifications(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::query-specification[]
			var specification = SelectionSpecification.create( Book.class )
					.restrict( Restriction.greaterThan( Book_.id, 1L ) )
					.sort( Order.desc( Book_.id ) );
			List<Book> books = specification.createQuery( session ).getResultList();
			//end::query-specification[]
			assertEquals( List.of( 3L, 2L ), books.stream().map( book -> book.id ).toList() );
			//tag::query-specification-projections[]
				specification.resort( List.of() );
				long count = specification.createCountProjection().createQuery( session ).getSingleResult();
				boolean exists = specification.createExistsProjection().createQuery( session ).getSingleResult();
			var projection = specification.sort( Order.desc( Book_.id ) ).createProjection();
			var title = projection.select( Book_.title );
			var rows = projection.createQuery( session ).getResultList();
			List<String> titles = rows.stream().map( title::in ).toList();
			//end::query-specification-projections[]
			assertEquals( 2, count );
			assertTrue( exists );
			assertEquals( List.of( "Hibernate 3", "Hibernate 2" ), titles );
		} );
	}

	@Test
	void specificationSourcesAndValidation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::query-specification-sources[]
			var builder = session.getCriteriaBuilder();
			var criteria = builder.createQuery( Book.class );
			var book = criteria.from( Book.class );
			criteria.select( book ).where( builder.greaterThan( book.get( Book_.id ), 1L ) );
			var fromCriteria = SelectionSpecification.create( criteria );
			fromCriteria.validate( builder );
			var rebuilt = fromCriteria.buildCriteria( builder );
			var reference = fromCriteria.reference();
			var fromReference = SelectionSpecification.create( reference );
			//end::query-specification-sources[]
			assertEquals( 2, session.createQuery( rebuilt ).getResultList().size() );
			assertEquals( 2, fromReference.createQuery( session ).getResultList().size() );
		} );
	}

	@Test
	void specificationMutation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::query-specification-mutation[]
			var mutation = MutationSpecification.create( Book.class, "delete GuideBook" )
					.restrict( Restriction.greaterThan( Book_.id, 1L ) );
			int affected = mutation.createQuery( session ).execute();
			//end::query-specification-mutation[]
			assertEquals( 2, affected );
			assertEquals( 1L, session.createQuery( "select count(*) from GuideBook", Long.class ).getSingleResult() );
		} );
	}

	@Test
	void nativeResultMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::query-native-mapping[]
			var mapping = entity( Book.class,
					field( Book_.id, "book_id" ),
					field( Book_.title, "book_title" ) );
			List<Book> books = entityManager.createNativeQuery(
					"select id as book_id, title as book_title from query_guide_book order by id", mapping )
					.getResultList();
			//end::query-native-mapping[]
			assertEquals( 3, books.size() );
			assertTrue( entityManager.contains( books.get( 0 ) ) );
		} );
	}

	@Entity(name = "GuideBook")
	@Table(name = "query_guide_book")
	public static class Book {
		@Id
		Long id;
		String title;
	}
}
