/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;

import org.junit.Assert;
import org.junit.Test;

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
	}

	private void testDialectNamingResolution(final Class<?> dialectClass) {
		String simpleName = dialectClass.getSimpleName();
		if ( simpleName.endsWith( "Dialect" ) ) {
			simpleName = simpleName.substring( 0, simpleName.length() - "Dialect".length() );
		}
		Class<? extends Dialect> aClass = strategySelector.resolve( simpleName );
		Assert.assertNotNull( aClass );
		Assert.assertEquals( dialectClass, aClass );
	}

}
