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

package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * test for {@link javax.persistence.UniqueConstraint}
 *
 * @author Strong Liu
 */
public class UniqueConstraintBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = TableWithUniqueConstraint.class)
	public void testTableUniqueConstraints() {
		EntityBinding binding = getEntityBinding( TableWithUniqueConstraint.class );
		TableSpecification table = binding.getPrimaryTable();
		Iterable<UniqueKey> uniqueKeyIterable = table.getUniqueKeys();
		assertNotNull( uniqueKeyIterable );
		int i = 0;
		for ( UniqueKey key : uniqueKeyIterable ) {
			i++;
			assertEquals( "u1", key.getName() );
			assertTrue( table == key.getTable() );
			assertNotNull( key.getColumns() );
			int j = 0;
			for ( Column column : key.getColumns() ) {
				j++;
			}
			assertEquals( "There should be two columns in the unique constraint", 2, j );
		}
		assertEquals( "There should only be one unique constraint", 1, i );
	}

	@Entity
	@Table(uniqueConstraints = { @UniqueConstraint(name = "u1", columnNames = { "name", "age" }) })
	class TableWithUniqueConstraint {
		@Id
		int id;
		String name;
		int age;
	}
}
