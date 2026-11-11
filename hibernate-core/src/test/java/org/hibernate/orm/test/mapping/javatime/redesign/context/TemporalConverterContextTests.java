package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.MappingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Active user converters conflict with adjustment; disabled/NONE controls retain
/// ordinary conversion and mapping-owned context for scalar and collection values.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalConverterContextTests {
	@BeforeEach
	void requireLiveProfile() {
		TemporalLiveProfile.requireSupported();
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void noneDoesNotSilentlyDiscardAnUnsupportedWholeColumnConverter(boolean metadata) {
		assertThatThrownBy( () -> { try ( var ignored = ContextScope.boot( metadata, true, WholeColumnConverter.class ) ) {} } )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "Whole-value user converter" ).hasMessageContaining( "COLUMN" );
	}
	@Entity(name = "ContextWholeColumnConverter")
	static class WholeColumnConverter {
		@Id int id;
		@Convert(converter = Identity.class)
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetDateTime value;
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void excludedMapSkipsDefaultsWithoutSuppressingItsUserConverter(boolean metadata) {
		try(var scope=ContextScope.open(metadata,ConvertedMap.class)) {
			final var collection=(org.hibernate.mapping.Collection)scope.metadata.getEntityBinding(ConvertedMap.class.getName()).getProperty("values").getValue();
			assertThat(((org.hibernate.mapping.BasicValue)collection.getElement()).resolve().getValueConverter()).isInstanceOf(org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter.class);
			final var row=new ConvertedMap();row.id=1;final var input=OffsetDateTime.parse("2026-07-01T12:00+05:30:45");row.values.put("source",input);
			scope.tx(session->session.persist(row));
			scope.tx(session->assertThat(session.find(ConvertedMap.class,1).values).containsEntry("source",input));
		}
	}
	@Converter
	public static class Text implements AttributeConverter<OffsetDateTime,String> {
		public String convertToDatabaseColumn(OffsetDateTime value){return value==null?null:value.toString();}
		public OffsetDateTime convertToEntityAttribute(String value){return value==null?null:OffsetDateTime.parse(value);}
	}
	@Entity(name="ContextConvertedMap") @TimeZoneAdjustment("America/New_York")
	static class ConvertedMap { @Id int id; @ElementCollection @Convert(converter=Text.class) Map<String,OffsetDateTime> values=new HashMap<>(); }

	@Converter(autoApply=true)
	public static class Identity implements AttributeConverter<OffsetDateTime,OffsetDateTime> {
		public OffsetDateTime convertToDatabaseColumn(OffsetDateTime value) { return value; }
		public OffsetDateTime convertToEntityAttribute(OffsetDateTime value) { return value; }
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void activeConvertersRejectBeforeExecution(boolean metadata) {
		for(Class<?> type:List.of(ScalarExplicit.class,ScalarAuto.class,SetExplicit.class,SetAuto.class,ListExplicit.class,ListAuto.class,BagExplicit.class,BagAuto.class,ScalarColumnExplicit.class,ScalarColumnAuto.class,SetColumnExplicit.class,SetColumnAuto.class,ListColumnExplicit.class,ListColumnAuto.class,BagColumnExplicit.class,BagColumnAuto.class)) {
			assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,true,type,Identity.class)) {}})
				.as(type.getSimpleName()).isInstanceOf(MappingException.class).hasMessageContaining("converter").hasMessageContaining("adjustment");
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void disabledConverterRetainsAdjustment(boolean metadata) {
		try(var scope=ContextScope.open(metadata,DisabledControls.class,Identity.class)) {
			final var input=OffsetDateTime.parse("2026-07-01T12:00+02:00");
			final var row=new DisabledControls();row.id=1;row.adjusted=input;row.column=input;row.values.add(input);
			scope.tx(session->session.persist(row));
			scope.tx(session->{final var actual=session.find(DisabledControls.class,1);assertThat(actual.adjusted).isEqualTo(input.withOffsetSameInstant(java.time.ZoneOffset.UTC));assertThat(actual.column).isEqualTo(input.withOffsetSameInstant(java.time.ZoneOffset.UTC));assertThat(actual.values).containsExactly(input.withOffsetSameInstant(java.time.ZoneOffset.UTC));});
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void noneDoesNotCreateAConverterConflict(boolean metadata) {
		if(ContextEnvironment.is( PostgreSQLDialect.class )) {
			assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,true,Controls.class,Identity.class)) {}}).isInstanceOf(MappingException.class).hasMessageContaining("OFFSET preservation").hasMessageNotContaining("converter and timezone adjustment");
			return;
		}
		try(var scope=ContextScope.open(metadata,Controls.class,Identity.class)) {
			final var input=OffsetDateTime.parse("2026-07-01T12:00+02:00");
			final var row=new Controls();row.id=1;row.adjusted=input;row.none=input;row.values.add(input);
			scope.tx(session->session.persist(row));
			scope.tx(session->{final var actual=session.find(Controls.class,1);assertThat(actual.none).isEqualTo(input);assertThat(actual.values).containsExactly(input);});
		}
	}
	@Entity(name="ContextDisabledConverters") @TimeZoneAdjustment("Z")
	static class DisabledControls { @Id int id; @Convert(disableConversion=true) OffsetDateTime adjusted; @Convert(disableConversion=true) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime column; @ElementCollection @Convert(disableConversion=true) List<OffsetDateTime> values=new ArrayList<>(); }

	@Entity(name="ContextConverterControls") @TimeZoneAdjustment("Z")
	static class Controls {
		@Id int id;
		@Convert(disableConversion=true) OffsetDateTime adjusted;
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE) OffsetDateTime none;
		@ElementCollection @TimeZoneAdjustment(TimeZoneAdjustment.NONE) List<OffsetDateTime> values=new ArrayList<>();
	}
	@Entity(name="ContextConverterScalarExplicit") @TimeZoneAdjustment("Z") static class ScalarExplicit { @Id int id; @Convert(converter=Identity.class) OffsetDateTime value; }
	@Entity(name="ContextConverterScalarAuto") @TimeZoneAdjustment("Z") static class ScalarAuto { @Id int id; OffsetDateTime value; }
	@Entity(name="ContextConverterSetExplicit") @TimeZoneAdjustment("Z") static class SetExplicit { @Id int id; @ElementCollection @Convert(converter=Identity.class) Set<OffsetDateTime> values; }
	@Entity(name="ContextConverterSetAuto") @TimeZoneAdjustment("Z") static class SetAuto { @Id int id; @ElementCollection Set<OffsetDateTime> values; }
	@Entity(name="ContextConverterListExplicit") @TimeZoneAdjustment("Z") static class ListExplicit { @Id int id; @ElementCollection @OrderColumn @Convert(converter=Identity.class) List<OffsetDateTime> values; }
	@Entity(name="ContextConverterListAuto") @TimeZoneAdjustment("Z") static class ListAuto { @Id int id; @ElementCollection @OrderColumn List<OffsetDateTime> values; }
	@Entity(name="ContextConverterBagExplicit") @TimeZoneAdjustment("Z") static class BagExplicit { @Id int id; @ElementCollection @Convert(converter=Identity.class) List<OffsetDateTime> values; }
	@Entity(name="ContextConverterBagAuto") @TimeZoneAdjustment("Z") static class BagAuto { @Id int id; @ElementCollection List<OffsetDateTime> values; }
	@Entity(name="ContextConverterScalarColumnExplicit") @TimeZoneAdjustment("Z") static class ScalarColumnExplicit { @Id int id; @Convert(converter=Identity.class) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime value; }
	@Entity(name="ContextConverterScalarColumnAuto") @TimeZoneAdjustment("Z") static class ScalarColumnAuto { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime value; }
	@Entity(name="ContextConverterSetColumnExplicit") @TimeZoneAdjustment("Z") static class SetColumnExplicit { @Id int id; @ElementCollection @Convert(converter=Identity.class) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetDateTime> values; }
	@Entity(name="ContextConverterSetColumnAuto") @TimeZoneAdjustment("Z") static class SetColumnAuto { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetDateTime> values; }
	@Entity(name="ContextConverterListColumnExplicit") @TimeZoneAdjustment("Z") static class ListColumnExplicit { @Id int id; @ElementCollection @OrderColumn @Convert(converter=Identity.class) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> values; }
	@Entity(name="ContextConverterListColumnAuto") @TimeZoneAdjustment("Z") static class ListColumnAuto { @Id int id; @ElementCollection @OrderColumn @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> values; }
	@Entity(name="ContextConverterBagColumnExplicit") @TimeZoneAdjustment("Z") static class BagColumnExplicit { @Id int id; @ElementCollection @Convert(converter=Identity.class) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> values; }
	@Entity(name="ContextConverterBagColumnAuto") @TimeZoneAdjustment("Z") static class BagColumnAuto { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> values; }
}
