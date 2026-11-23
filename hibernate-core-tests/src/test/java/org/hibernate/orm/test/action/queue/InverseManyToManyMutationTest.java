package org.hibernate.orm.test.action.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Bag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that inverse many-to-many changes never mutate association rows,
/// including queued operations, collection replacement, and detached merge.
/// Cascades and changes to entity properties are deliberately excluded.
///
/// @author Steve Ebersole
@JiraKey("HHH-20996")
@DomainModel(annotatedClasses = {InverseManyToManyMutationTest.Book.class, InverseManyToManyMutationTest.Author.class})
@SessionFactory(useCollectingStatementObserver = true)
public class InverseManyToManyMutationTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var author = new Author();
			author.id = 1;
			session.persist( author );
			for ( int id = 1; id <= 4; id++ ) {
				final var book = new Book();
				book.id = id;
				if ( id <= 2 ) {
					for ( var kind : Kind.values() ) {
						kind.owners( book ).add( author );
						kind.add( author, book );
					}
				}
				session.persist( book );
			}
		} );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> mutations() {
		return Stream.of( Kind.values() ).flatMap( kind ->
				Stream.of( Mutation.values() ).filter( mutation -> mutation.appliesTo( kind ) )
						.flatMap( mutation -> Stream.of( Path.values() )
								.map( path -> Arguments.of( kind, mutation, path ) ) ) );
	}

	@ParameterizedTest(name = "{0}: {1}, {2}")
	@MethodSource("mutations")
	void inverseMutationDoesNotWrite(Kind kind, Mutation mutation, Path path, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		if ( path == Path.DETACHED_MERGE ) {
			final var state = scope.fromTransaction( session -> {
				final var author = session.find( Author.class, 1 );
				Hibernate.initialize( kind.raw( author ) );
				return new State( author, books( session ) );
			} );
			mutation.apply( kind, state.author(), state.books() );
			observer.clear();
			scope.inTransaction( session -> {
				session.merge( state.author() );
				session.flush();
				session.flush();
			} );
		}
		else {
			scope.inTransaction( session -> {
				final var author = session.find( Author.class, 1 );
				final var original = (PersistentCollection<?>) kind.raw( author );
				assertThat( Hibernate.isInitialized( original ) ).isFalse();
				if ( path != Path.LAZY ) {
					Hibernate.initialize( original );
				}
				final var books = books( session );
				observer.clear();
				mutation.apply( kind, author, books );
				if ( path == Path.LAZY && ( mutation == Mutation.CLEAR
						|| kind == Kind.BAG && mutation == Mutation.ADD ) ) {
					assertThat( Hibernate.isInitialized( original ) ).isFalse();
					assertThat( original.hasQueuedOperations() ).isTrue();
				}
				if ( path == Path.AUTO_FLUSH ) {
					for ( int i = 0; i < 2; i++ ) {
						assertThat( session.createQuery(
								"select b.id from InverseMutationBook b join b."
										+ kind.name().toLowerCase( Locale.ROOT ) + "Authors a", Integer.class )
								.getResultList() ).containsExactlyInAnyOrder( 1, 2 );
					}
				}
				else {
					session.flush();
					session.flush();
				}
			} );
		}
		assertThat( observer.getSqlQueries() ).filteredOn( InverseManyToManyMutationTest::isDml ).isEmpty();
		scope.inTransaction( session -> {
			for ( var checkedKind : Kind.values() ) {
				assertLinks( session, checkedKind, 1, 1, 2 );
			}
			assertThat( kind.values( session.find( Author.class, 1 ) ) )
					.extracting( book -> book.id ).containsExactlyInAnyOrder( 1, 2 );
		} );
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	void inverseCreateDoesNotWriteAssociationRows(Kind kind, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var author = new Author();
			author.id = 2;
			kind.add( author, session.find( Book.class, 1 ) );
			kind.add( author, session.find( Book.class, 2 ) );
			observer.clear();
			session.persist( author );
			session.flush();
			session.flush();
		} );
		assertThat( observer.getSqlQueries() ).filteredOn( sql -> isDml( sql )
				&& sql.toLowerCase( Locale.ROOT ).contains( "invm_" ) ).isEmpty();
		scope.inTransaction( session -> {
			for ( var checkedKind : Kind.values() ) {
				assertLinks( session, checkedKind, 1, 1, 2 );
				assertLinks( session, checkedKind, 2 );
			}
			assertThat( kind.values( session.find( Author.class, 2 ) ) ).isEmpty();
		} );
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	void owningMutationsStillWrite(Kind kind, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var author = session.find( Author.class, 1 );
			final var first = kind.owners( session.find( Book.class, 1 ) );
			final var third = kind.owners( session.find( Book.class, 3 ) );
			Hibernate.initialize( first );
			Hibernate.initialize( third );
			observer.clear();
			first.remove( author );
			third.add( author );
			session.flush();
			session.flush();
		} );
		assertThat( observer.getSqlQueries() ).filteredOn( InverseManyToManyMutationTest::isDml ).hasSize( 2 );
		scope.inTransaction( session -> {
			for ( var checkedKind : Kind.values() ) {
				if ( checkedKind == kind ) {
					assertLinks( session, checkedKind, 1, 2, 3 );
				}
				else {
					assertLinks( session, checkedKind, 1, 1, 2 );
				}
			}
			assertThat( kind.values( session.find( Author.class, 1 ) ) )
					.extracting( book -> book.id ).containsExactlyInAnyOrder( 2, 3 );
		} );
	}

	private static List<Book> books(SessionImplementor session) {
		return List.of( session.find( Book.class, 1 ), session.find( Book.class, 2 ),
				session.find( Book.class, 3 ), session.find( Book.class, 4 ) );
	}

	private static void assertLinks(SessionImplementor session, Kind kind, int authorId, Integer... ids) {
		assertThat( session.createNativeQuery(
				"select book_id from " + kind.table + " where author_id = :id", Integer.class )
				.setParameter( "id", authorId ).getResultList() ).containsExactlyInAnyOrder( ids );
	}

	private static boolean isDml(String sql) {
		return sql.toLowerCase( Locale.ROOT ).matches( "(?s).*\\b(insert\\s+into|update|delete\\s+from|merge\\s+into)\\b.*" );
	}

	private record State(Author author, List<Book> books) {}

	enum Path { INITIALIZED, LAZY, AUTO_FLUSH, DETACHED_MERGE }

	enum Kind {
		SET("InvM_Set"), BAG("InvM_Bag"), MAP("InvM_Map");

		final String table;

		Kind(String table) {
			this.table = table;
		}

		Object raw(Author author) {
			return switch ( this ) {
				case SET -> author.setBooks;
				case BAG -> author.bagBooks;
				case MAP -> author.mapBooks;
			};
		}

		Collection<Book> values(Author author) {
			return switch ( this ) {
				case SET -> author.setBooks;
				case BAG -> author.bagBooks;
				case MAP -> author.mapBooks.values();
			};
		}

		Set<Author> owners(Book book) {
			return switch ( this ) {
				case SET -> book.setAuthors;
				case BAG -> book.bagAuthors;
				case MAP -> book.mapAuthors;
			};
		}

		void add(Author author, Book book) {
			if ( this == MAP ) {
				author.mapBooks.put( book.id, book );
			}
			else {
				values( author ).add( book );
			}
		}

		void clear(Author author) {
			if ( this == MAP ) {
				author.mapBooks.clear();
			}
			else {
				values( author ).clear();
			}
		}

		void replace(Author author, List<Book> books) {
			switch ( this ) {
				case SET -> author.setBooks = books == null ? null : new HashSet<>( books );
				case BAG -> author.bagBooks = books == null ? null : new ArrayList<>( books );
				case MAP -> {
					author.mapBooks = books == null ? null : new HashMap<>();
					if ( books != null ) {
						books.forEach( book -> add( author, book ) );
					}
				}
			}
		}
	}

	enum Mutation {
		ADD, ADD_ALL, REMOVE, REMOVE_ALL, RETAIN_ALL, REMOVE_IF, ITERATOR_REMOVE,
		CLEAR, CLEAR_AND_ADD, REMOVE_AND_ADD, REPLACE_COLLECTION, REPLACE_EMPTY, REPLACE_NULL,
		BAG_DUPLICATE_ADD, BAG_INDEXED_ADD, BAG_SET, BAG_INDEX_REMOVE, BAG_ITERATOR_REMOVE,
		BAG_ITERATOR_SET, BAG_ITERATOR_ADD, BAG_SUBLIST_CLEAR, BAG_REVERSE, BAG_REPLACE_ALL,
		MAP_PUT_REPLACE, MAP_PUT_ALL, MAP_REMOVE_KEY, MAP_REMOVE_KEY_VALUE, MAP_KEYSET_REMOVE,
		MAP_ENTRYSET_REMOVE, MAP_ENTRY_SET_VALUE, MAP_REPLACE, MAP_REPLACE_ALL, MAP_COMPUTE,
		MAP_COMPUTE_ABSENT, MAP_COMPUTE_PRESENT, MAP_MERGE;

		boolean appliesTo(Kind kind) {
			return name().startsWith( "BAG_" ) ? kind == Kind.BAG
					: !name().startsWith( "MAP_" ) || kind == Kind.MAP;
		}

		void apply(Kind kind, Author author, List<Book> books) {
			final var first = books.get( 0 );
			final var second = books.get( 1 );
			final var third = books.get( 2 );
			final var fourth = books.get( 3 );
			switch ( this ) {
				case ADD -> kind.add( author, third );
				case ADD_ALL -> {
					if ( kind == Kind.MAP ) {
						author.mapBooks.putAll( Map.of( 3, third, 4, fourth ) );
					}
					else {
						kind.values( author ).addAll( List.of( third, fourth ) );
					}
				}
				case REMOVE -> kind.values( author ).remove( first );
				case REMOVE_ALL -> kind.values( author ).removeAll( List.of( first ) );
				case RETAIN_ALL -> kind.values( author ).retainAll( List.of( second ) );
				case REMOVE_IF -> kind.values( author ).removeIf( book -> book.id == 1 );
				case ITERATOR_REMOVE -> {
					final var iterator = kind.values( author ).iterator();
					iterator.next();
					iterator.remove();
				}
				case CLEAR -> kind.clear( author );
				case CLEAR_AND_ADD -> {
					kind.clear( author );
					kind.add( author, third );
				}
				case REMOVE_AND_ADD -> {
					kind.values( author ).remove( first );
					kind.add( author, third );
				}
				case REPLACE_COLLECTION -> kind.replace( author, List.of( third ) );
				case REPLACE_EMPTY -> kind.replace( author, List.of() );
				case REPLACE_NULL -> kind.replace( author, null );
				case BAG_DUPLICATE_ADD -> author.bagBooks.add( first );
				case BAG_INDEXED_ADD -> author.bagBooks.add( 0, third );
				case BAG_SET -> author.bagBooks.set( 0, third );
				case BAG_INDEX_REMOVE -> author.bagBooks.remove( 0 );
				case BAG_ITERATOR_REMOVE, BAG_ITERATOR_SET, BAG_ITERATOR_ADD -> {
					final var iterator = author.bagBooks.listIterator();
					iterator.next();
					if ( this == BAG_ITERATOR_REMOVE ) {
						iterator.remove();
					}
					else if ( this == BAG_ITERATOR_SET ) {
						iterator.set( third );
					}
					else {
						iterator.add( third );
					}
				}
				case BAG_SUBLIST_CLEAR -> author.bagBooks.subList( 0, 1 ).clear();
				case BAG_REVERSE -> Collections.reverse( author.bagBooks );
				case BAG_REPLACE_ALL -> author.bagBooks.replaceAll( book -> third );
				case MAP_PUT_REPLACE -> author.mapBooks.put( 1, third );
				case MAP_PUT_ALL -> author.mapBooks.putAll( Map.of( 3, third, 4, fourth ) );
				case MAP_REMOVE_KEY -> author.mapBooks.remove( 1 );
				case MAP_REMOVE_KEY_VALUE -> author.mapBooks.remove( 1, first );
				case MAP_KEYSET_REMOVE -> author.mapBooks.keySet().remove( 1 );
				case MAP_ENTRYSET_REMOVE -> author.mapBooks.entrySet().removeIf( entry -> entry.getKey() == 1 );
				case MAP_ENTRY_SET_VALUE -> author.mapBooks.entrySet().iterator().next().setValue( third );
				case MAP_REPLACE -> author.mapBooks.replace( 1, third );
				case MAP_REPLACE_ALL -> author.mapBooks.replaceAll( (key, value) -> third );
				case MAP_COMPUTE -> author.mapBooks.compute( 1, (key, value) -> third );
				case MAP_COMPUTE_ABSENT -> author.mapBooks.computeIfAbsent( 3, key -> third );
				case MAP_COMPUTE_PRESENT -> author.mapBooks.computeIfPresent( 1, (key, value) -> third );
				case MAP_MERGE -> author.mapBooks.merge( 1, third, (oldValue, newValue) -> newValue );
			}
		}
	}

	@Entity(name = "InverseMutationBook")
	@Table(name = "InverseMutationBook")
	public static class Book {
		@Id
		int id;

		@ManyToMany
		@JoinTable(name = "InvM_Set", joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "author_id"))
		Set<Author> setAuthors = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "InvM_Bag", joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "author_id"))
		Set<Author> bagAuthors = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "InvM_Map", joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "author_id"))
		Set<Author> mapAuthors = new HashSet<>();
	}

	@Entity(name = "InverseMutationAuthor")
	@Table(name = "InverseMutationAuthor")
	public static class Author {
		@Id
		int id;

		@ManyToMany(mappedBy = "setAuthors")
		Set<Book> setBooks = new HashSet<>();

		@Bag
		@ManyToMany(mappedBy = "bagAuthors")
		List<Book> bagBooks = new ArrayList<>();

		@MapKey(name = "id")
		@ManyToMany(mappedBy = "mapAuthors")
		Map<Integer, Book> mapBooks = new HashMap<>();
	}
}
