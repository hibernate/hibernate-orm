package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for Instant.
///
/// @author Steve Ebersole
public class InstantPreservationTests extends AbstractPreservationTests<Instant> {
	public InstantPreservationTests() { super( Instant.class ); }
	@Override List<Mapping<Instant>> mappings() {
		return List.of(
				new Mapping<>( "Natural", Natural.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "None", None.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "Fallback", Fallback.class, Representation.DEFAULT, null, 6, true, false),
				new Mapping<>( "InvalidAdjustment", InvalidAdjustment.class, Representation.DEFAULT, "UTC", 6, false, false),
				new Mapping<>( "Seconds", Seconds.class, Representation.DEFAULT, null, 0, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "InstantNaturalPreservation")
	@Table(name = "tp_instant_0")
	public static class Natural extends PreservationRow<Instant> {
		@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}

	@Entity(name = "InstantNonePreservation")
	@Table(name = "tp_instant_1")
	public static class None extends PreservationRow<Instant> {
		@TimeZoneAdjustment("<none>")
		@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}

	@Entity(name = "InstantFallbackPreservation")
	@Table(name = "tp_instant_2")
	public static class Fallback extends PreservationRow<Instant> {
		@JdbcType(TimestampUtcAsPlainJdbcTimestampJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}

	@Entity(name = "InstantInvalidAdjustmentPreservation")
	@Table(name = "tp_instant_3")
	public static class InvalidAdjustment extends PreservationRow<Instant> {
		@TimeZoneAdjustment("UTC")
		@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}

	@Entity(name = "InstantSecondsPreservation")
	@Table(name = "tp_instant_4")
	public static class Seconds extends PreservationRow<Instant> {
		@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
		@Column(name = "temporal_value", secondPrecision = 0)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}

	@Entity(name = "InstantUnknownPreservation")
	@Table(name = "tp_instant_5")
	public static class Unknown extends PreservationRow<Instant> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;
		@Override Instant value() { return value; }
		@Override void value(Instant value) { this.value = value; }
	}
}
