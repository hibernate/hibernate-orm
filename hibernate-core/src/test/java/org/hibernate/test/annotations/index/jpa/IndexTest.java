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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;


/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexTest extends AbstractJPAIndexTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Dealer.class,
				Importer.class
		};
	}

	@Test
	public void testTableGeneratorIndex(){
		TableSpecification table = SchemaUtil.getTable( "ID_GEN", metadata() );

		Iterator<org.hibernate.metamodel.spi.relational.Index> indexes = table.getIndexes().iterator();
		assertTrue( indexes.hasNext() );
		org.hibernate.metamodel.spi.relational.Index index = indexes.next();
		assertFalse( indexes.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName().toString() ) );
		assertEquals( 1, index.getColumnSpan() );
		org.hibernate.metamodel.spi.relational.Column column = index.getColumns().get( 0 );
		assertEquals( "GEN_VALUE", column.getColumnName().getText() );
		assertSame( table, index.getTable() );
	}
}
