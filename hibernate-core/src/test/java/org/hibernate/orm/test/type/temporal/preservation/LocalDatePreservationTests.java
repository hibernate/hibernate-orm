package org.hibernate.orm.test.type.temporal.preservation;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.*;

/// Preservation cases for LocalDate.
///
/// @author Steve Ebersole
public class LocalDatePreservationTests extends AbstractPreservationTests<LocalDate> {
	public LocalDatePreservationTests() { super( LocalDate.class ); }
	@Override List<Mapping<LocalDate>> mappings() {
		return List.of(
				new Mapping<>( "Natural", Natural.class, Representation.DEFAULT, null, 6, false, false),
				new Mapping<>( "None", None.class, Representation.DEFAULT, "<none>", 6, false, false),
				new Mapping<>( "Fallback", Fallback.class, Representation.DEFAULT, null, 6, true, false),
				new Mapping<>( "InvalidAdjustment", InvalidAdjustment.class, Representation.DEFAULT, "UTC", 6, false, false),
				new Mapping<>( "Unknown", Unknown.class, Representation.DEFAULT, null, 6, true, true)
		);
	}

	@Entity(name = "LocalDateNaturalPreservation")
	@Table(name = "tp_ldate_0")
	public static class Natural extends PreservationRow<LocalDate> {
		@Column(name = "temporal_value")
		LocalDate value;
		@Override LocalDate value() { return value; }
		@Override void value(LocalDate value) { this.value = value; }
	}

	@Entity(name = "LocalDateNonePreservation")
	@Table(name = "tp_ldate_1")
	public static class None extends PreservationRow<LocalDate> {
		@TimeZoneAdjustment("<none>")
		@Column(name = "temporal_value")
		LocalDate value;
		@Override LocalDate value() { return value; }
		@Override void value(LocalDate value) { this.value = value; }
	}

	@Entity(name = "LocalDateFallbackPreservation")
	@Table(name = "tp_ldate_2")
	public static class Fallback extends PreservationRow<LocalDate> {
		@JdbcType(DateJdbcType.class)
		@Column(name = "temporal_value")
		LocalDate value;
		@Override LocalDate value() { return value; }
		@Override void value(LocalDate value) { this.value = value; }
	}

	@Entity(name = "LocalDateInvalidAdjustmentPreservation")
	@Table(name = "tp_ldate_3")
	public static class InvalidAdjustment extends PreservationRow<LocalDate> {
		@TimeZoneAdjustment("UTC")
		@Column(name = "temporal_value")
		LocalDate value;
		@Override LocalDate value() { return value; }
		@Override void value(LocalDate value) { this.value = value; }
	}

	@Entity(name = "LocalDateUnknownPreservation")
	@Table(name = "tp_ldate_4")
	public static class Unknown extends PreservationRow<LocalDate> {
		@JdbcType(UnknownTemporalJdbcType.class)
		@Column(name = "temporal_value")
		LocalDate value;
		@Override LocalDate value() { return value; }
		@Override void value(LocalDate value) { this.value = value; }
	}
}
