package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for OffsetTime.
///
/// @author Steve Ebersole
public class OffsetTimePreservationTests extends AbstractPreservationTests<OffsetTime> {
	public OffsetTimePreservationTests() { super( OffsetTime.class ); }
	@Override List<Mapping<OffsetTime>> mappings() {
		return List.of(
				new Mapping<>( "DefaultOriginal", DefaultOriginal.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "DefaultNone", DefaultNone.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "DefaultUtc", DefaultUtc.class, Representation.DEFAULT, "Z", 6, false, false),
				new Mapping<>( "DefaultFixed", DefaultFixed.class, Representation.DEFAULT, "+05:30", 6, false, false),
				new Mapping<>( "DefaultRegion", DefaultRegion.class, Representation.DEFAULT, "America/New_York", 6, false, false),
				new Mapping<>( "InlineOriginal", InlineOriginal.class, Representation.INLINE, null, 6, false, false),
				new Mapping<>( "InlineNone", InlineNone.class, Representation.INLINE, "<none>", 6, false, false),
				new Mapping<>( "InlineUtc", InlineUtc.class, Representation.INLINE, "Z", 6, false, false),
				new Mapping<>( "InlineFixed", InlineFixed.class, Representation.INLINE, "+05:30", 6, false, false),
				new Mapping<>( "InlineRegion", InlineRegion.class, Representation.INLINE, "America/New_York", 6, false, false),
				new Mapping<>( "ColumnOriginal", ColumnOriginal.class, Representation.COLUMN, null, 6, false, false),
				new Mapping<>( "ColumnNone", ColumnNone.class, Representation.COLUMN, "<none>", 6, false, false),
				new Mapping<>( "ColumnUtc", ColumnUtc.class, Representation.COLUMN, "Z", 6, false, false),
				new Mapping<>( "ColumnFixed", ColumnFixed.class, Representation.COLUMN, "+05:30", 6, false, false),
				new Mapping<>( "ColumnRegion", ColumnRegion.class, Representation.COLUMN, "America/New_York", 6, false, false),
				new Mapping<>( "InlineOriginalSeconds", InlineOriginalSeconds.class, Representation.INLINE, null, 0, false, false),
				new Mapping<>( "InlineUtcSeconds", InlineUtcSeconds.class, Representation.INLINE, "Z", 0, false, false),
				new Mapping<>( "InlineRegionSeconds", InlineRegionSeconds.class, Representation.INLINE, "America/New_York", 0, false, false),
				new Mapping<>( "ColumnOriginalSeconds", ColumnOriginalSeconds.class, Representation.COLUMN, null, 0, false, false),
				new Mapping<>( "ColumnUtcSeconds", ColumnUtcSeconds.class, Representation.COLUMN, "Z", 0, false, false),
				new Mapping<>( "ColumnRegionSeconds", ColumnRegionSeconds.class, Representation.COLUMN, "America/New_York", 0, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "OffsetTimeDefaultOriginalPreservation")
	@Table(name = "tp_otime_0")
	public static class DefaultOriginal extends PreservationRow<OffsetTime> {
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeDefaultNonePreservation")
	@Table(name = "tp_otime_1")
	public static class DefaultNone extends PreservationRow<OffsetTime> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeDefaultUtcPreservation")
	@Table(name = "tp_otime_2")
	public static class DefaultUtc extends PreservationRow<OffsetTime> {
		@TimeZoneAdjustment("Z")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeDefaultFixedPreservation")
	@Table(name = "tp_otime_3")
	public static class DefaultFixed extends PreservationRow<OffsetTime> {
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeDefaultRegionPreservation")
	@Table(name = "tp_otime_4")
	public static class DefaultRegion extends PreservationRow<OffsetTime> {
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineOriginalPreservation")
	@Table(name = "tp_otime_5")
	public static class InlineOriginal extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineNonePreservation")
	@Table(name = "tp_otime_6")
	public static class InlineNone extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineUtcPreservation")
	@Table(name = "tp_otime_7")
	public static class InlineUtc extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("Z")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineFixedPreservation")
	@Table(name = "tp_otime_8")
	public static class InlineFixed extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineRegionPreservation")
	@Table(name = "tp_otime_9")
	public static class InlineRegion extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnOriginalPreservation")
	@Table(name = "tp_otime_10")
	public static class ColumnOriginal extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnNonePreservation")
	@Table(name = "tp_otime_11")
	public static class ColumnNone extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnUtcPreservation")
	@Table(name = "tp_otime_12")
	public static class ColumnUtc extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("Z")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnFixedPreservation")
	@Table(name = "tp_otime_13")
	public static class ColumnFixed extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnRegionPreservation")
	@Table(name = "tp_otime_14")
	public static class ColumnRegion extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineOriginalSecondsPreservation")
	@Table(name = "tp_otime_15")
	public static class InlineOriginalSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineUtcSecondsPreservation")
	@Table(name = "tp_otime_16")
	public static class InlineUtcSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("Z")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeInlineRegionSecondsPreservation")
	@Table(name = "tp_otime_17")
	public static class InlineRegionSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnOriginalSecondsPreservation")
	@Table(name = "tp_otime_18")
	public static class ColumnOriginalSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnUtcSecondsPreservation")
	@Table(name = "tp_otime_19")
	public static class ColumnUtcSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("Z")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeColumnRegionSecondsPreservation")
	@Table(name = "tp_otime_20")
	public static class ColumnRegionSeconds extends PreservationRow<OffsetTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}

	@Entity(name = "OffsetTimeUnknownPreservation")
	@Table(name = "tp_otime_21")
	public static class Unknown extends PreservationRow<OffsetTime> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;
		@Override OffsetTime value() { return value; }
		@Override void value(OffsetTime value) { this.value = value; }
	}
}
