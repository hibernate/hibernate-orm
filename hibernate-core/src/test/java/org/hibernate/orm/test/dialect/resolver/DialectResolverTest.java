/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.TestingDialects;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.engine.jdbc.dialect.spi.BasicDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

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
		resolvers.addResolver( new BasicDialectResolver( "ErrorDatabase1", Dialect.class ) );

		testDetermination( resolvers, "MyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyDatabase1", 2, TestingDialects.MyDialect1.class );

		testDetermination( resolvers, "MyDatabase2", 0, null );
		testDetermination( resolvers, "MyDatabase2", 1, TestingDialects.MyDialect21.class );
		testDetermination( resolvers, "MyDatabase2", 2, TestingDialects.MyDialect22.class );

		testDetermination( resolvers, "ErrorDatabase1", 0, null );
	}

	@Test
	@JiraKey(value = "HHH-13225")
	public void testMinorVersion() {
		DialectResolverSet resolvers = new DialectResolverSet();
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase1", TestingDialects.MyDialect1.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase2", 1, TestingDialects.MyDialect21.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase2", 2, TestingDialects.MyDialect22.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase3", 1, 1, TestingDialects.MyDialect311.class ) );
		resolvers.addResolver( new BasicDialectResolver( "MyDatabase3", 1, 2, TestingDialects.MyDialect312.class ) );
		resolvers.addResolver( new BasicDialectResolver( "ErrorDatabase1", Dialect.class ) );

		testDetermination( resolvers, "MyDatabase1", 1, 1, TestingDialects.MyDialect1.class );

		testDetermination( resolvers, "MyDatabase3", 1, null );
		testDetermination( resolvers, "MyDatabase3", 1, 1, TestingDialects.MyDialect311.class );
		testDetermination( resolvers, "MyDatabase3", 1, 2, TestingDialects.MyDialect312.class );
		testDetermination( resolvers, "MyDatabase3", 1, 3, null );
	}

	private void testDetermination(
			DialectResolver resolver,
			String databaseName,
			int majorVersion,
			Class<? extends Dialect> dialectClass) {
		testDetermination( resolver, databaseName, majorVersion, DialectResolutionInfo.NO_VERSION, dialectClass );
	}

	private void testDetermination(
			DialectResolver resolver,
			String databaseName,
			int majorVersion,
			int minorVersion,
			Class<? extends Dialect> dialectClass) {
		Dialect dialect = resolver.resolveDialect(
				TestingDialectResolutionInfo.forDatabaseInfo( databaseName, majorVersion, minorVersion )
		);
		if ( dialectClass == null ) {
			assertNull( dialect );
		}
		else {
			assertEquals( dialectClass, dialect.getClass() );
		}
	}

}
