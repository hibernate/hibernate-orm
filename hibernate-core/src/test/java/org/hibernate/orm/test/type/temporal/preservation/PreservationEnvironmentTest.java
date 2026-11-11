package org.hibernate.orm.test.type.temporal.preservation;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Live provisioning must not pretend an older server implements newer DDL.
///
/// @author Steve Ebersole
class PreservationEnvironmentTest {
	@Test
	void edbIdentityDoesNotChangeItsSqlProfile() {
		var edb = new PreservationEnvironment( "E5", "read", "postgresql", "17.4", "org.hibernate.dialect.PostgresPlusDialect" );
		var pg = new PreservationEnvironment( "E5", "read", "postgresql", "17.4", "org.hibernate.dialect.PostgreSQLDialect" );
		assertEquals( true, edb.pg() );
		assertEquals( true, edb.edb() );
		assertEquals( true, pg.pg() );
		assertEquals( false, pg.edb() );
		var edbSamples = PreservationRangeSamples.samples( LocalDate.class, null, edb, false );
		var pgSamples = PreservationRangeSamples.samples( LocalDate.class, null, pg, false );
		assertEquals( LocalDate.of( 294276, 12, 31 ), edbSamples.stream()
				.filter( sample -> sample.id().equals( "range-max" ) ).findFirst().orElseThrow().value() );
		assertEquals( LocalDate.of( 5874897, 12, 31 ), pgSamples.stream()
				.filter( sample -> sample.id().equals( "range-max" ) ).findFirst().orElseThrow().value() );
		assertEquals( true, edbSamples.stream().anyMatch( sample -> sample.rejected()
				&& sample.value().equals( LocalDate.of( 294277, 1, 1 ) ) ) );
	}

	@Test
	void usesActualVersionAndRejectsConflictingOverride() {
		assertEquals( "21.3", PreservationEnvironment.checkedVersion( null, 21, 3 ) );
		assertEquals( "21.3.0", PreservationEnvironment.checkedVersion( "21.3.0", 21, 3 ) );
		assertThrows( IllegalStateException.class,
		() -> PreservationEnvironment.checkedVersion( "23.26.1", 21, 3 ) );
	}
}
