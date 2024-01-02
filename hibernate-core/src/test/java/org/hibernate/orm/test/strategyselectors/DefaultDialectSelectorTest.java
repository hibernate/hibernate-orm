/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.strategyselectors;

import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.community.dialect.DerbyTenFiveDialect;
import org.hibernate.community.dialect.DerbyTenSevenDialect;
import org.hibernate.community.dialect.DerbyTenSixDialect;
import org.hibernate.community.dialect.MySQL57Dialect;
import org.hibernate.community.dialect.MySQL5Dialect;
import org.hibernate.community.dialect.Oracle12cDialect;
import org.hibernate.community.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultDialectSelectorTest {

	private final DefaultDialectSelector strategySelector = new DefaultDialectSelector();

	@Test
	public void verifyAllDialectNamingResolve() {
		testDialectNamingResolution( DB2Dialect.class );
		testDialectNamingResolution( DB2400Dialect.class );
		testDialectNamingResolution( DB2400V7R3Dialect.class );

		testDialectNamingResolution( DerbyDialect.class );
		testDialectNamingResolution( DerbyTenFiveDialect.class );
		testDialectNamingResolution( DerbyTenSixDialect.class );
		testDialectNamingResolution( DerbyTenSevenDialect.class );

		testDialectNamingResolution( H2Dialect.class );
		testDialectNamingResolution( HANADialect.class );
		testDialectNamingResolution( HANAColumnStoreDialect.class );
		testDialectNamingResolution( HANARowStoreDialect.class );
		testDialectNamingResolution( HSQLDialect.class );

		testDialectNamingResolution( MySQLDialect.class );
		testDialectNamingResolution( MySQL5Dialect.class );
		testDialectNamingResolution( MySQL57Dialect.class );
		testDialectNamingResolution( MySQL8Dialect.class );

		testDialectNamingResolution( OracleDialect.class );
		testDialectNamingResolution( Oracle12cDialect.class );

		testDialectNamingResolution( PostgreSQLDialect.class );
		testDialectNamingResolution( PostgresPlusDialect.class );

		testDialectNamingResolution( SQLServerDialect.class );
		testDialectNamingResolution( SQLServer2008Dialect.class );
		testDialectNamingResolution( SQLServer2012Dialect.class );

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
