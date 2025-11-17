/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class CommunityDialectSelectorTest {

	private final CommunityDialectSelector strategySelector = new CommunityDialectSelector();

	@Test
	public void verifyAllDialectNamingResolve() {
		testDialectNamingResolution( CUBRIDDialect.class );
		testDialectNamingResolution( AltibaseDialect.class );

		testDialectNamingResolution( FirebirdDialect.class );
		testDialectNamingResolution( InformixDialect.class );
		testDialectNamingResolution( IngresDialect.class );
		testDialectNamingResolution( MimerSQLDialect.class );

		testDialectNamingResolution( SybaseAnywhereDialect.class );
		testDialectNamingResolution( TeradataDialect.class );
		testDialectNamingResolution( TimesTenDialect.class );
		testDialectNamingResolution( SingleStoreDialect.class );
		testDialectNamingResolution( DerbyDialect.class );
	}

	private void testDialectNamingResolution(final Class<?> dialectClass) {
		String simpleName = dialectClass.getSimpleName();
		if ( simpleName.endsWith( "Dialect" ) ) {
			simpleName = simpleName.substring( 0, simpleName.length() - "Dialect".length() );
		}
		Class<? extends Dialect> aClass = strategySelector.resolve( simpleName );
		assertThat( aClass ).isNotNull();
		assertThat( aClass ).isEqualTo( dialectClass );
	}

}
