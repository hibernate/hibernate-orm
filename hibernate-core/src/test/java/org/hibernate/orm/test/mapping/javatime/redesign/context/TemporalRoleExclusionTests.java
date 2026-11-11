package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.MappingException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Excluded roles reject explicit requests and retain ordinary mappings under
/// broader defaults. Negatives assert mapping diagnostics, not unrelated failures.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalRoleExclusionTests {
	@BeforeEach
	void requireLiveProfile() {
		TemporalLiveProfile.requireSupported();
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void explicitExcludedDeclarationsFail(boolean metadata) {
		for(Class<?> type:List.of(IdNone.class,IdColumn.class,VersionNone.class,VersionColumn.class,EmbeddedId.class,MapNone.class,MapColumn.class,MapRep.class,ArrayNone.class,ArrayColumn.class,ArrayRep.class,BasicList.class)) {
			assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,true,type)) {}})
				.as(type.getSimpleName()).isInstanceOf(MappingException.class).hasMessageContaining("excluded role");
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void embeddingDeclarationsFailAtTheirOwnRole(boolean metadata) {
		for(Class<?> type:List.of(EmbeddingTarget.class,EmbeddingInline.class,EmbeddingColumn.class,EmbeddingAux.class)) {
			assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,true,type)) {}})
				.as(type.getSimpleName()).isInstanceOf(MappingException.class).hasMessageContaining("member");
		}
		assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,false,EmbeddingNone.class)) {}})
			.isInstanceOf(MappingException.class).hasMessageContaining("TimeZoneAdjustment");
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void broaderDefaultsSkipIdVersionMapsAndArrays(boolean metadata) {
		try(var scope=ContextScope.open(metadata,OrdinaryRoles.class,PlainRoles.class)) {
			final var entity=scope.metadata.getEntityBinding(OrdinaryRoles.class.getName());
			for(String name:List.of("id","version","array","basicList")) {
				final var value=(BasicValue)entity.getProperty(name).getValue();
				assertThat(value.resolve().getValueConverter()).as(name).isNull();
			}
			final var map=(org.hibernate.mapping.Collection)entity.getProperty("values").getValue();
			assertThat(map.getElement()).isInstanceOf(BasicValue.class);
			assertThat(((BasicValue)map.getElement()).resolve().getValueConverter()).isNull();
			assertThat(((BasicValue)((org.hibernate.mapping.IndexedCollection)map).getIndex()).resolve().getValueConverter()).isNull();
			final var input=OffsetDateTime.parse("2026-07-01T12:00Z");
			final var row=new OrdinaryRoles();row.id=LocalDate.of(2026,7,1);row.array=new OffsetDateTime[]{input};row.basicList=new ArrayList<>(List.of(input));row.values.put(input,input);
			final var control=new PlainRoles();control.id=row.id;control.array=row.array.clone();control.basicList=new ArrayList<>(row.basicList);control.values.putAll(row.values);
			scope.tx(session->{session.persist(row);session.persist(control);});
			scope.tx(session->{final var actual=session.find(OrdinaryRoles.class,row.id);final var baseline=session.find(PlainRoles.class,row.id);assertThat(actual.array).containsExactly(baseline.array);assertThat(actual.basicList).containsExactlyElementsOf(baseline.basicList);assertThat(actual.values).isEqualTo(baseline.values);assertThat(actual.version).isNotNull();});
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void directDisabledLegacyColumnKeepsItsConfiguration(boolean metadata) {
		try(var scope=ContextScope.boot(metadata,false,LegacyColumnControl.class)) {
			final var value=(Component)scope.metadata.getEntityBinding(LegacyColumnControl.class.getName()).getProperty("stamp").getValue();
			assertThat(value.getContextualCompositeUserType()).isNull();
			assertThat(value.getColumns()).extracting(org.hibernate.mapping.Column::getName).containsExactlyInAnyOrder("legacy_instant","legacy_offset");
		}
	}
	@Entity(name="ContextLegacyColumn") static class LegacyColumnControl {
		@Id int id;
		@org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name="legacy_offset") @Column(name="legacy_instant") ZonedDateTime stamp;
	}

	@Embeddable static class BadKey implements java.io.Serializable { @TimeZoneAdjustment(TimeZoneAdjustment.NONE) LocalDate day; }
	@Embeddable static class PlainMember { LocalDate day; }
	@Entity(name="ContextOrdinaryRoles") @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
	static class OrdinaryRoles {
		@Id LocalDate id; @Version Instant version;
		OffsetDateTime[] array;
		@JdbcTypeCode(SqlTypes.ARRAY) List<OffsetDateTime> basicList;
		@ElementCollection @org.hibernate.annotations.MapKeyJdbcTypeCode(SqlTypes.TIMESTAMP) Map<OffsetDateTime,OffsetDateTime> values=new HashMap<>();
	}
	@Entity(name="ContextPlainRoles")
	static class PlainRoles { @Id LocalDate id; @Version Instant version; OffsetDateTime[] array; @JdbcTypeCode(SqlTypes.ARRAY) List<OffsetDateTime> basicList; @ElementCollection @org.hibernate.annotations.MapKeyJdbcTypeCode(SqlTypes.TIMESTAMP) Map<OffsetDateTime,OffsetDateTime> values=new HashMap<>(); }
	@Entity(name="ContextExcludedIdNone") static class IdNone { @Id @TimeZoneAdjustment(TimeZoneAdjustment.NONE) Instant id; }
	@Entity(name="ContextExcludedIdColumn") static class IdColumn { @Id @TimeZoneColumn Instant id; }
	@Entity(name="ContextExcludedVersionNone") static class VersionNone { @Id int id; @Version @TimeZoneAdjustment(TimeZoneAdjustment.NONE) Instant version; }
	@Entity(name="ContextExcludedVersionColumn") static class VersionColumn { @Id int id; @Version @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Instant version; }
	@Entity(name="ContextExcludedEmbeddedId") static class EmbeddedId { @jakarta.persistence.EmbeddedId BadKey id; }
	@Entity(name="ContextExcludedMapNone") static class MapNone { @Id int id; @ElementCollection @TimeZoneAdjustment(TimeZoneAdjustment.NONE) Map<OffsetDateTime,OffsetDateTime> values; }
	@Entity(name="ContextExcludedMapColumn") static class MapColumn { @Id int id; @ElementCollection @TimeZoneColumn Map<OffsetDateTime,OffsetDateTime> values; }
	@Entity(name="ContextExcludedMapRep") static class MapRep { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE) Map<OffsetDateTime,OffsetDateTime> values; }
	@Entity(name="ContextExcludedArrayNone") static class ArrayNone { @Id int id; @TimeZoneAdjustment(TimeZoneAdjustment.NONE) OffsetDateTime[] values; }
	@Entity(name="ContextExcludedArrayColumn") static class ArrayColumn { @Id int id; @TimeZoneColumn OffsetDateTime[] values; }
	@Entity(name="ContextExcludedArrayRep") static class ArrayRep { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime[] values; }
	@Entity(name="ContextExcludedBasicList") static class BasicList { @Id int id; @JdbcTypeCode(SqlTypes.ARRAY) @TimeZoneAdjustment(TimeZoneAdjustment.NONE) List<OffsetDateTime> values; }
	@Entity(name="ContextEmbeddingTarget") static class EmbeddingTarget { @Id int id; @Embedded @TimeZoneAdjustment("Z") PlainMember member; }
	@Entity(name="ContextEmbeddingInline") static class EmbeddingInline { @Id int id; @Embedded @TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE) PlainMember member; }
	@Entity(name="ContextEmbeddingColumn") static class EmbeddingColumn { @Id int id; @Embedded @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) PlainMember member; }
	@Entity(name="ContextEmbeddingAux") static class EmbeddingAux { @Id int id; @Embedded @TimeZoneColumn PlainMember member; }
	@Entity(name="ContextEmbeddingNone") static class EmbeddingNone { @Id int id; @Embedded @TimeZoneAdjustment(TimeZoneAdjustment.NONE) PlainMember member; }
}
