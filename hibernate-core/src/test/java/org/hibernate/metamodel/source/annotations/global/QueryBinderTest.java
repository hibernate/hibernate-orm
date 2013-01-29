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
package org.hibernate.metamodel.source.annotations.global;

import javax.persistence.NamedNativeQuery;

import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContextImpl;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class QueryBinderTest extends BaseUnitTestCase {

	private StandardServiceRegistryImpl serviceRegistry;
	private ClassLoaderService service;
	private MetadataImpl meta;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
		service = serviceRegistry.getService( ClassLoaderService.class );
		meta = (MetadataImpl) new MetadataSources( serviceRegistry ).buildMetadata();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test(expected = NotYetImplementedException.class)
	public void testNoResultClass() {
		@NamedNativeQuery(name = "fubar", query = "SELECT * FROM FOO")
		class Foo {
		}
		Index index = JandexHelper.indexForClass( service, Foo.class );
		QueryBinder.bind( new AnnotationBindingContextImpl( meta, index ) );
	}

	@Test
	public void testResultClass() {
		@NamedNativeQuery(name = "fubar", query = "SELECT * FROM FOO", resultClass = Foo.class)
		class Foo {
		}
		Index index = JandexHelper.indexForClass( service, Foo.class );
		QueryBinder.bind( new AnnotationBindingContextImpl( meta, index ) );

		NamedSQLQueryDefinition namedQuery = meta.getNamedNativeQuery( "fubar" );
		assertNotNull( namedQuery );
		NativeSQLQueryReturn queryReturns[] = namedQuery.getQueryReturns();
		assertTrue( "Wrong number of returns", queryReturns.length == 1 );
		assertTrue( "Wrong query return type", queryReturns[0] instanceof NativeSQLQueryRootReturn );
		NativeSQLQueryRootReturn rootReturn = (NativeSQLQueryRootReturn) queryReturns[0];
		assertEquals( "Wrong result class", Foo.class.getName(), rootReturn.getReturnEntityName() );
	}
}


