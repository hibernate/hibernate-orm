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
		testDialectNamingResolution( DB297Dialect.class );
		testDialectNamingResolution( DB2390Dialect.class );
		testDialectNamingResolution( DB2390V8Dialect.class );

		testDialectNamingResolution( Cache71Dialect.class );
		testDialectNamingResolution( CUBRIDDialect.class );
		testDialectNamingResolution( AltibaseDialect.class );

		testDialectNamingResolution( DerbyTenFiveDialect.class );
		testDialectNamingResolution( DerbyTenSixDialect.class );
		testDialectNamingResolution( DerbyTenSevenDialect.class );

		testDialectNamingResolution( FirebirdDialect.class );
		testDialectNamingResolution( InformixDialect.class );
		testDialectNamingResolution( IngresDialect.class );
		testDialectNamingResolution( Ingres9Dialect.class );
		testDialectNamingResolution( Ingres10Dialect.class );
		testDialectNamingResolution( MimerSQLDialect.class );

		testDialectNamingResolution( MariaDB53Dialect.class );
		testDialectNamingResolution( MariaDB10Dialect.class );
		testDialectNamingResolution( MariaDB102Dialect.class );
		testDialectNamingResolution( MariaDB103Dialect.class );

		testDialectNamingResolution( MySQL5Dialect.class );
		testDialectNamingResolution( MySQL55Dialect.class );
		testDialectNamingResolution( MySQL57Dialect.class );

		testDialectNamingResolution( Oracle8iDialect.class );
		testDialectNamingResolution( Oracle9iDialect.class );
		testDialectNamingResolution( Oracle10gDialect.class );
		testDialectNamingResolution( Oracle12cDialect.class );

		testDialectNamingResolution( PostgreSQL81Dialect.class );
		testDialectNamingResolution( PostgreSQL82Dialect.class );
		testDialectNamingResolution( PostgreSQL9Dialect.class );
		testDialectNamingResolution( PostgreSQL91Dialect.class );
		testDialectNamingResolution( PostgreSQL92Dialect.class );
		testDialectNamingResolution( PostgreSQL93Dialect.class );
		testDialectNamingResolution( PostgreSQL94Dialect.class );
		testDialectNamingResolution( PostgreSQL95Dialect.class );
		testDialectNamingResolution( PostgreSQL10Dialect.class );

		testDialectNamingResolution( SAPDBDialect.class );

		testDialectNamingResolution( SQLServer2005Dialect.class );
		testDialectNamingResolution( SQLServer2008Dialect.class );

		testDialectNamingResolution( SybaseAnywhereDialect.class );
		testDialectNamingResolution( Sybase11Dialect.class );
		testDialectNamingResolution( SybaseASE15Dialect.class );
		testDialectNamingResolution( SybaseASE157Dialect.class );

		testDialectNamingResolution( TeradataDialect.class );
		testDialectNamingResolution( TimesTenDialect.class );
		testDialectNamingResolution( SingleStoreDialect.class );
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
