/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.nameddescriptor;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies package-level named queries and statements registered by persistence XML.
///
/// @author Steve Ebersole
public class PackageDescriptorXmlNamedQueriesTest {
	@Test
	void packageDescriptorNamedQueryAndStatementExecution() {
		final var xml = getClass().getClassLoader()
				.getResource( "org/hibernate/orm/test/jpa/nameddescriptor/persistence.xml" );
		assertThat( xml ).isNotNull();
		final Map<String, Object> settings = new HashMap<>();
		Environment.getProperties().forEach( (key, value) -> settings.put( key.toString(), value ) );
		ServiceRegistryUtil.applySettings( settings );
		try ( var factory = Bootstrap.getEntityManagerFactoryBuilder( xml, "package-descriptor-queries", settings ).build() ) {
			PackageDescriptorNamedQueriesTest.verifyNamedQueriesAndStatement( factory );
		}
	}
}
