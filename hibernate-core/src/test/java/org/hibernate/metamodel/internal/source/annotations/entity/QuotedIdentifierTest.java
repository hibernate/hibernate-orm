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

package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class QuotedIdentifierTest extends BaseAnnotationBindingTestCase {
	private final String ormPath = "org/hibernate/metamodel/internal/source/annotations/xml/orm-quote-identifier.xml";

	@Test
	@Resources(annotatedClasses = { Item.class, Item2.class, Item3.class, Item4.class }, ormXmlPath = ormPath)
	public void testDelimitedIdentifiers() {
		EntityBinding item = getEntityBinding( Item.class );
		assertIdentifierEquals( "`QuotedIdentifierTest$Item`", item );

		item = getEntityBinding( Item2.class );
		assertIdentifierEquals( "`TABLE_ITEM2`", item );

		item = getEntityBinding( Item3.class );
		assertIdentifierEquals( "`TABLE_ITEM3`", item );

		// TODO: not sure about this -- revisit after metamodel merge
//		item = getEntityBinding( Item4.class );
//		assertIdentifierEquals( "`TABLE_ITEM4`", item );
	}

    //todo check if the column names are quoted

	private void assertIdentifierEquals(String expected, EntityBinding realValue) {
		org.hibernate.metamodel.spi.relational.Table table = (org.hibernate.metamodel.spi.relational.Table) realValue.getPrimaryTable();
		assertEquals( Identifier.toIdentifier( expected ), table.getPhysicalName() );
	}

	@Entity
	private static class Item {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "TABLE_ITEM2")
	private static class Item2 {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "`TABLE_ITEM3`")
	private static class Item3 {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "\"TABLE_ITEM4\"")
	private static class Item4 {
		@Id
		Long id;
	}
}
