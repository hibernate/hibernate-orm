/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.index.jpa;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.*;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class };
	}

	@Test
	public void testBasicIndex() {
		PersistentClass carClass = configuration().getClassMapping( Car.class.getName() );
		Iterator itr = carClass.getTable().getUniqueKeyIterator();
		assertTrue( itr.hasNext() );
		UniqueKey uk = (UniqueKey) itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( StringHelper.isNotEmpty( uk.getName() ) );
		assertEquals( 2, uk.getColumnSpan() );
		Column column = (Column) uk.getColumns().get( 0 );
		assertEquals( "brand", column.getName() );
		column = (Column) uk.getColumns().get( 1 );
		assertEquals( "producer", column.getName() );
		assertSame( carClass.getTable(), uk.getTable() );


		itr = carClass.getTable().getIndexIterator();
		assertTrue( itr.hasNext() );
		Index index = (Index)itr.next();
		assertFalse( itr.hasNext() );
		assertEquals( "Car_idx", index.getName() );
		assertEquals( 1, index.getColumnSpan() );
		column = index.getColumnIterator().next();
		assertEquals( "since", column.getName() );
		assertSame( carClass.getTable(), index.getTable() );
	}
}
