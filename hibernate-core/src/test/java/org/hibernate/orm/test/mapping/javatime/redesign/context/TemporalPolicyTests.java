package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.hibernate.type.internal.TemporalPolicySettings;
import org.hibernate.type.internal.TemporalPolicySettings.Selection;
import org.hibernate.type.internal.TemporalPreservationSettings;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.annotations.TemporalPreservationPolicy.Type.DOMAIN_VALUE;
import static org.hibernate.annotations.TemporalPreservationPolicy.Type.NORMALIZED;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.*;

/// Policy selection is mapping-local; normalized time is distinct from local clock time.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPolicyTests {
	@TempDir Path temporary;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void packageModuleAndUnmatchedSelectors(boolean metadata) throws Exception {
		final var sources = new LinkedHashMap<String, String>();
		final String imports = "import org.hibernate.annotations.TemporalPreservationPolicy; import static org.hibernate.annotations.TemporalPreservationPolicy.Type.*; ";
		sources.put( "policy.module.Event", TemporalRealm.entitySource( "policy.module", "Event", "", "public OffsetDateTime stamp;" ) );
		sources.put( "policy.packaged.Event", TemporalRealm.entitySource( "policy.packaged", "Event", "", "public OffsetDateTime stamp;" ).replace( "@Entity(name=\"Event\")", "@Entity(name=\"PackagedPolicy\")" ) );
		sources.put( "policy.packaged.package-info", "@TemporalPreservationPolicy(DOMAIN_VALUE) package policy.packaged; " + imports );
		try ( var realm = TemporalRealm.create( temporary.resolve( "policy" ), TemporalRealm.Kind.NAMED,
				imports + "@TemporalPreservationPolicy(NORMALIZED)", sources ) ) {
			final var module = realm.entity( "policy.module.Event" );
			final var packaged = realm.entity( "policy.packaged.Event" );
			try ( var scope = ContextScope.boot( metadata, true, module, packaged ) ) {
				assertPolicy( scope, module, "stamp", NORMALIZED );
				assertPolicy( scope, packaged, "stamp", DOMAIN_VALUE );
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void inheritanceEmbeddablesAndConverters(boolean metadata) {
		try ( var scope = ContextScope.boot( metadata, true, Child.class, Converted.class ) ) {
			assertPolicy( scope, Child.class, "stamp", DOMAIN_VALUE );
			assertPolicy( scope, Converted.class, "text", NORMALIZED );
			final var component = (Component) scope.metadata.getEntityBinding( Child.class.getName() )
					.getProperty( "embedded" ).getValue();
			assertThat( ((BasicValue) component.getProperty( "stamp" ).getValue()).getTemporalPreservationPolicy().policy() )
					.isEqualTo( DOMAIN_VALUE );
		}
	}

	@Test
	void configurationAndCompatibility() {
		final Map<String, Object> settings = Map.of(
				MappingSettings.TEMPORAL_PRESERVATION_POLICY, " normalized ",
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME, DOMAIN_VALUE );
		assertThat( TemporalPolicySettings.resolve( settings, OffsetDateTime.class ).policy() ).isEqualTo( NORMALIZED );
		assertThat( TemporalPolicySettings.resolve( settings, OffsetTime.class ).policy() ).isEqualTo( DOMAIN_VALUE );
		assertThat( TemporalPolicySettings.resolve( Map.of(), OffsetTime.class ).policy() ).isEqualTo( DOMAIN_VALUE );
		assertThat( TemporalPreservationSettings.resolve( settings ).enabled() ).isTrue();
		for ( Object invalid : new Object[] { "", "instant", true, Instant.class } ) {
			assertThatThrownBy( () -> TemporalPreservationSettings.resolve(
					Map.of( MappingSettings.TEMPORAL_PRESERVATION_POLICY, invalid ) ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "domain_value or normalized" );
		}
		assertThatThrownBy( () -> TemporalPreservationSettings.resolve( Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, false,
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME, NORMALIZED ) ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "requires" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void typedScopeAndMemberOverride(boolean metadata) {
		try ( var scope = ContextScope.boot( metadata, true, Map.of(
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME, NORMALIZED ), Scoped.class ) ) {
			assertPolicy( scope, Scoped.class, "stamp", NORMALIZED );
			assertPolicy( scope, Scoped.class, "time", DOMAIN_VALUE );
			assertPolicy( scope, Scoped.class, "strict", DOMAIN_VALUE );
		}
	}

	@Test
	void offlinePostgresqlRequirementsAndIsolation() {
		final Map<String, Object> settings = Map.of(
				"hibernate.dialect", PostgreSQLDialect.class.getName(),
				"jakarta.persistence.database-product-version", "18.0",
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME, NORMALIZED );
		try ( var scope = ContextScope.boot( false, true, settings, Plain.class ) ) {
			assertPolicy( scope, Plain.class, "stamp", NORMALIZED );
		}
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( false, true, settings, Strict.class ) ) {}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "OFFSET" ).hasMessageContaining( "DOMAIN_VALUE" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void invalidDeclarations(boolean metadata) {
		for ( Class<?> type : new Class<?>[] { Duplicate.class, InvalidMember.class, LocalNormalized.class, IdPolicy.class, AssociationPolicy.class } ) {
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, type ) ) {}
			} ).isInstanceOf( MappingException.class );
		}
	}

	@Test
	void normalizedTimeNeedsItsOwnEvidence() {
		final var localOnly = new TemporalPreservation( IRRELEVANT, SUPPORTED, IRRELEVANT, UNSUPPORTED, IRRELEVANT );
		final var strict = new TemporalPreservation( IRRELEVANT, SUPPORTED, IRRELEVANT, SUPPORTED, IRRELEVANT );
		assertThat( localOnly.preserves( TemporalPreservation.TemporalPart.NORMALIZED_TIME ) ).isEqualTo( UNKNOWN );
		assertThat( strict.preserves( TemporalPreservation.TemporalPart.NORMALIZED_TIME ) ).isEqualTo( SUPPORTED );
		final var normalized = new TemporalPreservation( IRRELEVANT, UNSUPPORTED, IRRELEVANT, UNSUPPORTED, IRRELEVANT, SUPPORTED );
		final var dialect = new H2Dialect() {
			@Override
			public TemporalPreservation getTemporalPreservation(JavaType<?> javaType,
					JdbcType jdbcType) {
				return normalized;
			}
		};
		TemporalMappingValidator.validate( dialect, OffsetTimeJavaType.INSTANCE, OffsetTimeJdbcType.INSTANCE,
				null, "normalized time", false, new Selection( NORMALIZED, "test", true ) );
		assertThatThrownBy( () -> TemporalMappingValidator.validate( dialect, OffsetTimeJavaType.INSTANCE,
				OffsetTimeJdbcType.INSTANCE, null, "strict time" ) ).isInstanceOf( MappingException.class );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void roundTripAndDirtyChecking(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of(
				MappingSettings.TEMPORAL_PRESERVATION_POLICY, NORMALIZED ), Plain.class ) ) {
			final var row = new Plain();
			row.id = 1;
			row.stamp = OffsetDateTime.parse( "2026-11-01T01:30:00-07:00" );
			row.time = OffsetTime.parse( "00:30:00+02:00" );
			scope.tx( session -> session.persist( row ) );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> {
				var loaded = session.find( Plain.class, 1 );
				assertThat( loaded.stamp.toInstant() ).isEqualTo( row.stamp.toInstant() );
				assertThat( normalizedTime( loaded.time ) ).isEqualTo( normalizedTime( row.time ) );
				assertThat( session.createQuery( "from PolicyPlain where stamp=:stamp", Plain.class )
						.setParameter( "stamp", row.stamp ).getSingleResult() ).isSameAs( loaded );
			} );
			scope.measure();
			scope.tx( session -> session.find( Plain.class, 1 ) );
			scope.noMutation();
		}
	}

	private static long normalizedTime(OffsetTime time) {
		return Math.floorMod( time.toLocalTime().toNanoOfDay() - time.getOffset().getTotalSeconds() * 1_000_000_000L,
				86_400_000_000_000L );
	}

	private static void assertPolicy(ContextScope scope, Class<?> entity, String property,
			TemporalPreservationPolicy.Type expected) {
		final var value = (BasicValue) scope.metadata.getEntityBinding( entity.getName() ).getProperty( property ).getValue();
		assertThat( value.getTemporalPreservationPolicy().policy() ).isEqualTo( expected );
	}

	@Entity(name = "PolicyPlain")
	static class Plain {
		@Id int id;
		OffsetDateTime stamp;
		OffsetTime time;
	}

	@Entity(name = "PolicyScoped")
	@TemporalPreservationPolicy(value = NORMALIZED, types = { OffsetDateTime.class, ZonedDateTime.class })
	@TemporalPreservationPolicy(value = DOMAIN_VALUE, types = OffsetTime.class)
	static class Scoped {
		@Id int id;
		OffsetDateTime stamp;
		OffsetTime time;
		@TemporalPreservationPolicy(DOMAIN_VALUE) OffsetDateTime strict;
	}

	@Entity(name = "PolicyStrict")
	static class Strict {
		@Id int id;
		@TemporalPreservationPolicy(DOMAIN_VALUE) OffsetDateTime stamp;
	}

	@Entity(name = "PolicyDuplicate")
	@TemporalPreservationPolicy(value = NORMALIZED, types = OffsetTime.class)
	@TemporalPreservationPolicy(value = DOMAIN_VALUE, types = OffsetTime.class)
	static class Duplicate { @Id int id; OffsetTime time; }

	@Entity(name = "PolicyInvalidMember")
	static class InvalidMember {
		@Id int id;
		@TemporalPreservationPolicy(value = NORMALIZED, types = OffsetTime.class) OffsetTime time;
	}

	@Entity(name = "PolicyLocalNormalized")
	static class LocalNormalized { @Id int id; @TemporalPreservationPolicy(NORMALIZED) LocalDateTime time; }

	@Entity(name = "PolicyId")
	static class IdPolicy { @Id @TemporalPreservationPolicy(DOMAIN_VALUE) OffsetTime id; }
	@MappedSuperclass
	@TemporalPreservationPolicy(DOMAIN_VALUE)
	static class Base { OffsetDateTime stamp; }

	@Embeddable
	static class EmbeddedStamp { @Column(name = "embedded_stamp") OffsetDateTime stamp; }

	@Entity(name = "PolicyChild")
	@TemporalPreservationPolicy(NORMALIZED)
	static class Child extends Base { @Id int id; @Embedded EmbeddedStamp embedded; }

	@Entity(name = "PolicyConverted")
	@TemporalPreservationPolicy(value = NORMALIZED, types = OffsetDateTime.class)
	static class Converted {
		@Id int id;
		@Convert(converter = TextDateTime.class) String text;
	}

	@Converter
	static class TextDateTime implements AttributeConverter<String, OffsetDateTime> {
		@Override public OffsetDateTime convertToDatabaseColumn(String value) { return value == null ? null : OffsetDateTime.parse( value ); }
		@Override public String convertToEntityAttribute(OffsetDateTime value) { return value == null ? null : value.toString(); }
	}

	@Entity(name = "PolicyAssociation")
	static class AssociationPolicy {
		@Id int id;
		@ManyToOne @TemporalPreservationPolicy(DOMAIN_VALUE) AssociationPolicy parent;
	}

}
