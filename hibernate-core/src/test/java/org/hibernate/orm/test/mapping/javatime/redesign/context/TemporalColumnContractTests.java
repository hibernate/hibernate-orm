package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.io.Serializable;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.UserComponentType;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Mapping-local sizing and cache contracts are resolved without boot-time
/// connections. Compatible factories share payloads; different adjustments do not.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalColumnContractTests {
	@Test
	void automaticAndExplicitSizingWithoutMetadata() {
		try ( var scope = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", true ), Sized.class ) ) {
			final var entity = scope.metadata.getEntityBinding( Sized.class.getName() );
			for ( var entry : Map.of( "automatic", 6, "seconds", 0, "millis", 3, "micros", 6, "nanos", 9 ).entrySet() ) {
				final var component = (Component) entity.getProperty( entry.getKey() ).getValue();
				final var primary = (BasicValue) component.getProperties().get( 0 ).getValue();
				final var temporal = TemporalPrecisionBasicType.find( primary.resolve().getJdbcMapping() );
				assertThat( primary.getColumns().get( 0 ).getColumnSize( scope.metadata.getDatabase().getDialect(), scope.metadata ).getPrecision() )
						.isEqualTo( entry.getValue() );
				assertThat( temporal ).isNotNull();
				assertThat( temporal.getPrecisionPlan().getContext().precision() ).isEqualTo( entry.getValue() );
			}
			final var physical = (Component) entity.getProperty( "physical" ).getValue();
			final var primary = (BasicValue) physical.getProperties().get( 0 ).getValue();
			assertThat( TemporalPrecisionBasicType.find( primary.resolve().getJdbcMapping() ).prepareRelational(
					OffsetDateTime.parse( "2026-01-02T12:34:56.123456789Z" ).toInstant() ) )
					.isEqualTo( OffsetDateTime.parse( "2026-01-02T12:34:56.123Z" ).toInstant() );
			assertThat( physical.getProperties().get( 1 ).getValue().getColumns().get( 0 ).getSqlType( scope.metadata ) )
					.isEqualTo( "integer" );
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	@Test
	void legacyColumnDeclarationRetainsFalseMode() {
		try ( var scope = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", false ), Legacy.class ) ) {
			final var component = (Component) scope.metadata.getEntityBinding( Legacy.class.getName() ).getProperty( "stamp" ).getValue();
			final var primary = (BasicValue) component.getProperties().get( 0 ).getValue();
			assertThat( TemporalPrecisionBasicType.find( primary.resolve().getJdbcMapping() ) ).isNull();
		}
	}

	@Test
	void keyMappingsRemainExcludedBesideOrdinaryAttributes() {
		try ( var scope = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", true ),
				TemporalKeyPrecisionCompatibilityTests.TargetRow.class,
				TemporalKeyPrecisionCompatibilityTests.Composite.class,
				TemporalKeyPrecisionCompatibilityTests.CompositeNatural.class,
				TemporalKeyPrecisionCompatibilityTests.Generated.class ) ) {
			final var target = scope.metadata.getEntityBinding( TemporalKeyPrecisionCompatibilityTests.TargetRow.class.getName() );
			assertThat( TemporalPrecisionBasicType.find( ((BasicValue) target.getIdentifier()).resolve().getJdbcMapping() ) ).isNull();
			assertThat( TemporalPrecisionBasicType.find( ((BasicValue) target.getProperty( "natural" ).getValue()).resolve().getJdbcMapping() ) ).isNull();
			assertThat( TemporalPrecisionBasicType.find( ((BasicValue) target.getProperty( "ordinary" ).getValue()).resolve().getJdbcMapping() ) ).isNotNull();
			final var composite = scope.metadata.getEntityBinding( TemporalKeyPrecisionCompatibilityTests.Composite.class.getName() );
			final var natural = scope.metadata.getEntityBinding( TemporalKeyPrecisionCompatibilityTests.CompositeNatural.class.getName() );
			for ( var component : new Component[] { (Component) composite.getIdentifier(), (Component) natural.getProperty( "natural" ).getValue() } ) {
				final var time = (BasicValue) component.getProperty( "time" ).getValue();
				assertThat( TemporalPrecisionBasicType.find( time.resolve().getJdbcMapping() ) ).isNull();
			}
			final var generated = scope.metadata.getEntityBinding( TemporalKeyPrecisionCompatibilityTests.Generated.class.getName() );
			assertThat( TemporalPrecisionBasicType.find( ((BasicValue) generated.getIdentifier()).resolve().getJdbcMapping() ) ).isNull();
		}
	}

	@Test
	void serializedPayloadAndAdjustmentIsolation() {
		try ( var first = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", true ), Sized.class );
				var second = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", true ), Sized.class ) ) {
			final var firstType = (UserComponentType<?>) first.factory.getMappingMetamodel().getEntityDescriptor( Sized.class ).getPropertyType( "millis" );
			final var same = (UserComponentType<?>) second.factory.getMappingMetamodel().getEntityDescriptor( Sized.class ).getPropertyType( "millis" );
			final var different = (UserComponentType<?>) second.factory.getMappingMetamodel().getEntityDescriptor( Sized.class ).getPropertyType( "normalized" );
			final var input = OffsetDateTime.parse( "2026-01-02T12:34:56.123456789+05:45" );
			final var payload = (Serializable) SerializationHelper.clone( firstType.disassemble( input, first.factory ) );
			try ( var session = second.factory.openSession() ) {
				assertThat( same.assemble( payload, session, null ) ).isEqualTo( input.withNano( 123000000 ) );
				assertThatThrownBy( () -> different.assemble( payload, session, null ) ).isInstanceOf( MappingException.class )
						.hasMessageContaining( "Incompatible temporal COLUMN cache payload" );
				assertThatThrownBy( () -> same.assemble( input, session, null ) ).isInstanceOf( MappingException.class );
			}
		}
	}

	@Test
	void contradictoryPhysicalDefinitionRemainsRejected() {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( false, false, Map.of( "hibernate.type.temporal_preservation", true ), Contradictory.class ) ) { }
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "physical column precision 3" );
	}

	@Entity(name = "ColumnSized")
	static class Sized {
		@Id int id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime automatic;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(columnDefinition = "timestamp(3) with time zone") OffsetDateTime physical;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @Column(secondPrecision = 0) OffsetDateTime seconds;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @Column(secondPrecision = 3) OffsetDateTime millis;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @Column(secondPrecision = 6) OffsetDateTime micros;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @Column(secondPrecision = 9) OffsetDateTime nanos;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("Z") @Column(secondPrecision = 3) OffsetDateTime normalized;
	}
	@Entity(name = "ColumnContradictory") static class Contradictory {
		@Id int id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(columnDefinition = "timestamp(3) with time zone", secondPrecision = 6) OffsetDateTime stamp;
	}
	@Entity(name = "ColumnLegacy") static class Legacy {
		@Id int id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(secondPrecision = 3) OffsetDateTime stamp;
	}

}
