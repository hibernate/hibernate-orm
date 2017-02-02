/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.resolver;

import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.TestingDialects;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.engine.jdbc.dialect.spi.BasicDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.exception.JDBCConnectionException;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class DialectResolverTest extends BaseUnitTestCase {
	@Test
	public void testDialects() throws Exception {
		DialectResolverSet resolvers = new DialectResolverSet();

		resolvers.addResolverAtFirst( new TestingDialects.MyDialectResolver1() );
		resolvers.addResolverAtFirst( new TestingDialects.MyDialectResolver2() );

		testDetermination( resolvers, "MyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyDatabase1", 2, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyDatabase2", 0, null );
		testDetermination( resolvers, "MyDatabase2", 1, TestingDialects.MyDialect21.class );
		testDetermination( resolvers, "MyDatabase2", 2, TestingDialects.MyDialect22.class );
		testDetermination( resolvers, "MyDatabase2", 3, TestingDialects.MyDialect22.class );
		testDetermination( resolvers, "MyDatabase3", 1, null );
		testDetermination( resolvers, "MyTrickyDatabase1", 1, TestingDialects.MyDialect1.class );
	}

	@Test
	public void testErrorAndOrder() throws Exception {
		DialectResolverSet resolvers = new DialectResolverSet();
		resolvers.addResolverAtFirst( new TestingDialects.MyDialectResolver1() );
		resolvers.addResolver( new TestingDialects.MyDialectResolver2() );

		// Non-connection errors are suppressed.
		testDetermination( resolvers, "MyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyTrickyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "NoSuchDatabase", 1, null );
	}

	@Test
	public void testBasicDialectResolver() throws Exception {
		DialectResolverSet resolvers = new DialectResolverSet();
		// Simulating MyDialectResolver1 by BasicDialectResolvers
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase1", TestingDialects.MyDialect1.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase2", 1, TestingDialects.MyDialect21.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase2", 2, TestingDialects.MyDialect22.class ) );
		resolvers.addResolver( new BasicDialectResolver( "ErrorDatabase1", Object.class ) );
		testDetermination( resolvers, "MyDatabase1", 1, TestingDialects.MyDialect1.class );

		testDetermination( resolvers, "MyDatabase1", 2, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyDatabase2", 0, null );
		testDetermination( resolvers, "MyDatabase2", 1, TestingDialects.MyDialect21.class );
		testDetermination( resolvers, "MyDatabase2", 2, TestingDialects.MyDialect22.class );
		testDetermination( resolvers, "ErrorDatabase1", 0, null );
	}


	private void testDetermination(
			DialectResolver resolver,
			String databaseName,
			int version,
			Class dialectClass) throws SQLException {
		Dialect dialect = resolver.resolveDialect( TestingDialectResolutionInfo.forDatabaseInfo( databaseName, version ) );
		if ( dialectClass == null ) {
			assertEquals( null, dialect );
		}
		else {
			assertEquals( dialectClass, dialect.getClass() );
		}
	}

}
