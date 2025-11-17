/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.boot.ServiceRegistryTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class IdentifierHelperTest {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testAutoQuotingDisabled(boolean useQuoting) {
		try (var registry = createServiceRegistry( useQuoting )) {
			var jdbcEnvironment = registry.requireService( JdbcEnvironment.class );
			var identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( "select" );
			assertThat( identifier.isQuoted() ).isEqualTo(  useQuoting );
		}
	}

	private static ServiceRegistry createServiceRegistry(boolean useQuoting) {
		return ServiceRegistryTestingImpl.forUnitTesting(
				Collections.singletonMap( KEYWORD_AUTO_QUOTING_ENABLED, useQuoting )
		);
	}
}
