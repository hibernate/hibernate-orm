/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
