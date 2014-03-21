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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

/**
 * test for {@link javax.persistence.UniqueConstraint}
 * 
 * @author Strong Liu
 * @author Brett Meyer
 */
public class UniqueConstraintBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = { TableWithUniqueConstraint.class, SecondTable.class })
	public void testTableUniqueConstraints() {
		EntityBinding binding = getEntityBinding( TableWithUniqueConstraint.class );
		testTableUniqueConstraints( binding.getPrimaryTable(), "u1", 2 );
		testTableUniqueConstraints( ( (MultipleHiLoPerTableGenerator) binding.getHierarchyDetails()
				.getEntityIdentifier().getIdentifierGenerator() ).getTable(), "u2", 1 );
		testTableUniqueConstraints( SchemaUtil.getCollection( TableWithUniqueConstraint.class, "secondTables", meta )
				.getPluralAttributeKeyBinding().getCollectionTable(), "u3", 2 );
		testTableUniqueConstraints( SchemaUtil.getCollection( TableWithUniqueConstraint.class, "elements", meta )
				.getPluralAttributeKeyBinding().getCollectionTable(), "u4", 1 );
	}

	private void testTableUniqueConstraints(TableSpecification table, String ukName, int ukNumColumns) {
		Iterable<UniqueKey> uniqueKeyIterable = table.getUniqueKeys();
		assertNotNull( uniqueKeyIterable );
		int i = 0;
		for ( UniqueKey key : uniqueKeyIterable ) {
			i++;
			assertEquals( ukName, key.getName().getText() );
			assertTrue( table == key.getTable() );
			assertNotNull( key.getColumns() );
			assertEquals( "There should be " + ukNumColumns + " columns in the unique constraint", ukNumColumns, key.getColumns().size() );
			assertEquals( "There should be " + ukNumColumns + " columns in the unique constraint", ukNumColumns, key.getColumnSpan() );
		}
		assertEquals( "There should only be one unique constraint", 1, i );
	}

	@Entity
	@Table(uniqueConstraints = { @UniqueConstraint(name = "u1", columnNames = { "name", "age" }) })
	class TableWithUniqueConstraint {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "fooGenerator")
		@TableGenerator(name = "fooGenerator", table = "foo_generator_table", valueColumnName = "fooGeneratorValue", uniqueConstraints = @UniqueConstraint(columnNames = "fooGeneratorValue", name = "u2"))
		int id;

		String name;

		int age;

		@ManyToMany
		@JoinTable(name = "JoinTable", joinColumns = @JoinColumn(name = "secondTable"), inverseJoinColumns = @JoinColumn(name = "tableWithUniqueConstraint"), uniqueConstraints = @UniqueConstraint(columnNames = {
				"secondTable", "tableWithUniqueConstraint" }, name = "u3"))
		Set<SecondTable> secondTables;

		@ElementCollection
		@CollectionTable(name = "CollectionTable", joinColumns = @JoinColumn(name = "element"), uniqueConstraints = @UniqueConstraint(columnNames = "element", name = "u4"))
		@OrderColumn(name = "element_index")
		public List<Integer> elements;
				// TODO: int[] is not completely supported using the new metamodel yet, so I changed int[] to List<Integer>
	}

	@Entity
	class SecondTable {
		@Id
		@GeneratedValue
		int id;

		String name;

		int age;

		@ManyToMany(mappedBy = "secondTables")
		Set<TableWithUniqueConstraint> tableWithUniqueConstraint;
	}
}
