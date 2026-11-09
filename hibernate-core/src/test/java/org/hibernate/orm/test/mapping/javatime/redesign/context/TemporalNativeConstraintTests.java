package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// A legacy native constraint survives an independent adjustment. An explicit
/// scoped representation can replace the global constraint. Run on the pinned
/// H2/PostgreSQL/Oracle environments; SQL Server live verification is deferred.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialects( {
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class ),
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.PostgreSQLDialect.class ),
		@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.OracleDialect.class ) } )
class TemporalNativeConstraintTests {
	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void nativeAndPlainStorageRemainDistinct( boolean metadata ) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true,
					Map.of( "hibernate.timezone.default_storage", "NATIVE" ), TargetPlain.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "native" ).hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true,
					TemporalLegacyDeclarationTests.NativePlain.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "native" ).hasMessageContaining( "stamp" );
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.timezone.default_storage", "NATIVE" ),
				TemporalLegacyDeclarationTests.InlinePlain.class ) ) {
			final var row = new TemporalLegacyDeclarationTests.InlinePlain();
			row.id = 1;
			row.stamp = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
			scope.tx( session -> session.persist( row ) );
			scope.tx( session -> assertThat( session.find( TemporalLegacyDeclarationTests.InlinePlain.class, 1 ).stamp )
					.isEqualTo( row.stamp.withOffsetSameInstant( ZoneOffset.UTC ) ) );
		}
		try ( var scope = ContextScope.open( metadata, NativeRow.class ) ) {
			final var row = new NativeRow();
			row.id = 1;
			row.stamp = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
			scope.tx( session -> session.persist( row ) );
			scope.tx( session -> assertThat( session.find( NativeRow.class, 1 ).stamp )
					.isEqualTo( row.stamp.withOffsetSameInstant( ZoneOffset.UTC ) ) );
		}
	}

	@Entity( name = "DeclarationTargetPlain" )
	static class TargetPlain {
		@Id
		int id;
		@TimeZoneAdjustment( "Z" )
		@org.hibernate.annotations.JdbcType( org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType.class )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativeRow" )
	static class NativeRow {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		@TimeZoneAdjustment( "Z" )
		OffsetDateTime stamp;
	}
}
