/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.strategyselectors;

import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.dialect.*;

import org.junit.Assert;
import org.junit.Test;

public class DefaultDialectSelectorTest {

	private DefaultDialectSelector strategySelector = new DefaultDialectSelector();

	@Test
	public void verifyAllDialectNamingResolve() {
		testDialectNamingResolution( Cache71Dialect.class );
		testDialectNamingResolution( CUBRIDDialect.class );
		testDialectNamingResolution( DB2Dialect.class );
		testDialectNamingResolution( DB2390Dialect.class );
		testDialectNamingResolution( DB2390V8Dialect.class );
		testDialectNamingResolution( DB2400Dialect.class );
		testDialectNamingResolution( DB2400V7R3Dialect.class );
		testDialectNamingResolution( DerbyTenFiveDialect.class );
		testDialectNamingResolution( DerbyTenSixDialect.class );
		testDialectNamingResolution( DerbyTenSevenDialect.class );
		testDialectNamingResolution( FirebirdDialect.class );
		testDialectNamingResolution( FrontBaseDialect.class );
		testDialectNamingResolution( H2Dialect.class );
		testDialectNamingResolution( HANAColumnStoreDialect.class );
		testDialectNamingResolution( HANARowStoreDialect.class );
		testDialectNamingResolution( HSQLDialect.class );
		testDialectNamingResolution( InformixDialect.class );
		testDialectNamingResolution( IngresDialect.class );
		testDialectNamingResolution( Ingres9Dialect.class );
		testDialectNamingResolution( Ingres10Dialect.class );
		testDialectNamingResolution( InterbaseDialect.class );
		testDialectNamingResolution( JDataStoreDialect.class );

		testDialectNamingResolution( MckoiDialect.class );
		testDialectNamingResolution( MimerSQLDialect.class );
		testDialectNamingResolution( MySQL5Dialect.class );
		testDialectNamingResolution( MySQL5InnoDBDialect.class );
		testDialectNamingResolution( MySQL57InnoDBDialect.class );
		testDialectNamingResolution( MySQL57Dialect.class );
		testDialectNamingResolution( MySQL8Dialect.class );
		testDialectNamingResolution( Oracle8iDialect.class );
		testDialectNamingResolution( Oracle9iDialect.class );
		testDialectNamingResolution( Oracle10gDialect.class );

		testDialectNamingResolution( PointbaseDialect.class );
		testDialectNamingResolution( PostgresPlusDialect.class );
		testDialectNamingResolution( PostgreSQL81Dialect.class );
		testDialectNamingResolution( PostgreSQL82Dialect.class );
		testDialectNamingResolution( PostgreSQL9Dialect.class );
		testDialectNamingResolution( ProgressDialect.class );

		testDialectNamingResolution( SAPDBDialect.class );
		testDialectNamingResolution( SQLServerDialect.class );
		testDialectNamingResolution( SQLServer2005Dialect.class );
		testDialectNamingResolution( SQLServer2008Dialect.class );
		testDialectNamingResolution( Sybase11Dialect.class );
		testDialectNamingResolution( SybaseAnywhereDialect.class );
		testDialectNamingResolution( SybaseASE15Dialect.class );
		testDialectNamingResolution( SybaseASE157Dialect.class );
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
