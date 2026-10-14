package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		UnidirectionalOrphanRemovalMergeTest.Parent.class,
		UnidirectionalOrphanRemovalMergeTest.Child.class,
		ManagedOrphanRemovalMergeTest.Author.class,
		ManagedOrphanRemovalMergeTest.Book.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20973")
class ManagedOrphanRemovalMergeTest {
	static Stream<Arguments> removals() {
		return Stream.of( false, true ).flatMap( flushFirst ->
				Stream.of( false, true ).flatMap( keepSecond ->
						Stream.of( false, true ).map( addNew -> Arguments.of( flushFirst, keepSecond, addNew ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("removals")
	void deletesRemovedRowsAndRetainsSurvivors(
			boolean flushFirst, boolean keepSecond, boolean addNew, SessionFactoryScope scope) {
		Long[] ids = scope.fromTransaction( session -> {
			var parent = new UnidirectionalOrphanRemovalMergeTest.Parent();
			var first = new UnidirectionalOrphanRemovalMergeTest.Child();
			var second = new UnidirectionalOrphanRemovalMergeTest.Child();
			parent.children.add( first );
			parent.children.add( second );
			session.persist( parent );
			if ( flushFirst ) {
				session.flush();
			}
			parent.children.remove( first );
			if ( !keepSecond ) {
				parent.children.remove( second );
			}
			var added = new UnidirectionalOrphanRemovalMergeTest.Child();
			if ( addNew ) {
				parent.children.add( added );
			}
			assertThat( session.contains( parent ) ).isTrue();
			assertThat( session.merge( parent ) ).isSameAs( parent );
			Long addedId = null;
			if ( addNew ) {
				var mergedChild = parent.children.get( parent.children.size() - 1 );
				assertThat( mergedChild ).isNotSameAs( added );
				assertThat( session.contains( mergedChild ) ).isTrue();
				addedId = mergedChild.id;
			}
			session.flush();
			session.flush();
			return new Long[] { parent.id, first.id, second.id, addedId };
		} );
		scope.inTransaction( session -> {
			var parent = session.find( UnidirectionalOrphanRemovalMergeTest.Parent.class, ids[0] );
			assertThat( session.find( UnidirectionalOrphanRemovalMergeTest.Child.class, ids[1] ) ).isNull();
			var second = session.find( UnidirectionalOrphanRemovalMergeTest.Child.class, ids[2] );
			if ( keepSecond ) {
				assertThat( second ).isNotNull();
				assertThat( parent.children ).contains( second );
			}
			else {
				assertThat( second ).isNull();
			}
			if ( addNew ) {
				var added = session.find( UnidirectionalOrphanRemovalMergeTest.Child.class, ids[3] );
				assertThat( added ).isNotNull();
				assertThat( parent.children ).contains( added );
			}
			assertThat( parent.children ).hasSize( (keepSecond ? 1 : 0) + (addNew ? 1 : 0) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19419")
	void removesOrphansWithoutCascadeMerge(SessionFactoryScope scope) {
		Long[] ids = scope.fromTransaction( session -> {
			var author = new Author();
			var book = new Book();
			book.author = author;
			author.books.add( book );
			session.persist( author );
			author.books.clear();
			assertThat( session.merge( author ) ).isSameAs( author );
			session.flush();
			return new Long[] { author.id, book.id };
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Author.class, ids[0] ).books ).isEmpty();
			assertThat( session.find( Book.class, ids[1] ) ).isNull();
		} );
	}

	@Entity(name = "MergeAuthor")
	static class Author {
		@Id
		@GeneratedValue
		Long id;
		@OneToMany(mappedBy = "author", cascade = { PERSIST, REMOVE }, orphanRemoval = true)
		List<Book> books = new ArrayList<>();
	}

	@Entity(name = "MergeBook")
	static class Book {
		@Id
		@GeneratedValue
		Long id;
		@ManyToOne
		Author author;
	}
}
