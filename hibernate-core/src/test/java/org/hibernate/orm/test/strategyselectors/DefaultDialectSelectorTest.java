/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.strategyselectors;

import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.dialect.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultDialectSelectorTest {

	private final DefaultDialectSelector strategySelector = new DefaultDialectSelector();

	@Test
	public void verifyAllDialectNamingResolve() {
		testDialectNamingResolution( DB2Dialect.class );

		testDialectNamingResolution( H2Dialect.class );
		testDialectNamingResolution( HANADialect.class );
		testDialectNamingResolution( HSQLDialect.class );

		testDialectNamingResolution( MySQLDialect.class );

		testDialectNamingResolution( OracleDialect.class );

		testDialectNamingResolution( PostgreSQLDialect.class );
		testDialectNamingResolution( PostgresPlusDialect.class );

		testDialectNamingResolution( SQLServerDialect.class );

		testDialectNamingResolution( SybaseDialect.class );
	}

	private void testDialectNamingResolution(final Class<?> dialectClass) {
		String simpleName = dialectClass.getSimpleName();
		if ( simpleName.endsWith( "Dialect" ) ) {
			simpleName = simpleName.substring( 0, simpleName.length() - "Dialect".length() );
		}
		Class<? extends Dialect> aClass = strategySelector.resolve( simpleName );
		assertNotNull( aClass );
		assertEquals( dialectClass, aClass );
	}

}
