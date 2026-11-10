package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Local legacy/new conflicts and independent global contributions are policy
/// assertions plus complete boot/handler validation, not inferred driver facts.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalLegacyDeclarationTests {

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void compatibleLocalDeclarations( boolean metadata ) {
		try ( var scope = ContextScope.boot( metadata, true, AuxOnly.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, AuxOnly.class, "stamp", true, ZoneId.of( "+05:30" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, LegacyColumn.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, LegacyColumn.class, "stamp", true, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, LegacyNative.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, LegacyNative.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, LegacyUtc.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, LegacyUtc.class, "stamp", false, ZoneId.of( "Z" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, ColumnMatch.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, ColumnMatch.class, "stamp", true, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, AuxMatch.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, AuxMatch.class, "stamp", true, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, NativeMatch.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, NativeMatch.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, UtcMatch.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, UtcMatch.class, "stamp", false, ZoneId.of( "Z" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, ColumnTarget.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, ColumnTarget.class, "stamp", true, ZoneId.of( "+05:30" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, NativeTarget.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, NativeTarget.class, "stamp", false, ZoneId.of( "+05:30" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, ColumnNone.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, ColumnNone.class, "stamp", true, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, NativeNone.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, NativeNone.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, UtcOverRegion.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, UtcOverRegion.class, "stamp", false, ZoneId.of( "Z" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, UtcOverNone.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, UtcOverNone.class, "stamp", false, ZoneId.of( "Z" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, ColumnOverInline.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, ColumnOverInline.class, "stamp", true,
					ZoneId.of( "+05:30" ) );
		}
		try ( var scope = ContextScope.boot( metadata, true, NativeOverColumn.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, NativeOverColumn.class, "stamp", false, ZoneId.of( "Z" ) );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void contradictoryLocalDeclarations( boolean metadata ) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, InlineColumn.class ) ) {
			}
		} ).as( "InlineColumn" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, ColumnNative.class ) ) {
			}
		} ).as( "ColumnNative" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, AuxNative.class ) ) {
			}
		} ).as( "AuxNative" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, ColumnUtc.class ) ) {
			}
		} ).as( "ColumnUtc" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, AuxUtc.class ) ) {
			}
		} ).as( "AuxUtc" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, InlineAux.class ) ) {
			}
		} ).as( "InlineAux" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Conflicting" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, UtcNone.class ) ) {
			}
		} ).as( "UtcNone" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Remove legacy" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, UtcIdentical.class ) ) {
			}
		} ).as( "UtcIdentical" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Remove legacy" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, BadNORMALIZE.class ) ) {
			}
		} ).as( "BadNORMALIZE" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Explicit local" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, BadAUTO.class ) ) {
			}
		} ).as( "BadAUTO" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Explicit local" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, BadDEFAULT.class ) ) {
			}
		} ).as( "BadDEFAULT" ).isInstanceOf( MappingException.class ).hasMessageContaining( "Explicit local" )
				.hasMessageContaining( "stamp" );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, Map.of( "hibernate.jdbc.time_zone", "Asia/Tokyo" ),
					BadNORMALIZE.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "NORMALIZE" );
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void globalReplacementAndContributions( boolean metadata ) {
		for ( String policy : List.of( "NORMALIZE", "AUTO", "DEFAULT" ) ) {
			final Map<String, Object> settings = Map.of( "hibernate.timezone.default_storage", policy );
			for ( Class<?> type : List.of( CoveredType.class, CoveredMember.class, LegacyColumn.class,
					LegacyNative.class, LegacyUtc.class, AuxMatch.class, AuxOnly.class ) ) {
				try ( var ignored = ContextScope.boot( metadata, true, settings, type ) ) {
				}
			}
			try ( var ignored = ContextScope.boot( metadata, true, settings, Unused.class ) ) {
			}
			for ( Class<?> type : List.of( Plain.class, AdjustmentOnly.class, NoneOnly.class, Mixed.class,
					MixedCollection.class ) ) {
				assertThatThrownBy( () -> {
					try ( var ignored = ContextScope.boot( metadata, true, settings, type ) ) {
					}
				} ).isInstanceOf( MappingException.class ).hasMessageContaining( "hibernate.timezone.default_storage" )
						.hasMessageContaining( policy )
						.hasMessageContaining( type == MixedCollection.class ? "entries" : "stamp" );
			}
		}
		try ( var scope = ContextScope.boot( metadata, true, Plain.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, Plain.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true, Map.of( "hibernate.timezone.default_storage", "COLUMN" ),
				Inline.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, Inline.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ), Inline.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, Inline.class, "stamp", false, ZoneOffset.UTC );
		}
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ), NoneOnly.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, NoneOnly.class, "stamp", false, null );
		}
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ), CoveredType.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, CoveredType.class, "stamp", true, ZoneOffset.UTC );
		}
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ), AdjustmentOnly.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, AdjustmentOnly.class, "stamp", false, ZoneId.of( "+05:30" ) );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void nativeConstraintIsRealAndScopedRepresentationCanRemoveIt( boolean metadata ) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, NativePlain.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "native" );
		try ( var scope = ContextScope.boot( metadata, true, Map.of( "hibernate.timezone.default_storage", "NATIVE" ),
				InlinePlain.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, InlinePlain.class, "stamp", false, ZoneOffset.UTC );
			final var value = ( BasicValue ) scope.metadata.getEntityBinding( InlinePlain.class.getName() )
					.getProperty( "stamp" ).getValue();
			assertThat( value.resolve().getJdbcType().getDdlTypeCode() ).isEqualTo( java.sql.Types.TIMESTAMP );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void falseModeRetainsLegacyButRejectsEffectiveNewDefaults( boolean metadata ) {
		for ( Class<?> type : List.of( LegacyColumn.class, LegacyNative.class, LegacyUtc.class ) )
			try ( var ignored = ContextScope.boot( metadata, false, type ) ) {
			}
		for ( Class<?> type : List.of( CoveredType.class, NoneOnly.class, AdjustmentOnly.class ) )
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, false, type ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "java_time_use_direct_jdbc" );
	}

	@Entity( name = "DeclarationLegacyColumn" )
	static class LegacyColumn {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationLegacyNative" )
	static class LegacyNative {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationLegacyUtc" )
	static class LegacyUtc {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnMatch" )
	static class ColumnMatch {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationAuxMatch" )
	static class AuxMatch {
		@Id
		int id;
		@TimeZoneColumn( name = "explicit_offset" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativeMatch" )
	static class NativeMatch {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationUtcMatch" )
	static class UtcMatch {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnTarget" )
	static class ColumnTarget {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		@TimeZoneAdjustment( "+05:30" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativeTarget" )
	static class NativeTarget {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		@TimeZoneAdjustment( "+05:30" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnNone" )
	@TimeZoneAdjustment( "Z" )
	static class ColumnNone {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativeNone" )
	@TimeZoneAdjustment( "Z" )
	static class NativeNone {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationUtcOverRegion" )
	@TimeZoneAdjustment( "America/New_York" )
	static class UtcOverRegion {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationUtcOverNone" )
	@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
	static class UtcOverNone {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnOverInline" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
	@TimeZoneAdjustment( "+05:30" )
	static class ColumnOverInline {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativeOverColumn" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
	@TimeZoneAdjustment( "Z" )
	static class NativeOverColumn {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationInlineColumn" )
	static class InlineColumn {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		@TimeZoneStorage( TimeZoneStorageType.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnNative" )
	static class ColumnNative {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationAuxNative" )
	static class AuxNative {
		@Id
		int id;
		@TimeZoneColumn
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationColumnUtc" )
	static class ColumnUtc {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationAuxUtc" )
	static class AuxUtc {
		@Id
		int id;
		@TimeZoneColumn
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationInlineAux" )
	static class InlineAux {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		@TimeZoneColumn
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationUtcNone" )
	static class UtcNone {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationUtcIdentical" )
	static class UtcIdentical {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		@TimeZoneAdjustment( "Z" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationBadNORMALIZE" )
	static class BadNORMALIZE {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationBadAUTO" )
	static class BadAUTO {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.AUTO )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationBadDEFAULT" )
	static class BadDEFAULT {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.DEFAULT )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationPlain" )
	static class Plain {
		@Id
		int id;
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationInline" )
	static class Inline {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationCoveredType" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
	static class CoveredType {
		@Id
		int id;
		OffsetDateTime stamp;
		@ElementCollection
		List<OffsetDateTime> entries;
	}

	@Entity( name = "DeclarationCoveredMember" )
	static class CoveredMember {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationAdjustmentOnly" )
	static class AdjustmentOnly {
		@Id
		int id;
		@TimeZoneAdjustment( "+05:30" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNoneOnly" )
	static class NoneOnly {
		@Id
		int id;
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationMixed" )
	static class Mixed {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime covered;
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationMixedCollection" )
	static class MixedCollection {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;
		@ElementCollection
		List<OffsetDateTime> entries;
	}

	@Entity( name = "DeclarationUnused" )
	static class Unused {
		@Id
		int id;
		LocalDate date;
		Instant instant;
	}

	@Entity( name = "DeclarationAuxOnly" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
	@TimeZoneAdjustment( "+05:30" )
	static class AuxOnly {
		@Id
		int id;
		@TimeZoneColumn( name = "aux_only" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationNativePlain" )
	static class NativePlain {
		@Id
		int id;
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		@TimeZoneAdjustment( "Z" )
		@JdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.class )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationInlinePlain" )
	static class InlinePlain {
		@Id
		int id;
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		@TimeZoneAdjustment( "Z" )
		@JdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.class )
		OffsetDateTime stamp;
	}
}
