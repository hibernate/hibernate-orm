package org.hibernate.orm.test.type.temporal.preservation;

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
	}

	@Test
	void usesActualVersionAndRejectsConflictingOverride() {
		assertEquals( "21.3", PreservationEnvironment.checkedVersion( null, 21, 3 ) );
		assertEquals( "21.3.0", PreservationEnvironment.checkedVersion( "21.3.0", 21, 3 ) );
		assertThrows( IllegalStateException.class,
		() -> PreservationEnvironment.checkedVersion( "23.26.1", 21, 3 ) );
	}
}
