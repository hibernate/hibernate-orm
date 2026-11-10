package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// COLUMN operation flags apply to the complete tuple. Independent JDBC seeds
/// and fresh sessions prevent read-only values from appearing preserved by
/// entity state.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalColumnLayoutTests {
	private static final OffsetDateTime ORIGINAL = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
	private static final OffsetDateTime CHANGED = ORIGINAL.plusMinutes( 7 )
			.withOffsetSameInstant( ZoneOffset.ofHours( -3 ) );

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void matchingFlagsControlBothColumns( boolean metadata ) throws Exception {
		verify( metadata, Both.class, true, true );
		verify( metadata, InsertOnly.class, true, false );
		verify( metadata, UpdateOnly.class, false, true );
		verify( metadata, ReadOnly.class, false, false );
		verify( metadata, InheritedFlags.class, false, false );
		verify( metadata, ImplicitFlags.class, false, false );
	}

	private void verify( boolean metadata, Class<? extends Row> type, boolean insert, boolean update )
			throws Exception {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.cache.use_second_level_cache", false ),
				type ) ) {
			final String primary = type == ImplicitFlags.class ? "stamp" : "stamp_time";
			final String auxiliary = type == ImplicitFlags.class ? "stamp_tz"
					: type == InheritedFlags.class ? "stamp_time_tz" : "stamp_offset";
			final String table = scope.metadata.getEntityBinding( type.getName() ).getTable().getName();
			final Row row = type.getDeclaredConstructor().newInstance();
			row.id = 1;
			row.stamp( ORIGINAL );
			scope.measure();
			scope.tx( session -> session.persist( row ) );
			final var insertSql = scope.sql.stream().filter( sql -> sql.startsWith( "insert " ) ).toList();
			assertThat( insertSql ).hasSize( 1 );
			assertThat( insertSql.get( 0 ).contains( primary ) ).isEqualTo( insert );
			assertThat( insertSql.get( 0 ).contains( auxiliary ) ).isEqualTo( insert );
			if ( !insert )
				scope.tx( session -> session.doWork( connection -> {
					try ( var statement = connection.prepareStatement(
							"update " + table + " set " + primary + "=?," + auxiliary + "=? where id=1" ) ) {
						statement.setObject( 1, ORIGINAL.withOffsetSameInstant( ZoneOffset.UTC ).toLocalDateTime() );
						statement.setInt( 2, ORIGINAL.getOffset().getTotalSeconds() );
						assertThat( statement.executeUpdate() ).isEqualTo( 1 );
					}
				} ) );
			scope.measure();
			scope.tx( session -> {
				final Row found = session.find( type, 1 );
				assertThat( found.stamp() ).isEqualTo( ORIGINAL );
				found.stamp( CHANGED );
			} );
			final var updates = scope.sql.stream().filter( sql -> sql.startsWith( "update " ) ).toList();
			if ( update ) {
				assertThat( updates ).hasSize( 1 );
				assertThat( updates.get( 0 ) ).contains( primary, auxiliary );
			} else
				assertThat( updates ).isEmpty();
			scope.tx(
					session -> assertThat( session.find( type, 1 ).stamp() ).isEqualTo( update ? CHANGED : ORIGINAL ) );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void mismatchedFlagsRejectAtBoot( boolean metadata ) {
		for ( Class<?> type : List.of( InsertPrimary.class, InsertAux.class, UpdatePrimary.class, UpdateAux.class,
				OverrideInsert.class, OverrideUpdate.class ) )
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, type ) ) {
				}
			} ).as( type.getSimpleName() ).isInstanceOf( MappingException.class )
					.hasMessageContaining( "matching insertability" );
	}

	@MappedSuperclass
	static abstract class Row {
		@Id
		int id;

		abstract OffsetDateTime stamp();

		abstract void stamp( OffsetDateTime value );
	}

	@Entity( name = "LayoutBoth" )
	static class Both extends Row {
		@Column( name = "stamp_time", insertable = true, updatable = true )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( name = "stamp_offset", insertable = true, updatable = true )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutInsertOnly" )
	static class InsertOnly extends Row {
		@Column( name = "stamp_time", insertable = true, updatable = false )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( name = "stamp_offset", insertable = true, updatable = false )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutUpdateOnly" )
	static class UpdateOnly extends Row {
		@Column( name = "stamp_time", insertable = false, updatable = true )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( name = "stamp_offset", insertable = false, updatable = true )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutReadOnly" )
	static class ReadOnly extends Row {
		@Column( name = "stamp_time", insertable = false, updatable = false )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( name = "stamp_offset", insertable = false, updatable = false )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutInheritedFlags" )
	static class InheritedFlags extends Row {
		@Column( name = "stamp_time", insertable = false, updatable = false )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutImplicitFlags" )
	static class ImplicitFlags extends Row {
		@Column( insertable = false, updatable = false )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;

		OffsetDateTime stamp() {
			return stamp;
		}

		void stamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@Entity( name = "LayoutInsertPrimary" )
	static class InsertPrimary {
		@Id
		int id;
		@Column( insertable = true, updatable = true )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( insertable = false, updatable = true )
		OffsetDateTime stamp;
	}

	@Entity( name = "LayoutInsertAux" )
	static class InsertAux {
		@Id
		int id;
		@Column( insertable = false, updatable = true )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( insertable = true, updatable = true )
		OffsetDateTime stamp;
	}

	@Entity( name = "LayoutUpdatePrimary" )
	static class UpdatePrimary {
		@Id
		int id;
		@Column( insertable = true, updatable = true )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( insertable = true, updatable = false )
		OffsetDateTime stamp;
	}

	@Entity( name = "LayoutUpdateAux" )
	static class UpdateAux {
		@Id
		int id;
		@Column( insertable = true, updatable = false )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneColumn( insertable = true, updatable = true )
		OffsetDateTime stamp;
	}

	@Entity( name = "LayoutOverrideInsert" )
	static class OverrideInsert {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@AttributeOverride( name = "instant", column = @Column( name = "overridden", insertable = false ) )
		OffsetDateTime stamp;
	}

	@Entity( name = "LayoutOverrideUpdate" )
	static class OverrideUpdate {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@AttributeOverride( name = "zoneOffset", column = @Column( name = "overridden", updatable = false ) )
		OffsetDateTime stamp;
	}
}
