/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.resolver;

import java.sql.SQLException;

import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.TestCase;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.TestingDialects;
import org.hibernate.dialect.Mocks;
import org.hibernate.exception.JDBCConnectionException;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DialectResolverTest extends TestCase {

	public DialectResolverTest(String name) {
		super( name );
	}

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

	public void testErrorAndOrder() throws Exception {
		DialectResolverSet resolvers = new DialectResolverSet();
		resolvers.addResolverAtFirst( new TestingDialects.MyDialectResolver1() );
		resolvers.addResolver( new TestingDialects.ErrorDialectResolver1() );
		resolvers.addResolverAtFirst( new TestingDialects.ErrorDialectResolver1() );
		resolvers.addResolver( new TestingDialects.MyDialectResolver2() );

		// Non-connection errors are suppressed.
		testDetermination( resolvers, "MyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "MyTrickyDatabase1", 1, TestingDialects.MyDialect1.class );
		testDetermination( resolvers, "NoSuchDatabase", 1, null );

		// Connection errors are reported
		try {
			testDetermination( resolvers, "ConnectionErrorDatabase1", 1, null );
			fail();
		}
		catch ( JDBCConnectionException e ) {
			// expected
		}
	}

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
		Dialect dialect = resolver.resolveDialect( Mocks.createConnection( databaseName, version ).getMetaData() );
		if ( dialectClass == null ) {
			assertEquals( null, dialect );
		}
		else {
			assertEquals( dialectClass, dialect.getClass() );
		}
	}

	public static Test suite() {
		return new TestSuite( DialectResolverTest.class );
	}
}
