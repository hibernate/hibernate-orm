package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for OffsetDateTime.
///
/// @author Steve Ebersole
public class OffsetDateTimePreservationTests extends AbstractPreservationTests<OffsetDateTime> {
	public OffsetDateTimePreservationTests() { super( OffsetDateTime.class ); }
	@Override List<Mapping<OffsetDateTime>> mappings() {
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

	@Entity(name = "OffsetDateTimeDefaultOriginalPreservation")
	@Table(name = "tp_odt_0")
	public static class DefaultOriginal extends PreservationRow<OffsetDateTime> {
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeDefaultNonePreservation")
	@Table(name = "tp_odt_1")
	public static class DefaultNone extends PreservationRow<OffsetDateTime> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeDefaultUtcPreservation")
	@Table(name = "tp_odt_2")
	public static class DefaultUtc extends PreservationRow<OffsetDateTime> {
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeDefaultFixedPreservation")
	@Table(name = "tp_odt_3")
	public static class DefaultFixed extends PreservationRow<OffsetDateTime> {
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeDefaultRegionPreservation")
	@Table(name = "tp_odt_4")
	public static class DefaultRegion extends PreservationRow<OffsetDateTime> {
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineOriginalPreservation")
	@Table(name = "tp_odt_5")
	public static class InlineOriginal extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineNonePreservation")
	@Table(name = "tp_odt_6")
	public static class InlineNone extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineUtcPreservation")
	@Table(name = "tp_odt_7")
	public static class InlineUtc extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineFixedPreservation")
	@Table(name = "tp_odt_8")
	public static class InlineFixed extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineRegionPreservation")
	@Table(name = "tp_odt_9")
	public static class InlineRegion extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnOriginalPreservation")
	@Table(name = "tp_odt_10")
	public static class ColumnOriginal extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnNonePreservation")
	@Table(name = "tp_odt_11")
	public static class ColumnNone extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnUtcPreservation")
	@Table(name = "tp_odt_12")
	public static class ColumnUtc extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnFixedPreservation")
	@Table(name = "tp_odt_13")
	public static class ColumnFixed extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("+05:30")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnRegionPreservation")
	@Table(name = "tp_odt_14")
	public static class ColumnRegion extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineOriginalSecondsPreservation")
	@Table(name = "tp_odt_15")
	public static class InlineOriginalSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineUtcSecondsPreservation")
	@Table(name = "tp_odt_16")
	public static class InlineUtcSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeInlineRegionSecondsPreservation")
	@Table(name = "tp_odt_17")
	public static class InlineRegionSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnOriginalSecondsPreservation")
	@Table(name = "tp_odt_18")
	public static class ColumnOriginalSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnUtcSecondsPreservation")
	@Table(name = "tp_odt_19")
	public static class ColumnUtcSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeColumnRegionSecondsPreservation")
	@Table(name = "tp_odt_20")
	public static class ColumnRegionSeconds extends PreservationRow<OffsetDateTime> {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds")
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 0)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}

	@Entity(name = "OffsetDateTimeUnknownPreservation")
	@Table(name = "tp_odt_21")
	public static class Unknown extends PreservationRow<OffsetDateTime> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;
		@Override OffsetDateTime value() { return value; }
		@Override void value(OffsetDateTime value) { this.value = value; }
	}
}
