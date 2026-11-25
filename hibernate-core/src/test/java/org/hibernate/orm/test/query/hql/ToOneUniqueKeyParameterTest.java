package org.hibernate.orm.test.query.hql;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21257")
@DomainModel(annotatedClasses = { ToOneUniqueKeyParameterTest.Note.class,
		ToOneUniqueKeyParameterTest.BoxedItem.class, ToOneUniqueKeyParameterTest.PrimitiveItem.class })
@SessionFactory(useCollectingStatementObserver = true)
class ToOneUniqueKeyParameterTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				var boxed = new BoxedItem();
				boxed.id = id;
				// The unique keys and identifiers sort in opposite orders.
				boxed.code = id == 1 ? "21" : "12";
				session.persist( boxed );
				var primitive = new PrimitiveItem();
				primitive.id = id;
				primitive.code = boxed.code;
				session.persist( primitive );
				var note = new Note();
				note.id = id;
				note.label = "initial";
				note.boxed = boxed;
				note.boxedById = boxed;
				note.primitive = primitive;
				session.persist( note );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource({ "boxed,false", "boxed,true", "primitive,false", "primitive,true" })
	void transientArgument(String path, boolean criteria, SessionFactoryScope scope) {
		final Item argument = path.equals( "boxed" ) ? new BoxedItem() : new PrimitiveItem();
		argument.code = "12";
		scope.inTransaction( session -> {
			final List<Long> ids;
			if ( criteria ) {
				final var builder = session.getCriteriaBuilder();
				final var query = builder.createQuery( Long.class );
				final var note = query.from( Note.class );
				query.select( note.get( "id" ) ).where( builder.equal( note.get( path ), argument ) );
				ids = session.createQuery( query ).getResultList();
			}
			else {
				ids = session.createQuery( "select n.id from Note n where n." + path + " = :v", Long.class )
						.setParameter( "v", argument ).getResultList();
			}
			assertThat( ids ).containsExactly( 2L );
		} );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"n.boxed in (:v) | 2", "n.boxed not in (:v) | 1", ":v in (n.boxed) | 2", ":v not in (n.boxed) | 1",
			"n.boxed in (:v, :v) | 2", "n.boxed between :v and :v | 2", "n.boxed not between :v and :v | 1",
			":v between n.boxed and n.boxed | 2", "n.boxed between n.boxed and :v | 2",
			"n.boxed not between n.boxed and :v | 1"
	})
	void transientParameterInPredicate(String predicate, long expected, SessionFactoryScope scope) {
		final var item = new BoxedItem();
		item.code = "12";
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			assertThat( session.createQuery( "select n.id from Note n where " + predicate, Long.class )
					.setParameter( "v", item ).getResultList() ).containsExactly( expected );
			assertThat( observer.getSqlQueries() ).singleElement().asString().doesNotContainIgnoringCase( "BoxedItem" );
		} );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"in (:v, (select i from BoxedItem i where i.id = 1)) | 1,2",
			"in ((select i from BoxedItem i where i.id = 1), :v) | 1,2",
			"not in (:v, (select i from BoxedItem i where i.id = 2)) | 1",
			"in (:v, (select i from BoxedItem i where i.id = 3)) | 2",
			"not in ((select i from BoxedItem i where i.id = 3), :v) | ''",
			"in (:v, (select i from BoxedItem i where i.id = n.id)) | 1,2",
			"in (:v, (select other.boxed from Note other where other.id = 1)) | 1,2",
			"in (:v, (select other.boxedById from Note other where other.id = 1)) | 1,2"
	})
	void parameterAndScalarSubquery(String predicate, String expected, SessionFactoryScope scope) {
		final var transientItem = new BoxedItem();
		transientItem.code = "12";
		final var reference = scope.fromTransaction( session -> session.getReference( BoxedItem.class, 2L ) );
		final var expectedIds = expected.isEmpty() ? List.<Long>of()
				: Arrays.stream( expected.split( "," ) ).map( Long::valueOf ).toList();
		scope.inTransaction( session -> {
			final var query = session.createSelectionQuery(
					"select n.id from Note n where n.boxed " + predicate + " order by n.id", Long.class );
			final var observer = scope.getCollectingStatementObserver();
			for ( var argument : List.of( transientItem, reference, transientItem ) ) {
				observer.clear();
				assertThat( query.setParameter( "v", argument ).getResultList() ).isEqualTo( expectedIds );
				assertThat( observer.getSqlQueries() ).hasSize( 1 );
			}
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnionInSubquery.class)
	void subqueryWithDifferentKeysInUnion(SessionFactoryScope scope) {
		final var item = new BoxedItem();
		item.code = "12";
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select n.id from Note n where n.boxed in (:v, "
						+ "(select i from BoxedItem i where i.id = 3 union all "
						+ "select m.boxedById from Note m where m.id = 1)) order by n.id", Long.class )
				.setParameter( "v", item ).getResultList() ).containsExactly( 1L, 2L ) );
	}

	@Test
	void transientTestExpressionWithScalarSubquery(SessionFactoryScope scope) {
		final var item = new BoxedItem();
		item.code = "12";
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select n.id from Note n where :v in (n.boxed, (select i from BoxedItem i where i.id = 1))",
				Long.class ).setParameter( "v", item ).getResultList() ).containsExactly( 2L ) );
	}

	@Test
	void mixedInListPreservesNullableAssociation(SessionFactoryScope scope) {
		final var item = new BoxedItem();
		item.code = "12";
		scope.inTransaction( session -> {
			final var note = new Note();
			note.id = 3;
			session.persist( note );
		} );
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select n.id from Note n where n.id = 3 or n.boxed in "
						+ "(:v, (select m.boxedById from Note m where m.id = 1)) order by n.id", Long.class )
				.setParameter( "v", item ).getResultList() ).containsExactly( 1L, 2L, 3L ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void listOfEntities(boolean negated, SessionFactoryScope scope) {
		final var item = new BoxedItem();
		item.code = "12";
		final var reference = scope.fromTransaction( session -> session.getReference( BoxedItem.class, 1L ) );
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			final var query = session.createQuery(
					"select n.id from Note n where n.boxed " + ( negated ? "not in" : "in" ) + " :items order by n.id",
					Long.class );
			assertListResult( observer, query, List.of(), negated ? List.of( 1L, 2L ) : List.of() );
			assertListResult( observer, query, List.of( item ), negated ? List.of( 1L ) : List.of( 2L ) );
			assertListResult( observer, query, List.of( reference, item ), negated ? List.of() : List.of( 1L, 2L ) );
			assertListResult( observer, query, List.of( item, reference ), negated ? List.of() : List.of( 1L, 2L ) );
			assertListResult( observer, query, Arrays.asList( item, null ), negated ? List.of() : List.of( 2L ) );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	private static void assertListResult(CollectingStatementObserver observer, SelectionQuery<Long> query,
			List<BoxedItem> values, List<Long> expected) {
		observer.clear();
		assertThat( query.setParameterList( "items", values ).getResultList() ).isEqualTo( expected );
		assertThat( observer.getSqlQueries() ).hasSize( 1 );
	}

	@Test
	void criteriaListParameter(SessionFactoryScope scope) {
		final var item = new PrimitiveItem();
		item.code = "12";
		final var reference = scope.fromTransaction( session -> session.getReference( PrimitiveItem.class, 1L ) );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var note = criteria.from( Note.class );
			final var items = builder.listParameter( PrimitiveItem.class );
			criteria.select( note.get( "id" ) ).where( note.get( "primitive" ).in( items ) )
					.orderBy( builder.asc( note.get( "id" ) ) );
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			assertThat( session.createQuery( criteria ).setParameter( items, List.of( reference, item ) ).getResultList() )
					.containsExactly( 1L, 2L );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "n.boxed between :lo and :hi", "n.boxed >= :lo and n.boxed <= :hi",
			":lo <= n.boxed and :hi >= n.boxed" })
	void rangeUsesUniqueKeyOrder(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var low = session.find( BoxedItem.class, 2L );
			final var high = session.find( BoxedItem.class, 1L );
			final var query = session.createQuery( "select n.id from Note n where " + predicate + " order by n.id", Long.class );
			assertThat( query.setParameter( "lo", low ).setParameter( "hi", high ).getResultList() ).containsExactly( 1L, 2L );
			assertThat( query.setParameter( "lo", high ).setParameter( "hi", low ).getResultList() ).isEmpty();
		} );
	}

	@Test
	void cachedCriteriaWithDetachedReference(SessionFactoryScope scope) {
		final var reference = scope.fromTransaction( session -> session.getReference( BoxedItem.class, 1L ) );
		final var transientItem = new BoxedItem();
		transientItem.code = "12";
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			for ( var argument : List.of( reference, transientItem, reference ) ) {
				final var builder = session.getCriteriaBuilder();
				final var criteria = builder.createQuery( Long.class );
				final var note = criteria.from( Note.class );
				criteria.select( note.get( "id" ) ).where( builder.equal( note.get( "boxed" ), argument ) );
				observer.clear();
				assertThat( session.createQuery( criteria ).setQueryPlanCacheable( true ).getResultList() )
						.containsExactly( argument == reference ? 1L : 2L );
				assertThat( observer.getSqlQueries() ).hasSize( 1 );
			}
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@Test
	void comparisonWithListParameter(SessionFactoryScope scope) {
		final var reference = scope.fromTransaction( session -> session.getReference( PrimitiveItem.class, 1L ) );
		final var transientItem = new PrimitiveItem();
		transientItem.code = "12";
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			final var query = session.createQuery(
					"select n.id from Note n where n.primitive = :v and n.id in :ids", Long.class );
			for ( var argument : List.of( transientItem, reference ) ) {
				final long expected = argument == reference ? 1L : 2L;
				for ( var ids : List.of( List.of( expected ), List.of( 1L, 2L ) ) ) {
					observer.clear();
					assertThat( query.setParameter( "v", argument ).setParameterList( "ids", ids ).getResultList() )
							.containsExactly( expected );
					assertThat( observer.getSqlQueries() ).hasSize( 1 );
				}
			}
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@Test
	void updateSecondaryTable(SessionFactoryScope scope) {
		final var reference = scope.fromTransaction( session -> session.getReference( PrimitiveItem.class, 1L ) );
		final var transientItem = new PrimitiveItem();
		transientItem.code = "12";
		scope.inTransaction( session -> {
			final var update = session.createMutationQuery( "update Note n set n.label = :label where n.primitive = :v" );
			assertThat( update.setParameter( "v", transientItem ).setParameter( "label", "second" )
					.executeUpdate() ).isEqualTo( 1 );
			assertThat( update.setParameter( "v", reference ).setParameter( "label", "first" )
					.executeUpdate() ).isEqualTo( 1 );
			assertThat( session.createQuery( "select n.label from Note n order by n.id", String.class ).getResultList() )
					.containsExactly( "first", "second" );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "n.primitive = :v", "n.primitive in (:v, :v)", "n.primitive between :v and :v",
			"n.primitive in (:v, (select i from PrimitiveItem i where i.id = 3))" })
	@RequiresDialect(H2Dialect.class)
	void foreignKeyIndex(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			final var item = session.find( PrimitiveItem.class, 1L );
			final var reference = session.getReference( PrimitiveItem.class, 2L );
			final var query = session.createQuery( "select n.id from Note n where " + predicate, Long.class );
			for ( var argument : List.of( item, reference, item ) ) {
				observer.clear();
				assertThat( query.setParameter( "v", argument ).getResultList() )
						.containsExactly( argument == item ? 1L : 2L );
				assertThat( observer.getSqlQueries() ).hasSize( 1 );
				final String sql = observer.getSqlQueries().get( 0 );
				if ( argument == item && !predicate.contains( "select" ) ) {
					assertThat( sql ).doesNotContainIgnoringCase( "PrimitiveItem" );
				}
				final String plan = session.doReturningWork( connection -> {
					try ( var statement = connection.prepareStatement( "explain " + sql ) ) {
						try ( var rows = statement.executeQuery() ) {
							rows.next();
							return rows.getString( 1 );
						}
					}
				} );
				// A scan of the FK index also mentions its name. Check the actual index search condition.
				assertThat( plan.toUpperCase( Locale.ROOT ) )
						.containsPattern( "IX_NOTE_PRIMITIVE:\\s+PRIMITIVE_CODE (?:=|IN|>=)" );
			}
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );
	}

	@MappedSuperclass
	static class Item {
		@Column(unique = true)
		String code;
	}

	@Entity(name = "BoxedItem")
	static class BoxedItem extends Item {
		@Id Long id;
	}

	@Entity(name = "PrimitiveItem")
	static class PrimitiveItem extends Item {
		@Id long id;
	}

	@Entity(name = "Note")
	@Table(name = "q_note", indexes = @Index(name = "ix_note_primitive", columnList = "primitive_code"))
	@SecondaryTable(name = "note_details")
	static class Note {
		@Id long id;
		@Column(table = "note_details")
		String label;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "primitive_code", referencedColumnName = "code")
		PrimitiveItem primitive;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "boxed_code", referencedColumnName = "code")
		BoxedItem boxed;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "boxed_id")
		BoxedItem boxedById;
	}
}
