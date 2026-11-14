package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for ZonedDateTime; legacy explicit-conversion tests stay separate.
///
/// @author Steve Ebersole
public class ZonedDateTimePreservationTests extends AbstractPreservationTests<ZonedDateTime> {
	public ZonedDateTimePreservationTests() { super( ZonedDateTime.class ); }
	@Override List<Mapping<ZonedDateTime>> mappings() {
		return List.of(
				new Mapping<>( "DefaultOriginal", DefaultOriginal.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "DefaultNone", DefaultNone.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "DefaultUtc", DefaultUtc.class, Representation.DEFAULT, "UTC", 6, false, false),
				new Mapping<>( "DefaultFixed", DefaultFixed.class, Representation.DEFAULT, "+05:30", 6, false, false),
				new Mapping<>( "DefaultRegion", DefaultRegion.class, Representation.DEFAULT, "America/New_York", 6, false, false),
				new Mapping<>( "InlineOriginal", InlineOriginal.class, Representation.INLINE, null, 6, false, false),
				new Mapping<>( "InlineNone", InlineNone.class, Representation.INLINE, "<none>", 6, false, false),
				new Mapping<>( "InlineUtc", InlineUtc.class, Representation.INLINE, "UTC", 6, false, false),
				new Mapping<>( "InlineFixed", InlineFixed.class, Representation.INLINE, "+05:30", 6, false, false),
				new Mapping<>( "InlineRegion", InlineRegion.class, Representation.INLINE, "America/New_York", 6, false, false),
				new Mapping<>( "ColumnOriginal", ColumnOriginal.class, Representation.COLUMN, null, 6, false, false),
				new Mapping<>( "ColumnNone", ColumnNone.class, Representation.COLUMN, "<none>", 6, false, false),
				new Mapping<>( "ColumnUtc", ColumnUtc.class, Representation.COLUMN, "UTC", 6, false, false),
				new Mapping<>( "ColumnFixed", ColumnFixed.class, Representation.COLUMN, "+05:30", 6, false, false),
				new Mapping<>( "ColumnRegion", ColumnRegion.class, Representation.COLUMN, "America/New_York", 6, false, false),
				new Mapping<>( "InlineOriginalSeconds", InlineOriginalSeconds.class, Representation.INLINE, null, 0, false, false),
				new Mapping<>( "InlineUtcSeconds", InlineUtcSeconds.class, Representation.INLINE, "UTC", 0, false, false),
				new Mapping<>( "InlineRegionSeconds", InlineRegionSeconds.class, Representation.INLINE, "America/New_York", 0, false, false),
				new Mapping<>( "ColumnOriginalSeconds", ColumnOriginalSeconds.class, Representation.COLUMN, null, 0, false, false),
				new Mapping<>( "ColumnUtcSeconds", ColumnUtcSeconds.class, Representation.COLUMN, "UTC", 0, false, false),
				new Mapping<>( "ColumnRegionSeconds", ColumnRegionSeconds.class, Representation.COLUMN, "America/New_York", 0, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "ZonedDateTimeDefaultOriginalPreservation")
	@Table(name = "tp_zdt_0")
	public static class DefaultOriginal extends PreservationRow<ZonedDateTime> {
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeDefaultNonePreservation")
	@Table(name = "tp_zdt_1")
	public static class DefaultNone extends PreservationRow<ZonedDateTime> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeDefaultUtcPreservation")
	@Table(name = "tp_zdt_2")
	public static class DefaultUtc extends PreservationRow<ZonedDateTime> {
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeDefaultFixedPreservation")
	@Table(name = "tp_zdt_3")
	public static class DefaultFixed extends PreservationRow<ZonedDateTime> {
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeDefaultRegionPreservation")
	@Table(name = "tp_zdt_4")
	public static class DefaultRegion extends PreservationRow<ZonedDateTime> {
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineOriginalPreservation")
	@Table(name = "tp_zdt_5")
	public static class InlineOriginal extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineNonePreservation")
	@Table(name = "tp_zdt_6")
	public static class InlineNone extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineUtcPreservation")
	@Table(name = "tp_zdt_7")
	public static class InlineUtc extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineFixedPreservation")
	@Table(name = "tp_zdt_8")
	public static class InlineFixed extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineRegionPreservation")
	@Table(name = "tp_zdt_9")
	public static class InlineRegion extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnOriginalPreservation")
	@Table(name = "tp_zdt_10")
	public static class ColumnOriginal extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnNonePreservation")
	@Table(name = "tp_zdt_11")
	public static class ColumnNone extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnUtcPreservation")
	@Table(name = "tp_zdt_12")
	public static class ColumnUtc extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnFixedPreservation")
	@Table(name = "tp_zdt_13")
	public static class ColumnFixed extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnRegionPreservation")
	@Table(name = "tp_zdt_14")
	public static class ColumnRegion extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineOriginalSecondsPreservation")
	@Table(name = "tp_zdt_15")
	public static class InlineOriginalSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineUtcSecondsPreservation")
	@Table(name = "tp_zdt_16")
	public static class InlineUtcSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeInlineRegionSecondsPreservation")
	@Table(name = "tp_zdt_17")
	public static class InlineRegionSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnOriginalSecondsPreservation")
	@Table(name = "tp_zdt_18")
	public static class ColumnOriginalSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnUtcSecondsPreservation")
	@Table(name = "tp_zdt_19")
	public static class ColumnUtcSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeColumnRegionSecondsPreservation")
	@Table(name = "tp_zdt_20")
	public static class ColumnRegionSeconds extends PreservationRow<ZonedDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}

	@Entity(name = "ZonedDateTimeUnknownPreservation")
	@Table(name = "tp_zdt_21")
	public static class Unknown extends PreservationRow<ZonedDateTime> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;
		@Override ZonedDateTime value() { return value; }
		@Override void value(ZonedDateTime value) { this.value = value; }
	}
}
