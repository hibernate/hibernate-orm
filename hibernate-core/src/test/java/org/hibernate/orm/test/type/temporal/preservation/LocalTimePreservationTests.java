package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for LocalTime.
///
/// @author Steve Ebersole
public class LocalTimePreservationTests extends AbstractPreservationTests<LocalTime> {
	public LocalTimePreservationTests() { super( LocalTime.class ); }
	@Override List<Mapping<LocalTime>> mappings() {
		return List.of(
				new Mapping<>( "RawDirect", RawDirect.class, Representation.DEFAULT, null, 6, false, false ),
				new Mapping<>( "Natural", Natural.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "None", None.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "Fallback", Fallback.class, Representation.DEFAULT, null, 6, true, false),
				new Mapping<>( "InvalidAdjustment", InvalidAdjustment.class, Representation.DEFAULT, "UTC", 6, false, false),
				new Mapping<>( "Seconds", Seconds.class, Representation.DEFAULT, null, 0, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "LocalTimeNaturalPreservation")
	@Table(name = "tp_ltime_0")
	public static class Natural extends PreservationRow<LocalTime> {
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

	@Entity(name = "LocalTimeNonePreservation")
	@Table(name = "tp_ltime_1")
	public static class None extends PreservationRow<LocalTime> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

	@Entity(name = "LocalTimeFallbackPreservation")
	@Table(name = "tp_ltime_2")
	public static class Fallback extends PreservationRow<LocalTime> {
		@JdbcType(TimeJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

	@Entity(name = "LocalTimeInvalidAdjustmentPreservation")
	@Table(name = "tp_ltime_3")
	public static class InvalidAdjustment extends PreservationRow<LocalTime> {
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

	@Entity(name = "LocalTimeSecondsPreservation")
	@Table(name = "tp_ltime_4")
	public static class Seconds extends PreservationRow<LocalTime> {
		@Column(name = "temporal_value", secondPrecision = 0)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

	@Entity(name = "LocalTimeUnknownPreservation")
	@Table(name = "tp_ltime_5")
	public static class Unknown extends PreservationRow<LocalTime> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}
	@Entity(name = "LocalTimeRawDirectPreservation")
	@Table(name = "tp_ltime_raw")
	public static class RawDirect extends PreservationRow<LocalTime> {
		@JdbcType(org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;
		@Override LocalTime value() { return value; }
		@Override void value(LocalTime value) { this.value = value; }
	}

}
