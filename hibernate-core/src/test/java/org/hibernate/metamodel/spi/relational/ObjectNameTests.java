/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

import org.junit.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class ObjectNameTests extends BaseUnitTestCase {
	@Test
	public void testMissingName() {
		try {
			new ObjectName( null, (String)null, null );
			fail();
		}
		catch ( IllegalIdentifierException ignore ) {
		}

		try {
			new ObjectName( "catalog", "schema", null );
			fail();
		}
		catch ( IllegalIdentifierException ignore ) {
		}
	}

	@Test
	public void testIdentifierBuilding() {
		Dialect dialect = new H2Dialect();
		ObjectName on = new ObjectName( "catalog", "schema", "name" );
		assertEquals( "catalog.schema.name", on.toText() );
		on = new ObjectName( null, "schema", "name" );
		assertEquals( "schema.name", on.toText() );
		assertEquals( "schema.name", on.toText( dialect ) );
		on = new ObjectName( "`catalog`", "`schema`", "`name`" );
		assertEquals( "`catalog`.`schema`.`name`", on.toText() );
		assertEquals( "\"catalog\".\"schema\".\"name\"", on.toText( dialect ) );
		on = new ObjectName( null, "`schema`", "`name`" );
		assertEquals( "`schema`.`name`", on.toText() );
		assertEquals( "\"schema\".\"name\"", on.toText( dialect ) );
	}
}

