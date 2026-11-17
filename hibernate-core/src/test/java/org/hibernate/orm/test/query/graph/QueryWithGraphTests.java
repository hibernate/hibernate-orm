package org.hibernate.orm.test.query.graph;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.query.resultmapping.dynamic.Book;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Book.class, QueryWithGraphTests.Publisher.class})
@SessionFactory
public class QueryWithGraphTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = new Book( 1, "The Two Towers", "987-654", LocalDate.now() );
			session.persist( book );

			var publisher = new Publisher( 1, "Me", List.of(book) );
			session.persist( publisher );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void simpleTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.createQuery( "from Publisher", Publisher.class ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isFalse();
		} );
		factoryScope.inTransaction( (session) -> {
			final RootGraphImplementor<Publisher> entityGraph = (RootGraphImplementor<Publisher>) session.getEntityGraph( "pub-with-books" );
			var results = session.createQuery( "from Publisher", entityGraph ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Test
	void namedQueryAppliesEntityGraph(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.createNamedQuery( "Publisher.findAllWithGraph", Publisher.class ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Test
	void getEntityGraphReturnsMutableCopy(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-base" );
			assertThat( entityGraph.getName() ).isEqualTo( "pub-base" );
			assertThat( entityGraph.hasAttributeNode( "books" ) ).isFalse();

			entityGraph.addAttributeNode( "books" );
			assertThat( entityGraph.hasAttributeNode( "books" ) ).isTrue();

			final EntityGraph<Publisher> entityGraphAgain = session.getEntityGraph( Publisher.class, "pub-base" );
			assertThat( entityGraphAgain.getName() ).isEqualTo( "pub-base" );
			assertThat( entityGraphAgain.hasAttributeNode( "books" ) ).isFalse();
		} );
	}

	@Test
	void withEntityGraphAppliesEntityGraph(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-with-books" );
			final var query = session.createQuery( "from Publisher" );
			final var graphedQuery = query.withEntityGraph( entityGraph );
			assertThat( graphedQuery ).isNotSameAs( query );

			final var plainResults = query.asSelectionQuery(Publisher.class).getResultList();
			assertThat( plainResults ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( plainResults.get( 0 ).books ) ).isFalse();
		} );
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-with-books" );
			var results = session.createQuery( "from Publisher" )
					.withEntityGraph( entityGraph )
					.getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Test
	void ofTypeReturnsCloneWithCopiedParameters(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var query = session.createQuery( "select p.name from Publisher p where p.id = :id" );
			query.setParameter( "id", 1 );

			final var typedQuery = query.ofType( String.class );
			assertThat( typedQuery ).isNotSameAs( query );
			assertThat( typedQuery.getSingleResult() ).isEqualTo( "Me" );
		} );
	}

	@Test
	void ofTypeRejectsIncompatibleHqlResultType(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> assertThatIllegalArgumentException()
				.isThrownBy( () -> session.createQuery( "select p.name from Publisher p" )
						.ofType( Publisher.class ) ) );
		factoryScope.inTransaction( (session) -> assertThatIllegalArgumentException()
				.isThrownBy( () -> session.createQuery("SELECT p FROM Publisher p")
						.ofType(String.class)) );
	}

	@Test
	void ofTypeRejectsIncompatibleNativeResultSetMapping(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> assertThatIllegalArgumentException()
				.isThrownBy( () -> session.createNamedQuery( "Publisher.nativeNames" )
						.ofType( Integer.class ) ) );
	}

	@Test
	void ofTypeRejectsUnmappedNativeEntityResult(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select id, name from Publisher" )
					.getSingleResult() ).isInstanceOf( Object[].class );
			assertThatIllegalArgumentException().isThrownBy( () -> session
					.createNativeQuery( "select id, name from Publisher" )
					.ofType( Publisher.class ) );
		} );
	}

	@Test
	void ofTypePreservesMappedNativeEntityAndParameters(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			final var query = session.createNativeQuery(
					"select id, name from Publisher where id = :id", Publisher.class );
			query.setParameter( "id", 1 );
			query.setMaxResults( 1 );
			final var typedQuery = query.ofType( Publisher.class );
			assertThat( typedQuery.getMaxResults() ).isEqualTo( 1 );
			assertThat( typedQuery.getSingleResult().name ).isEqualTo( "Me" );
			assertThat( query.ofType( Object.class ).getSingleResult() ).isInstanceOf( Publisher.class );
			assertThatIllegalArgumentException().isThrownBy( () -> query.ofType( String.class ) );
		} );
	}

	@Test
	void ofTypePreservesImplicitNativeResults(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select name from Publisher" )
					.ofType( String.class ).getSingleResult() ).isEqualTo( "Me" );
			assertThat( session.createNativeQuery( "select id, name from Publisher" )
					.ofType( Object[].class ).getSingleResult() ).containsExactly( 1, "Me" );
			assertThat( session.createNativeQuery( "select id, name from Publisher" )
					.ofType( Object.class ).getSingleResult() ).isInstanceOf( Object[].class );
		} );
	}

	@Test
	void ofTypePreservesTransformedNativeEntity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			final var publisher = session.find( Publisher.class, 1 );
			assertThat( session.createNativeQuery( "select id, name from Publisher" )
					.setTupleTransformer( (tuple, aliases) -> publisher )
					.ofType( Publisher.class ).getSingleResult() ).isSameAs( publisher );
		} );
	}

	@Test
	void ofTypePreservesListTransformedNativeEntity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			final var publisher = session.find( Publisher.class, 1 );
			assertThat( session.createNativeQuery( "select id, name from Publisher", Object.class )
					.setResultListTransformer( list -> List.of( publisher ) )
					.ofType( Publisher.class ).getSingleResult() ).isSameAs( publisher );
		} );
	}

	@Test
	void incompatibleGraphPreservesHibernateException(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Book> bookGraph = session.createEntityGraph( Book.class );

			assertThatThrownBy( () -> session.createQuery( "from Publisher", bookGraph ) )
					.isInstanceOf( QueryTypeMismatchException.class );
		} );
	}

	@Test
	void resultSetMappingIsRejectedForJpaQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> assertThatIllegalStateException()
				.isThrownBy( () -> session.createQuery( "from Publisher" )
						.withResultSetMapping( jakarta.persistence.sql.ResultSetMapping.entity( Publisher.class ) ) ) );
	}

	@Entity(name="Publisher")
	@Table(name="Publisher")
	@NamedQuery(
			name = "Publisher.findAllWithGraph",
			query = "from Publisher",
			resultClass = Publisher.class,
			entityGraph = "pub-with-books"
	)
	@NamedNativeQuery(
			name = "Publisher.nativeNames",
			query = "select name from Publisher",
			resultSetMapping = "Publisher.nameMapping"
	)
	@SqlResultSetMapping(
			name = "Publisher.nameMapping",
			columns = @ColumnResult(name = "name", type = String.class)
	)
	@NamedEntityGraph(
			name = "pub-with-books",
			graph = "id, name, books"
	)
	@NamedEntityGraph(
			name = "pub-base",
			graph = "id, name"
	)
	public static class Publisher {
		@Id
		private Integer id;
		private String name;
		@OneToMany
		@JoinColumn(name = "book_fk")
		private List<Book> books;

		public Publisher() {
		}

		public Publisher(Integer id, String name, List<Book> books) {
			this.id = id;
			this.name = name;
			this.books = books;
		}
	}
}
