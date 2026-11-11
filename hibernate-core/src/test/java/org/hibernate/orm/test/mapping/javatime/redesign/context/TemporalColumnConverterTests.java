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
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Whole-value auto-applied converters cannot be discarded by synthetic COLUMN storage.
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalColumnConverterTests {
	@BeforeEach
	void requireLiveProfile() {
		TemporalLiveProfile.requireSupported();
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void rejectsWholeValueConvertersWithoutAdjustment(boolean metadata) {
		for (Class<?> model : List.of(OffsetDateTimeScalarAbsent.class, OffsetDateTimeScalarNone.class, OffsetDateTimeSetAbsent.class, OffsetDateTimeSetNone.class, OffsetDateTimeListAbsent.class, OffsetDateTimeListNone.class, OffsetDateTimeBagAbsent.class, OffsetDateTimeBagNone.class, OffsetTimeScalarAbsent.class, OffsetTimeScalarNone.class, OffsetTimeSetAbsent.class, OffsetTimeSetNone.class, ZonedDateTimeScalarAbsent.class, ZonedDateTimeScalarNone.class, ZonedDateTimeListAbsent.class, ZonedDateTimeListNone.class)) {
			assertThatThrownBy(() -> {try (var ignored=ContextScope.boot(metadata,true,model,DateTimeText.class,TimeText.class,ZonedText.class)) {}})
				.as(model.getSimpleName()).isInstanceOf(MappingException.class)
				.hasMessageContaining("Whole-value user converter").hasMessageContaining("COLUMN")
				.hasMessageContaining("value").hasMessageContaining("INLINE").hasMessageContaining("disable conversion");
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void identityConverterAlsoRejects(boolean metadata) {
		assertThatThrownBy(() -> {try(var ignored=ContextScope.boot(metadata,true,OffsetDateTimeScalarNone.class,TemporalConverterContextTests.Identity.class)) {}})
			.isInstanceOf(MappingException.class).hasMessageContaining("Whole-value user converter").hasMessageContaining("COLUMN");
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void inlineConverterExecutesAndDisabledColumnConverterDoesNot(boolean metadata) {
		try(var scope=ContextScope.open(metadata,Controls.class,DateTimeText.class)) {
			DateTimeText.calls=0;
			var value=OffsetDateTime.parse("2026-07-01T12:00+02:00");
			scope.tx(s->{var row=new Controls();row.id=1;row.inline=value;row.column=value;s.persist(row);});
			assertThat(DateTimeText.calls).isGreaterThan(0);
			scope.tx(s->{var row=s.find(Controls.class,1);assertThat(row.inline).isEqualTo(value);assertThat(row.column).isEqualTo(value);});
		}
	}
	@Converter(autoApply=true) public static class DateTimeText implements AttributeConverter<OffsetDateTime,String> {
		static int calls;
		public String convertToDatabaseColumn(OffsetDateTime value){calls++;return value==null?null:value.toString();}
		public OffsetDateTime convertToEntityAttribute(String value){return value==null?null:OffsetDateTime.parse(value);}
	}
	@Converter(autoApply=true) public static class TimeText implements AttributeConverter<OffsetTime,String> {
		public String convertToDatabaseColumn(OffsetTime value){return value==null?null:value.toString();}
		public OffsetTime convertToEntityAttribute(String value){return value==null?null:OffsetTime.parse(value);}
	}
	@Converter(autoApply=true) public static class ZonedText implements AttributeConverter<ZonedDateTime,String> {
		public String convertToDatabaseColumn(ZonedDateTime value){return value==null?null:value.toString();}
		public ZonedDateTime convertToEntityAttribute(String value){return value==null?null:ZonedDateTime.parse(value);}
	}
	@Entity(name="ColumnConverterControls") static class Controls {
		@Id int id;
		OffsetDateTime inline;
		@Convert(disableConversion=true) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime column;
	}
	@Entity(name="OffsetDateTimeScalarAbsent") static class OffsetDateTimeScalarAbsent { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime value; }
	@Entity(name="OffsetDateTimeScalarNone") @TimeZoneAdjustment("Z") static class OffsetDateTimeScalarNone { @Id int id; @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetDateTime value; }
	@Entity(name="OffsetDateTimeSetAbsent") static class OffsetDateTimeSetAbsent { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetDateTime> value; }
	@Entity(name="OffsetDateTimeSetNone") @TimeZoneAdjustment("Z") static class OffsetDateTimeSetNone { @Id int id; @ElementCollection @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetDateTime> value; }
	@Entity(name="OffsetDateTimeListAbsent") static class OffsetDateTimeListAbsent { @Id int id; @ElementCollection @OrderColumn @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> value; }
	@Entity(name="OffsetDateTimeListNone") @TimeZoneAdjustment("Z") static class OffsetDateTimeListNone { @Id int id; @ElementCollection @OrderColumn @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> value; }
	@Entity(name="OffsetDateTimeBagAbsent") static class OffsetDateTimeBagAbsent { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> value; }
	@Entity(name="OffsetDateTimeBagNone") @TimeZoneAdjustment("Z") static class OffsetDateTimeBagNone { @Id int id; @ElementCollection @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<OffsetDateTime> value; }
	@Entity(name="OffsetTimeScalarAbsent") static class OffsetTimeScalarAbsent { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetTime value; }
	@Entity(name="OffsetTimeScalarNone") @TimeZoneAdjustment("Z") static class OffsetTimeScalarNone { @Id int id; @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) OffsetTime value; }
	@Entity(name="OffsetTimeSetAbsent") static class OffsetTimeSetAbsent { @Id int id; @ElementCollection @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetTime> value; }
	@Entity(name="OffsetTimeSetNone") @TimeZoneAdjustment("Z") static class OffsetTimeSetNone { @Id int id; @ElementCollection @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) Set<OffsetTime> value; }
	@Entity(name="ZonedDateTimeScalarAbsent") static class ZonedDateTimeScalarAbsent { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) ZonedDateTime value; }
	@Entity(name="ZonedDateTimeScalarNone") @TimeZoneAdjustment("Z") static class ZonedDateTimeScalarNone { @Id int id; @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) ZonedDateTime value; }
	@Entity(name="ZonedDateTimeListAbsent") static class ZonedDateTimeListAbsent { @Id int id; @ElementCollection @OrderColumn @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<ZonedDateTime> value; }
	@Entity(name="ZonedDateTimeListNone") @TimeZoneAdjustment("Z") static class ZonedDateTimeListNone { @Id int id; @ElementCollection @OrderColumn @TimeZoneAdjustment(TimeZoneAdjustment.NONE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<ZonedDateTime> value; }
}
