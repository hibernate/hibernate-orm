/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.engine.jdbc.env;

import org.hibernate.engine.jdbc.env.spi.StandardSchemaCatalogSupportImpl;
import org.hibernate.metamodel.spi.relational.ObjectName;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class StandardSchemaCatalogSupportImplTest extends BaseUnitTestCase {
	private final StandardSchemaCatalogSupportImpl basic = new StandardSchemaCatalogSupportImpl();
	private final StandardSchemaCatalogSupportImpl oracle = new StandardSchemaCatalogSupportImpl( "@", true );

	@Test
	public void testFormatName() throws Exception {
		// todo : add tests for partial schema/catalog naming
		ObjectName on = new ObjectName( "catalog", "schema", "name" );
		String formatted = basic.formatName( on );
		assertEquals( "catalog.schema.name", formatted );

		on = new ObjectName( null, null, "name" );
		formatted = basic.formatName( on );
		assertEquals( "name", formatted );

		on = new ObjectName( "catalog", "schema", "name" );
		formatted = oracle.formatName( on );
		assertEquals( "schema.name@catalog", formatted );

		on = new ObjectName( null, null, "name" );
		formatted = oracle.formatName( on );
		assertEquals( "name", formatted );
	}

	@Test
	public void testParseName() throws Exception {
		ObjectName on = basic.parseName( "catalog.schema.name" );
		assertEquals( "schema", on.getSchema().toString() );
		assertEquals( "catalog", on.getCatalog().toString() );
		assertEquals( "name", on.getName().toString() );

		on = basic.parseName( "name" );
		assertNull( on.getSchema() );
		assertNull( on.getCatalog() );
		assertEquals( "name", on.getName().toString() );

		on = oracle.parseName( "schema.name@catalog" );
		assertEquals( "schema", on.getSchema().toString() );
		assertEquals( "catalog", on.getCatalog().toString() );
		assertEquals( "name", on.getName().toString() );

		on = oracle.parseName( "name" );
		assertNull( on.getSchema() );
		assertNull( on.getCatalog() );
		assertEquals( "name", on.getName().toString() );
	}
}
