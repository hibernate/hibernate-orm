package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Real aggregate binding paths reject explicit temporal requests and skip broader
/// defaults independently from ordinary reuse of the same embeddable declaration.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalAggregateExclusionTests {
	@ParameterizedTest @ValueSource(booleans={true,false})
	void jsonMembersRejectExplicitAndSkipDefaults(boolean metadata) { verify(metadata,JsonControl.class,JsonColumn.class,JsonNone.class,JsonInline.class,JsonAux.class,JsonTarget.class,JsonNestedColumn.class); }
	@ParameterizedTest @ValueSource(booleans={true,false})
	void xmlMembersRejectExplicitAndSkipDefaults(boolean metadata) { verify(metadata,XmlControl.class,XmlColumn.class,XmlNone.class,XmlInline.class,XmlAux.class,XmlTarget.class,XmlNestedColumn.class); }
	@ParameterizedTest @ValueSource(booleans={true,false})
	void structMembersRejectExplicitAndSkipDefaults(boolean metadata) { verify(metadata,StructControl.class,StructColumn.class,StructNone.class,StructInline.class,StructAux.class,StructTarget.class,StructNestedColumn.class); }
	@ParameterizedTest @ValueSource(booleans={true,false})
	void sameExplicitColumnMemberIsValidOutsideAggregate(boolean metadata) {
		try(var ignored=ContextScope.boot(metadata,true,OrdinaryExplicit.class)) {}
	}
	@Entity(name="ContextOrdinaryExplicit") static class OrdinaryExplicit { @Id int id; @Embedded ExplicitColumn member; }
	@Embeddable static class ExplicitTarget { @TimeZoneAdjustment("Z") OffsetDateTime value; }
	@Embeddable static class NestedColumn { @Embedded ExplicitColumn member; }
	static void verify(boolean metadata,Class<?> control,Class<?>... invalid) {
		// Capability is checked on a configured, connection-free Dialect, never
		// inferred from a fixture's failure. Unsupported families remain visible skips.
		try(var boot=ContextScope.boot(false,true,Capability.class)) {
			final var dialect=boot.factory.getJdbcServices().getDialect();
			final boolean supported=control==JsonControl.class?new org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsJsonAggregate().apply(dialect)
					:control==XmlControl.class?new org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsXmlAggregate().apply(dialect)
					:new org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsStructAggregate().apply(dialect);
			org.junit.jupiter.api.Assumptions.assumeTrue(supported,"Configured dialect lacks this aggregate family: "+control.getSimpleName());
		}
		for(Class<?> type:invalid) {
			assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,true,type)) {}})
				.as(type.getSimpleName()).isInstanceOf(MappingException.class).hasMessageContaining("excluded role");
		}
		try(var scope=ContextScope.open(metadata,control)) {
			final var binding=scope.metadata.getEntityBinding(control.getName());
			final var aggregate=(Component)binding.getProperty("aggregate").getValue();
			final var nested=(Component)aggregate.getProperty("member").getValue();
			assertThat(nested.getProperty("value").getValue()).isInstanceOf(BasicValue.class);
			assertThat(((BasicValue)nested.getProperty("value").getValue()).resolve().getValueConverter()).isNull();
			assertThat(nested.getContextualCompositeUserType()).isNull();
			assertThat(binding.getProperty("ordinary").getValue()).isInstanceOf(Component.class);
			final var ordinary=(Component)binding.getProperty("ordinary").getValue();
			assertThat(ordinary.getProperty("value").getValue()).isInstanceOf(Component.class);
			assertThat(((Component)ordinary.getProperty("value").getValue()).getContextualCompositeUserType()).isNotNull();
			try {
				final var row=(Holder)control.getDeclaredConstructor().newInstance();row.id=1;final var nestedValue=new Nested();nestedValue.member=new DefaultMember();nestedValue.member.value=OffsetDateTime.parse("2026-07-01T12:00Z");row.aggregate(nestedValue);row.ordinary=new DefaultMember();row.ordinary.value=nestedValue.member.value;
				scope.tx(session->session.persist(row));
				scope.tx(session->{final var actual=(Holder)session.find(control,1);assertThat(actual.aggregate().member.value.toInstant()).isEqualTo(row.aggregate().member.value.toInstant());assertThat(actual.ordinary.value).isEqualTo(row.ordinary.value.withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5,30)));});
				scope.tx(session->{final var actual=(Holder)session.find(control,1);actual.aggregate().member.value=OffsetDateTime.parse("2026-07-02T13:00Z");});
				scope.tx(session->{final var actual=(Holder)session.find(control,1);assertThat(actual.aggregate().member.value.toInstant()).isEqualTo(Instant.parse("2026-07-02T13:00:00Z"));});
				// Exercise null binding on both driver-backed and casting paths of the EDB repair.
				if ( scope.factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect ) {
					scope.tx(session->{final var actual=(Holder)session.find(control,1);actual.aggregate(null);});
					scope.tx(session->{final var actual=(Holder)session.find(control,1);assertThat(actual.aggregate()).isNull();});
				}
			}
			catch(ReflectiveOperationException ex) {throw new AssertionError(ex);}
		}
	}
	@Entity(name="ContextCapability") static class Capability { @Id int id; }
	@Embeddable @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("+05:30")
	static class DefaultMember { @Column(name="temporal_value") OffsetDateTime value; }
	@Embeddable static class Nested { @Embedded DefaultMember member; }
	@Embeddable static class ExplicitColumn { @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime value; }
	@Embeddable static class ExplicitNone { @TimeZoneAdjustment(TimeZoneAdjustment.NONE) OffsetDateTime value; }
	@Embeddable static class ExplicitInline { @TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE) OffsetDateTime value; }
	@Embeddable static class ExplicitAux { @TimeZoneColumn OffsetDateTime value; }
	@MappedSuperclass static abstract class Holder { @Id int id; abstract Nested aggregate(); abstract void aggregate(Nested value); @Embedded @AttributeOverrides({@AttributeOverride(name="value.instant",column=@Column(name="ordinary_instant")),@AttributeOverride(name="value.zoneOffset",column=@Column(name="ordinary_offset"))}) DefaultMember ordinary; }
	@Entity(name="ContextJsonControl") static class JsonControl extends Holder { @JdbcTypeCode(SqlTypes.JSON) Nested aggregate; Nested aggregate(){return aggregate;} void aggregate(Nested value){aggregate=value;} }
	@Entity(name="ContextJsonColumn") static class JsonColumn { @Id int id; @JdbcTypeCode(SqlTypes.JSON) ExplicitColumn aggregate; }
	@Entity(name="ContextJsonNone") static class JsonNone { @Id int id; @JdbcTypeCode(SqlTypes.JSON) ExplicitNone aggregate; }
	@Entity(name="ContextJsonInline") static class JsonInline { @Id int id; @JdbcTypeCode(SqlTypes.JSON) ExplicitInline aggregate; }
	@Entity(name="ContextJsonAux") static class JsonAux { @Id int id; @JdbcTypeCode(SqlTypes.JSON) ExplicitAux aggregate; }
	@Entity(name="ContextXmlControl") static class XmlControl extends Holder { @JdbcTypeCode(SqlTypes.SQLXML) Nested aggregate; Nested aggregate(){return aggregate;} void aggregate(Nested value){aggregate=value;} }
	@Entity(name="ContextXmlColumn") static class XmlColumn { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) ExplicitColumn aggregate; }
	@Entity(name="ContextXmlNone") static class XmlNone { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) ExplicitNone aggregate; }
	@Entity(name="ContextXmlInline") static class XmlInline { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) ExplicitInline aggregate; }
	@Entity(name="ContextXmlAux") static class XmlAux { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) ExplicitAux aggregate; }
	@Entity(name="ContextStructControl") static class StructControl extends Holder { @Struct(name="context_struct") Nested aggregate; Nested aggregate(){return aggregate;} void aggregate(Nested value){aggregate=value;} }
	@Entity(name="ContextStructColumn") static class StructColumn { @Id int id; @Struct(name="context_struct") ExplicitColumn aggregate; }
	@Entity(name="ContextStructNone") static class StructNone { @Id int id; @Struct(name="context_struct") ExplicitNone aggregate; }
	@Entity(name="ContextStructInline") static class StructInline { @Id int id; @Struct(name="context_struct") ExplicitInline aggregate; }
	@Entity(name="ContextStructAux") static class StructAux { @Id int id; @Struct(name="context_struct") ExplicitAux aggregate; }
	@Entity(name="ContextJsonTarget") static class JsonTarget { @Id int id; @JdbcTypeCode(SqlTypes.JSON) ExplicitTarget aggregate; }
	@Entity(name="ContextJsonNestedColumn") static class JsonNestedColumn { @Id int id; @JdbcTypeCode(SqlTypes.JSON) NestedColumn aggregate; }
	@Entity(name="ContextXmlTarget") static class XmlTarget { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) ExplicitTarget aggregate; }
	@Entity(name="ContextXmlNestedColumn") static class XmlNestedColumn { @Id int id; @JdbcTypeCode(SqlTypes.SQLXML) NestedColumn aggregate; }
	@Entity(name="ContextStructTarget") static class StructTarget { @Id int id; @Struct(name="context_struct") ExplicitTarget aggregate; }
	@Entity(name="ContextStructNestedColumn") static class StructNestedColumn { @Id int id; @Struct(name="context_struct") NestedColumn aggregate; }
}
