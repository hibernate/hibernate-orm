package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.mapping.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Dynamic XML component identifiers remain dynamic beside real Java temporal
/// declarations, through guarded bootstrap and fresh JDBC-backed map reads.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalDynamicModelTests {
	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void mixedDynamicAndJavaModels( boolean metadata ) {
		for ( boolean direct : List.of( true, false ) ) {
			final Class<?> javaType = direct ? Adjusted.class : Legacy.class;
			try ( var scope = ContextScope.open( metadata, direct,
					Map.of( "hibernate.cache.use_second_level_cache", false ),
					List.of( "org/hibernate/jpa/test/metamodel/CompositeIdEntity.orm.xml" ), javaType ) ) {
				final var dynamic = scope.metadata.getEntityBinding( "CompositeIdEntity" );
				assertThat( dynamic.getClassName() ).isNull();
				final var id = ( Component ) dynamic.getIdentifier();
				assertThat( id.getContextualCompositeUserType() ).isNull();
				assertThat( id.getColumns().stream().map( org.hibernate.mapping.Column::getName ).toList() )
						.containsExactlyInAnyOrder( "id1", "id2" );
				assertThat( dynamic.getTable().getColumns() ).hasSize( 3 );
				if ( direct )
					TemporalDeclarationAssertions.mapping( scope, Adjusted.class, "stamp", true, ZoneOffset.UTC );
				else
					assertThat( scope.metadata.getEntityBinding( Legacy.class.getName() ).getProperty( "stamp" )
							.getValue().getColumnSpan() ).isEqualTo( 2 );
				final var key = new HashMap<String, Object>();
				key.put( "id1", 1 );
				key.put( "id2", 2 );
				final var row = new HashMap<String, Object>();
				row.put( "originalId", key );
				row.put( "data", "dynamic data" );
				final var input = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
				scope.tx( session -> {
					session.persist( "CompositeIdEntity", row );
					if ( direct ) {
						var value = new Adjusted();
						value.id = 1;
						value.stamp = input;
						session.persist( value );
					} else {
						var value = new Legacy();
						value.id = 1;
						value.stamp = input;
						session.persist( value );
					}
				} );
				scope.measure();
				scope.tx( session -> {
					final var found = ( Map<?, ?> ) session.get( "CompositeIdEntity", key );
					assertThat( found.get( "data" ) ).isEqualTo( "dynamic data" );
					assertThat( ( Map<?, ?> ) found.get( "originalId" ) ).isEqualTo( key );
					if ( direct )
						assertThat( session.find( Adjusted.class, 1 ).stamp )
								.isEqualTo( input.withOffsetSameInstant( ZoneOffset.UTC ) );
					else
						assertThat( session.find( Legacy.class, 1 ).stamp ).isEqualTo( input );
				} );
				assertThat( scope.sql ).anyMatch( sql -> sql.startsWith( "select " ) );
			}
		}
	}

	@Entity( name = "DynamicAdjusted" )
	static class Adjusted {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneAdjustment( "Z" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DynamicLegacy" )
	static class Legacy {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}
}
