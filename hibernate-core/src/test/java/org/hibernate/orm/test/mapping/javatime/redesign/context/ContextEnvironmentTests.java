package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.Properties;
import java.util.Map;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Resolved ownership is stable across processes and configuration aliases.
/// Live parallel handoff tests additionally exercise the Gradle worker allocator.
///
/// @author Steve Ebersole
class ContextEnvironmentTests {
	@ParameterizedTest
	@ValueSource(strings = {"hibernate.connection.", "jakarta.persistence.jdbc."})
	void retainsConcreteNonDefaultWorkerAssignment(String prefix) {
		var input = new Properties();
		input.setProperty( prefix + "url", "jdbc:postgresql://localhost/test_2" );
		String user = prefix + (prefix.startsWith( "hibernate" ) ? "username" : "user");
		input.setProperty( user, "worker_2" );
		input.setProperty( "org.gradle.test.worker", "999" );
		var output = ContextEnvironment.handoffSettings( input, "18.6" );
		assertNotSame( input, output );
		assertEquals( input.getProperty( prefix + "url" ), output.getProperty( prefix + "url" ) );
		assertEquals( "worker_2", output.getProperty( user ) );
		assertFalse( output.containsKey( "org.gradle.test.worker" ) );
		assertFalse( input.containsKey( "jakarta.persistence.database-product-version" ) );
		assertEquals( "18.6", output.getProperty( "jakarta.persistence.database-product-version" ) );
	}

	@Test
	void refusesAnInventedLiveDatabaseVersion() {
		assertEquals( "19.3", ContextEnvironment.checkedVersion( null, 19, 3 ) );
		assertEquals( "19.3.0", ContextEnvironment.checkedVersion( "19.3.0", 19, 3 ) );
		assertThrows( IllegalStateException.class,
				() -> ContextEnvironment.checkedVersion( "23.26.1", 19, 3 ) );
		assertEquals( "19.3", ContextEnvironment.bootstrapVersion( false,
				Map.of( "jakarta.persistence.database-product-version", "19.3" ) ) );
	}

	@Test
	void recognizesDialectFamilyRatherThanClassNameSpelling() {
		assertTrue( ContextEnvironment.is( PostgresPlusDialect.class.getName(), PostgreSQLDialect.class ) );
		assertFalse( ContextEnvironment.is( PostgresPlusDialect.class.getName(), H2Dialect.class ) );
	}
}
