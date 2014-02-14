/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.global;

import javax.persistence.NamedNativeQuery;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class QueryBinderTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testNoResultClass() {
		@NamedNativeQuery(name = "fubar", query = "SELECT * FROM FOO")
		class Foo {
		}
		new MetadataSources( serviceRegistry ).addAnnotatedClass( Foo.class ).buildMetadata();
	}

	@Test
	public void testResultClass() {
		@NamedNativeQuery(name = "fubar", query = "SELECT * FROM FOO", resultClass = Foo.class)
		class Foo {
		}
		Metadata meta = new MetadataSources( serviceRegistry ).addAnnotatedClass( Foo.class ).buildMetadata();
		NamedSQLQueryDefinition namedQuery = meta.getNamedNativeQuery( "fubar" );
		assertNotNull( namedQuery );
		NativeSQLQueryReturn queryReturns[] = namedQuery.getQueryReturns();
		assertTrue( "Wrong number of returns", queryReturns.length == 1 );
		assertTrue( "Wrong query return type", queryReturns[0] instanceof NativeSQLQueryRootReturn );
		NativeSQLQueryRootReturn rootReturn = (NativeSQLQueryRootReturn) queryReturns[0];
		assertEquals( "Wrong result class", Foo.class.getName(), rootReturn.getReturnEntityName() );
	}
}


