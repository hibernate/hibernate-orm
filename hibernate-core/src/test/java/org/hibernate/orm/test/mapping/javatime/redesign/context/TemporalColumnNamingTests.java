package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.naming.*;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Hand-checked names are compared across new, legacy-alias and legacy-handling
/// mappings. Quoted physical identifiers are also exercised through DDL and
/// JDBC.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialects( {
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class ),
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.PostgreSQLDialect.class ),
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.OracleDialect.class ) } )
class TemporalColumnNamingTests {
	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void retainedNamesAndQuotes( boolean metadata ) throws Exception {
		compare( metadata, AbsentNew.class, AbsentLegacy.class, List.of( "observed_time", "observed_time_tz" ), false );
		compare( metadata, EmptyNew.class, EmptyLegacy.class, List.of( "observed_time", "zoneOffset" ), false );
		compare( metadata, LiteralNew.class, LiteralLegacy.class, List.of( "observed_time", "literal_offset" ), false );
		compare( metadata, QuotedNew.class, QuotedLegacy.class, List.of( "Observed Time", "Observed Offset" ), true );
	}

	private void compare( boolean metadata, Class<?> modern, Class<?> legacy, List<String> expected, boolean quoted )
			throws Exception {
		for ( boolean physical : List.of( false, true ) ) {
			final Map<String, Object> settings = physical ? Map.of( "hibernate.physical_naming_strategy", Prefix.class )
					: Map.of();
			final var names = expected.stream().map( name -> physical ? "p_" + name : name ).toList();
			try ( var scope = ContextScope.boot( metadata, true, settings, modern ) ) {
				assertLayout( scope, modern, names, quoted );
			}
			try ( var scope = ContextScope.boot( metadata, true, settings, legacy ) ) {
				assertLayout( scope, legacy, names, quoted );
			}
			try ( var scope = ContextScope.boot( metadata, false, settings, legacy ) ) {
				assertLayout( scope, legacy, names, quoted );
			}
			if ( quoted )
				try ( var scope = ContextScope.open( metadata, settings, modern ) ) {
					scope.factory.getSchemaManager().validate();
					try {
						final var row = modern.getDeclaredConstructor().newInstance();
						modern.getDeclaredField( "id" ).setInt( row, 1 );
						modern.getDeclaredField( "stamp" ).set( row, OffsetDateTime.parse( "2026-07-01T12:00+02:00" ) );
						scope.tx( session -> session.persist( row ) );
						scope.tx( session -> {
							try {
								assertThat( modern.getDeclaredField( "stamp" ).get( session.find( modern, 1 ) ) )
										.isEqualTo( OffsetDateTime.parse( "2026-07-01T12:00+02:00" ) );
							} catch ( ReflectiveOperationException e ) {
								throw new AssertionError( e );
							}
						} );
					} catch ( ReflectiveOperationException e ) {
						throw new AssertionError( e );
					}
				}
		}
	}

	private void assertLayout( ContextScope scope, Class<?> type, List<String> names, boolean quoted ) {
		final var value = ( Component ) scope.metadata.getEntityBinding( type.getName() ).getProperty( "stamp" )
				.getValue();
		assertThat( value.getColumns().stream().map( org.hibernate.mapping.Column::getName ).toList() )
				.containsExactlyInAnyOrderElementsOf( names );
		assertThat( value.getColumns() ).allSatisfy( column -> {
			assertThat( column.isQuoted() ).isEqualTo( quoted );
			assertThat( column.getValue().getTable() ).isEqualTo( value.getTable() );
		} );
		assertThat( value.getColumnInsertability() ).containsOnly( true );
		assertThat( value.getColumnUpdateability() ).containsOnly( true );
	}

	public static class Prefix extends PhysicalNamingStrategyStandardImpl {
		@Override
		public Identifier toPhysicalColumnName( Identifier name, JdbcEnvironment environment ) {
			return name == null ? null : Identifier.toIdentifier( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity( name = "NamingAbsentNew" )
	static class AbsentNew {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingAbsentLegacy" )
	static class AbsentLegacy {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingEmptyNew" )
	static class EmptyNew {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneColumn( name = "" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingEmptyLegacy" )
	static class EmptyLegacy {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneColumn( name = "" )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingLiteralNew" )
	static class LiteralNew {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneColumn( name = "literal_offset" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingLiteralLegacy" )
	static class LiteralLegacy {
		@Id
		int id;
		@Column( name = "observed_time" )
		@TimeZoneColumn( name = "literal_offset" )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingQuotedNew" )
	static class QuotedNew {
		@Id
		int id;
		@Column( name = "`Observed Time`" )
		@TimeZoneColumn( name = "`Observed Offset`" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "NamingQuotedLegacy" )
	static class QuotedLegacy {
		@Id
		int id;
		@Column( name = "`Observed Time`" )
		@TimeZoneColumn( name = "`Observed Offset`" )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}
}
