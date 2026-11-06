package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Bootstrap checks for applicability, declaration context and compatibility boundaries.
///
/// @author Steve Ebersole
class TemporalBootstrapTests {
	@Test
	void inheritedAttributesKeepDeclarationContext() {
		try ( var registry = registry( true ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( Child.class ).buildMetadata();
			final var entity = metadata.getEntityBinding( Child.class.getName() );
			final var base = (BasicValue) entity.getProperty( "inherited" ).getValue();
			final var child = (BasicValue) entity.getProperty( "declared" ).getValue();
			final var none = (BasicValue) entity.getProperty( "unadjusted" ).getValue();
			assertThat( ((TemporalAdjustmentConverter<?>) base.resolve().getValueConverter()).getTarget() ).isEqualTo( ZoneOffset.UTC );
			assertThat( ((TemporalAdjustmentConverter<?>) child.resolve().getValueConverter()).getTarget() ).isEqualTo( ZoneId.of( "America/New_York" ) );
			assertThat( none.resolve().getValueConverter() ).isNull();
		}
	}

	@Test
	void explicitInapplicableRepresentationFails() {
		assertBootFailure( InvalidRepresentation.class, true, "not applicable" );
	}

	@Test
	void explicitInapplicableAdjustmentFails() {
		assertBootFailure( InvalidAdjustment.class, true, "not applicable" );
	}

	@Test
	void noneStillRequiresDirectEnabledPath() {
		assertBootFailure( ExplicitNone.class, false, "java_time_use_direct_jdbc=true" );
	}

	@Test
	void newDeclarationsOnIdentifiersFail() {
		assertBootFailure( AdjustedId.class, true, "excluded role" );
		assertBootFailure( ColumnId.class, true, "excluded role" );
	}

	@Test
	void fallbackCannotReduceResolvedPrecision() {
		assertBootFailure( InsufficientPrecision.class, true, "millisecond precision" );
	}

	@Test
	void genericAttributeUsesItsDeclaringScope() {
		try ( var registry = registry( true ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( GenericChild.class ).buildMetadata();
			final var value = (BasicValue) metadata.getEntityBinding( GenericChild.class.getName() ).getProperty( "value" ).getValue();
			assertThat( value.resolve().getValueConverter() ).isInstanceOf( TemporalAdjustmentConverter.class );
			assertThat( ((TemporalAdjustmentConverter<?>) value.resolve().getValueConverter()).getTarget() ).isEqualTo( ZoneOffset.ofHours( 5 ) );
			final var copy = value.copy();
			assertThat( ((TemporalAdjustmentConverter<?>) copy.resolve().getValueConverter()).getTarget() ).isEqualTo( ZoneOffset.ofHours( 5 ) );
		}
	}

	@Test
	void untranslatableGlobalMayBeCompletelyOverridden() {
		for ( String policy : new String[] { "NORMALIZE", "AUTO", "DEFAULT" } ) {
			try ( var registry = registry( true ).applySetting( MappingSettings.TIMEZONE_DEFAULT_STORAGE, policy ).build() ) {
				new MetadataSources( registry ).addAnnotatedClass( CoveredGlobal.class ).buildMetadata();
			}
		}
	}

	@Test
	void adjustmentAloneDoesNotOverrideLegacyGlobalStorage() {
		try ( var registry = registry( true ).applySetting( MappingSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE" ).build() ) {
			assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( UncoveredGlobal.class ).buildMetadata() )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "choose representation and adjustment explicitly" );
		}
	}

	@Test
	void columnDefinitionRemainsApplicationResponsibility() {
		try ( var registry = registry( true ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( DefinitionOverride.class ).buildMetadata();
			final var value = (BasicValue) metadata.getEntityBinding( DefinitionOverride.class.getName() ).getProperty( "timestamp" ).getValue();
			assertThat( value.resolve().getJdbcType() ).isInstanceOf( org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType.class );
		}
	}

	@Test
	void columnOverridesCannotMakeOnlyOneComponentWritable() {
		try ( var registry = registry( true ).build() ) {
			assertThatThrownBy( () -> {
				final var metadata = new MetadataSources( registry ).addAnnotatedClass( PartialColumnWrite.class ).buildMetadata();
				metadata.getEntityBinding( PartialColumnWrite.class.getName() ).getProperty( "timestamp" ).getType();
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "matching insertability" );
		}
	}

	@Test
	void explicitDeclarationsOnMapsAndArraysFail() {
		assertBootFailure( AdjustedMap.class, true, "excluded role" );
		assertBootFailure( AdjustedArray.class, true, "excluded role" );
	}

	@Test
	void broaderDefaultsSkipMapAndArrayMappings() {
		try ( var registry = registry( true ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( ExcludedContainers.class ).buildMetadata();
			final var entity = metadata.getEntityBinding( ExcludedContainers.class.getName() );
			final var array = (BasicValue) entity.getProperty( "timestamps" ).getValue();
			assertThat( array.resolve().getValueConverter() ).isNull();
			final var map = (org.hibernate.mapping.Collection) entity.getProperty( "values" ).getValue();
			assertThat( ((BasicValue) map.getElement()).resolve().getValueConverter() ).isNull();
		}
	}

	@Entity
	static class AdjustedMap {
		@Id Long id;
		@jakarta.persistence.ElementCollection
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		java.util.Map<String, OffsetDateTime> values;
	}

	@Entity
	static class AdjustedArray {
		@Id Long id;
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		OffsetDateTime[] timestamps;
	}

	@Entity
	@TimeZoneAdjustment("Z")
	static class ExcludedContainers {
		@Id Long id;
		OffsetDateTime[] timestamps;
		@jakarta.persistence.ElementCollection
		java.util.Map<String, OffsetDateTime> values;
	}

	@Test
	void missingInstantFailsBootForInlineAndColumn() {
		for ( Class<?> dialect : new Class<?>[] { UnknownInstantDialect.class, UnsupportedInstantDialect.class } ) {
			for ( Class<?> entity : new Class<?>[] { MissingInstantInline.class, MissingInstantColumn.class } ) {
				try ( var registry = registry( true ).applySetting( "hibernate.dialect", dialect ).build() ) {
					assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( entity ).buildMetadata() )
							.isInstanceOf( MappingException.class ).hasMessageContaining( "INSTANT" );
				}
			}
		}
	}

	public static class UnknownInstantDialect extends H2Dialect {
		@Override
		public org.hibernate.dialect.temporaltype.spi.TemporalPreservation getTemporalPreservation(
				org.hibernate.type.descriptor.java.JavaType<?> javaType, org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
			return missingInstant( org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.UNKNOWN );
		}
	}

	public static class UnsupportedInstantDialect extends H2Dialect {
		@Override
		public org.hibernate.dialect.temporaltype.spi.TemporalPreservation getTemporalPreservation(
				org.hibernate.type.descriptor.java.JavaType<?> javaType, org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
			return missingInstant( org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.UNSUPPORTED );
		}
	}

	private static org.hibernate.dialect.temporaltype.spi.TemporalPreservation missingInstant(
			org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level level) {
		final var supported = org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.SUPPORTED;
		return new org.hibernate.dialect.temporaltype.spi.TemporalPreservation( supported, supported, level, supported, supported );
	}

	@Entity
	@TimeZoneAdjustment("America/New_York")
	static class MissingInstantInline {
		@Id Long id;
		OffsetDateTime timestamp;
	}

	@Entity
	@TimeZoneAdjustment("America/New_York")
	static class MissingInstantColumn {
		@Id Long id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetDateTime timestamp;
	}

	private static void assertBootFailure(Class<?> entity, boolean direct, String message) {
		try ( var registry = registry( direct ).build() ) {
			assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( entity ).buildMetadata() )
					.isInstanceOf( MappingException.class ).hasMessageContaining( message );
		}
	}

	private static org.hibernate.boot.registry.StandardServiceRegistryBuilder registry(boolean direct) {
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, direct )
				.applySetting( "hibernate.dialect", H2Dialect.class );
	}

	@MappedSuperclass
	@TimeZoneAdjustment("Z")
	static class Base {
		OffsetDateTime inherited;
	}

	@Entity
	@TimeZoneAdjustment("America/New_York")
	static class Child extends Base {
		@Id Long id;
		OffsetDateTime declared;
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		OffsetDateTime unadjusted;
	}

	@Entity
	static class InvalidRepresentation {
		@Id Long id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		LocalDate value;
	}

	@Entity
	static class InvalidAdjustment {
		@Id Long id;
		@TimeZoneAdjustment("+05:00")
		Instant value;
	}

	@Entity
	static class ExplicitNone {
		@Id Long id;
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		OffsetDateTime value;
	}

	@Entity
	static class AdjustedId {
		@Id
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		Long id;
	}
	@Entity
	static class ColumnId {
		@Id
		@org.hibernate.annotations.TimeZoneColumn
		OffsetDateTime id;
	}

	@Entity
	static class InsufficientPrecision {
		@Id Long id;
		@org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIME)
		@jakarta.persistence.Column(secondPrecision = 6)
		java.time.LocalTime value;
	}

	@MappedSuperclass
	@TimeZoneAdjustment("+05:00")
	static class GenericBase<T> {
		@Id Long id;
		T value;
	}

	@Entity
	static class GenericChild extends GenericBase<OffsetDateTime> {
	}

	@Entity
	@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
	@TimeZoneAdjustment("America/New_York")
	static class CoveredGlobal {
		@Id Long id;
		OffsetDateTime timestamp;
	}

	@Entity
	@TimeZoneAdjustment("America/New_York")
	static class UncoveredGlobal {
		@Id Long id;
		OffsetDateTime timestamp;
	}

	@Entity
	static class DefinitionOverride {
		@Id Long id;
		@jakarta.persistence.Column(columnDefinition = "timestamp")
		OffsetDateTime timestamp;
	}

	@Entity
	static class PartialColumnWrite {
		@Id Long id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@jakarta.persistence.AttributeOverride(name = "instant", column = @jakarta.persistence.Column(name = "primary_time", insertable = false))
		OffsetDateTime timestamp;
	}

}
