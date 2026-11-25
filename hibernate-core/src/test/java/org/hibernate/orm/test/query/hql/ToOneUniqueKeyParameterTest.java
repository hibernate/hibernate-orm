package org.hibernate.orm.test.query.hql;

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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
				boxed.code = String.valueOf( 10 + id );
				session.persist( boxed );
				var primitive = new PrimitiveItem();
				primitive.id = id;
				primitive.code = boxed.code;
				session.persist( primitive );
				var note = new Note();
				note.id = id;
				note.label = "initial";
				note.boxed = boxed;
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

	@Test
	@RequiresDialect(H2Dialect.class)
	void foreignKeyIndex(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			final var item = session.find( PrimitiveItem.class, 1L );
			final var reference = session.getReference( PrimitiveItem.class, 2L );
			final var query = session.createQuery( "select n.id from Note n where n.primitive = :v", Long.class );
			for ( var argument : List.of( item, reference, item ) ) {
				observer.clear();
				assertThat( query.setParameter( "v", argument ).getResultList() )
						.containsExactly( argument == item ? 1L : 2L );
				assertThat( observer.getSqlQueries() ).hasSize( 1 );
				final String sql = observer.getSqlQueries().get( 0 );
				if ( argument == item ) {
					assertThat( sql ).containsPattern( "primitive_code\\s*=\\s*\\?" );
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
						.containsPattern( "IX_NOTE_PRIMITIVE:\\s+PRIMITIVE_CODE =" );
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
	}
}
