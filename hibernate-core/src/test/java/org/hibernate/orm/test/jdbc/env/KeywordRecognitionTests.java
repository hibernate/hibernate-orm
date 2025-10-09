/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class KeywordRecognitionTests {
	@Test
	@JiraKey("HHH-9768")
	@ServiceRegistry(settings = @Setting(name = KEYWORD_AUTO_QUOTING_ENABLED, value = "true"))
	public void testAnsiSqlKeyword(ServiceRegistryScope registryScope) {
		// END is ANSI SQL keyword

		registryScope.withService( JdbcEnvironment.class, (jdbcEnvironment) -> {
			assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "end" ) );
			assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "END" ) );

			Identifier identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( "end" );
			assertTrue( identifier.isQuoted() );
		} );
	}
}
