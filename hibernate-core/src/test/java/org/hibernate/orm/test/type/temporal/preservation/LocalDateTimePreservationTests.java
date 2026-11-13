package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for LocalDateTime.
///
/// @author Steve Ebersole
public class LocalDateTimePreservationTests extends AbstractPreservationTests<LocalDateTime> {
	public LocalDateTimePreservationTests() { super( LocalDateTime.class ); }
	@Override List<Mapping<LocalDateTime>> mappings() {
		return List.of(
				new Mapping<>( "Natural", Natural.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "None", None.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "Fallback", Fallback.class, Representation.DEFAULT, null, 6, true, false),
				new Mapping<>( "InvalidAdjustment", InvalidAdjustment.class, Representation.DEFAULT, "UTC", 6, false, false),
				new Mapping<>( "Seconds", Seconds.class, Representation.DEFAULT, null, 0, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "LocalDateTimeNaturalPreservation")
	@Table(name = "tp_ldt_0")
	public static class Natural extends PreservationRow<LocalDateTime> {
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}

	@Entity(name = "LocalDateTimeNonePreservation")
	@Table(name = "tp_ldt_1")
	public static class None extends PreservationRow<LocalDateTime> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}

	@Entity(name = "LocalDateTimeFallbackPreservation")
	@Table(name = "tp_ldt_2")
	public static class Fallback extends PreservationRow<LocalDateTime> {
		@JdbcType(TimestampJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}

	@Entity(name = "LocalDateTimeInvalidAdjustmentPreservation")
	@Table(name = "tp_ldt_3")
	public static class InvalidAdjustment extends PreservationRow<LocalDateTime> {
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}

	@Entity(name = "LocalDateTimeSecondsPreservation")
	@Table(name = "tp_ldt_4")
	public static class Seconds extends PreservationRow<LocalDateTime> {
		@Column(name = "temporal_value", secondPrecision = 0)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}

	@Entity(name = "LocalDateTimeUnknownPreservation")
	@Table(name = "tp_ldt_5")
	public static class Unknown extends PreservationRow<LocalDateTime> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;
		@Override LocalDateTime value() { return value; }
		@Override void value(LocalDateTime value) { this.value = value; }
	}
}
