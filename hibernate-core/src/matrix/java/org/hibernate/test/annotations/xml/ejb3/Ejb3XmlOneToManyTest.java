/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.xml.ejb3;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Ejb3XmlOneToManyTest extends Ejb3XmlTestCase {
	@Test
	public void testNoChildren() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm1.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationNotPresent( Access.class );
		OneToMany relAnno = reader.getAnnotation( OneToMany.class );
		assertEquals( 0, relAnno.cascade().length );
		assertEquals( FetchType.LAZY, relAnno.fetch() );
		assertEquals( "", relAnno.mappedBy() );
		assertFalse( relAnno.orphanRemoval() );
		assertEquals( void.class, relAnno.targetEntity() );
	}

	@Test
	public void testOrderBy() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm2.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertEquals(
				"col1 ASC, col2 DESC", reader.getAnnotation( OrderBy.class )
				.value()
		);
	}

	@Test
	public void testOrderColumnNoAttributes() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm3.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationPresent( OrderColumn.class );
		OrderColumn orderColumnAnno = reader.getAnnotation( OrderColumn.class );
		assertEquals( "", orderColumnAnno.columnDefinition() );
		assertEquals( "", orderColumnAnno.name() );
		assertTrue( orderColumnAnno.insertable() );
		assertTrue( orderColumnAnno.nullable() );
		assertTrue( orderColumnAnno.updatable() );
	}

	@Test
	public void testOrderColumnAllAttributes() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm4.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationPresent( OrderColumn.class );
		OrderColumn orderColumnAnno = reader.getAnnotation( OrderColumn.class );
		assertEquals( "int", orderColumnAnno.columnDefinition() );
		assertEquals( "col1", orderColumnAnno.name() );
		assertFalse( orderColumnAnno.insertable() );
		assertFalse( orderColumnAnno.nullable() );
		assertFalse( orderColumnAnno.updatable() );
	}

	@Test
	public void testMapKeyNoAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm5.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertEquals( "", reader.getAnnotation( MapKey.class ).name() );
	}

	@Test
	public void testMapKeyAllAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm6.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertEquals( "field2", reader.getAnnotation( MapKey.class ).name() );
	}

	@Test
	public void testMapKeyClass() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm7.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertEquals(
				Entity2.class, reader.getAnnotation( MapKeyClass.class )
				.value()
		);
	}

	@Test
	public void testMapKeyTemporal() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm8.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertEquals(
				TemporalType.DATE, reader.getAnnotation(
				MapKeyTemporal.class
		).value()
		);
	}

	@Test
	public void testMapKeyEnumerated() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm9.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertEquals(
				EnumType.STRING, reader.getAnnotation(
				MapKeyEnumerated.class
		).value()
		);
	}

	/**
	 * When there's a single map key attribute override, we still wrap it with
	 * an AttributeOverrides annotation.
	 */
	@Test
	public void testSingleMapKeyAttributeOverride() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm10.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( AttributeOverride.class );
		assertAnnotationPresent( AttributeOverrides.class );
		AttributeOverrides overridesAnno = reader
				.getAnnotation( AttributeOverrides.class );
		AttributeOverride[] overrides = overridesAnno.value();
		assertEquals( 1, overrides.length );
		assertEquals( "field1", overrides[0].name() );
		assertEquals( "col1", overrides[0].column().name() );
	}

	@Test
	public void testMultipleMapKeyAttributeOverrides() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm11.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( AttributeOverride.class );
		assertAnnotationPresent( AttributeOverrides.class );
		AttributeOverrides overridesAnno = reader
				.getAnnotation( AttributeOverrides.class );
		AttributeOverride[] overrides = overridesAnno.value();
		assertEquals( 2, overrides.length );
		assertEquals( "field1", overrides[0].name() );
		assertEquals( "", overrides[0].column().name() );
		assertFalse( overrides[0].column().unique() );
		assertTrue( overrides[0].column().nullable() );
		assertTrue( overrides[0].column().insertable() );
		assertTrue( overrides[0].column().updatable() );
		assertEquals( "", overrides[0].column().columnDefinition() );
		assertEquals( "", overrides[0].column().table() );
		assertEquals( 255, overrides[0].column().length() );
		assertEquals( 0, overrides[0].column().precision() );
		assertEquals( 0, overrides[0].column().scale() );
		assertEquals( "field2", overrides[1].name() );
		assertEquals( "col1", overrides[1].column().name() );
		assertTrue( overrides[1].column().unique() );
		assertFalse( overrides[1].column().nullable() );
		assertFalse( overrides[1].column().insertable() );
		assertFalse( overrides[1].column().updatable() );
		assertEquals( "int", overrides[1].column().columnDefinition() );
		assertEquals( "table1", overrides[1].column().table() );
		assertEquals( 50, overrides[1].column().length() );
		assertEquals( 2, overrides[1].column().precision() );
		assertEquals( 1, overrides[1].column().scale() );
	}

	@Test
	public void testMapKeyColumnNoAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm12.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		MapKeyColumn keyColAnno = reader.getAnnotation( MapKeyColumn.class );
		assertEquals( "", keyColAnno.columnDefinition() );
		assertEquals( "", keyColAnno.name() );
		assertEquals( "", keyColAnno.table() );
		assertFalse( keyColAnno.nullable() );
		assertTrue( keyColAnno.insertable() );
		assertFalse( keyColAnno.unique() );
		assertTrue( keyColAnno.updatable() );
		assertEquals( 255, keyColAnno.length() );
		assertEquals( 0, keyColAnno.precision() );
		assertEquals( 0, keyColAnno.scale() );
	}

	@Test
	public void testMapKeyColumnAllAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm13.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		MapKeyColumn keyColAnno = reader.getAnnotation( MapKeyColumn.class );
		assertEquals( "int", keyColAnno.columnDefinition() );
		assertEquals( "col1", keyColAnno.name() );
		assertEquals( "table1", keyColAnno.table() );
		assertTrue( keyColAnno.nullable() );
		assertFalse( keyColAnno.insertable() );
		assertTrue( keyColAnno.unique() );
		assertFalse( keyColAnno.updatable() );
		assertEquals( 50, keyColAnno.length() );
		assertEquals( 2, keyColAnno.precision() );
		assertEquals( 1, keyColAnno.scale() );
	}

	/**
	 * When there's a single map key join column, we still wrap it with a
	 * MapKeyJoinColumns annotation.
	 */
	@Test
	public void testSingleMapKeyJoinColumn() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm14.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		MapKeyJoinColumns joinColumnsAnno = reader
				.getAnnotation( MapKeyJoinColumns.class );
		MapKeyJoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 1, joinColumns.length );
		assertEquals( "col1", joinColumns[0].name() );
	}

	@Test
	public void testMultipleMapKeyJoinColumns() throws Exception {
		reader = getReader( Entity3.class, "field1", "one-to-many.orm15.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		MapKeyJoinColumns joinColumnsAnno = reader
				.getAnnotation( MapKeyJoinColumns.class );
		MapKeyJoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 2, joinColumns.length );
		assertEquals( "", joinColumns[0].name() );
		assertEquals( "", joinColumns[0].referencedColumnName() );
		assertFalse( joinColumns[0].unique() );
		assertFalse( joinColumns[0].nullable() );
		assertTrue( joinColumns[0].insertable() );
		assertTrue( joinColumns[0].updatable() );
		assertEquals( "", joinColumns[0].columnDefinition() );
		assertEquals( "", joinColumns[0].table() );
		assertEquals( "col1", joinColumns[1].name() );
		assertEquals( "col2", joinColumns[1].referencedColumnName() );
		assertTrue( joinColumns[1].unique() );
		assertTrue( joinColumns[1].nullable() );
		assertFalse( joinColumns[1].insertable() );
		assertFalse( joinColumns[1].updatable() );
		assertEquals( "int", joinColumns[1].columnDefinition() );
		assertEquals( "table1", joinColumns[1].table() );
	}

	@Test
	public void testJoinTableNoChildren() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm16.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationPresent( JoinTable.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		JoinTable joinTableAnno = reader.getAnnotation( JoinTable.class );
		assertEquals( "", joinTableAnno.catalog() );
		assertEquals( "", joinTableAnno.name() );
		assertEquals( "", joinTableAnno.schema() );
		assertEquals( 0, joinTableAnno.joinColumns().length );
		assertEquals( 0, joinTableAnno.inverseJoinColumns().length );
		assertEquals( 0, joinTableAnno.uniqueConstraints().length );
	}

	@Test
	public void testJoinTableAllChildren() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm17.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationPresent( JoinTable.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		JoinTable joinTableAnno = reader.getAnnotation( JoinTable.class );
		assertEquals( "cat1", joinTableAnno.catalog() );
		assertEquals( "table1", joinTableAnno.name() );
		assertEquals( "schema1", joinTableAnno.schema() );

		// JoinColumns
		JoinColumn[] joinColumns = joinTableAnno.joinColumns();
		assertEquals( 2, joinColumns.length );
		assertEquals( "", joinColumns[0].name() );
		assertEquals( "", joinColumns[0].referencedColumnName() );
		assertEquals( "", joinColumns[0].table() );
		assertEquals( "", joinColumns[0].columnDefinition() );
		assertTrue( joinColumns[0].insertable() );
		assertTrue( joinColumns[0].updatable() );
		assertTrue( joinColumns[0].nullable() );
		assertFalse( joinColumns[0].unique() );
		assertEquals( "col1", joinColumns[1].name() );
		assertEquals( "col2", joinColumns[1].referencedColumnName() );
		assertEquals( "table2", joinColumns[1].table() );
		assertEquals( "int", joinColumns[1].columnDefinition() );
		assertFalse( joinColumns[1].insertable() );
		assertFalse( joinColumns[1].updatable() );
		assertFalse( joinColumns[1].nullable() );
		assertTrue( joinColumns[1].unique() );

		// InverseJoinColumns
		JoinColumn[] inverseJoinColumns = joinTableAnno.inverseJoinColumns();
		assertEquals( 2, inverseJoinColumns.length );
		assertEquals( "", inverseJoinColumns[0].name() );
		assertEquals( "", inverseJoinColumns[0].referencedColumnName() );
		assertEquals( "", inverseJoinColumns[0].table() );
		assertEquals( "", inverseJoinColumns[0].columnDefinition() );
		assertTrue( inverseJoinColumns[0].insertable() );
		assertTrue( inverseJoinColumns[0].updatable() );
		assertTrue( inverseJoinColumns[0].nullable() );
		assertFalse( inverseJoinColumns[0].unique() );
		assertEquals( "col3", inverseJoinColumns[1].name() );
		assertEquals( "col4", inverseJoinColumns[1].referencedColumnName() );
		assertEquals( "table3", inverseJoinColumns[1].table() );
		assertEquals( "int", inverseJoinColumns[1].columnDefinition() );
		assertFalse( inverseJoinColumns[1].insertable() );
		assertFalse( inverseJoinColumns[1].updatable() );
		assertFalse( inverseJoinColumns[1].nullable() );
		assertTrue( inverseJoinColumns[1].unique() );

		// UniqueConstraints
		UniqueConstraint[] uniqueConstraints = joinTableAnno
				.uniqueConstraints();
		assertEquals( 2, uniqueConstraints.length );
		assertEquals( "", uniqueConstraints[0].name() );
		assertEquals( 1, uniqueConstraints[0].columnNames().length );
		assertEquals( "col5", uniqueConstraints[0].columnNames()[0] );
		assertEquals( "uq1", uniqueConstraints[1].name() );
		assertEquals( 2, uniqueConstraints[1].columnNames().length );
		assertEquals( "col6", uniqueConstraints[1].columnNames()[0] );
		assertEquals( "col7", uniqueConstraints[1].columnNames()[1] );
	}

	/**
	 * When there's a single join column, we still wrap it with a JoinColumns
	 * annotation.
	 */
	@Test
	public void testSingleJoinColumn() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm18.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinTable.class );
		JoinColumns joinColumnsAnno = reader.getAnnotation( JoinColumns.class );
		JoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 1, joinColumns.length );
		assertEquals( "col1", joinColumns[0].name() );
		assertEquals( "col2", joinColumns[0].referencedColumnName() );
		assertEquals( "table1", joinColumns[0].table() );
	}

	@Test
	public void testMultipleJoinColumns() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm19.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinTable.class );
		JoinColumns joinColumnsAnno = reader.getAnnotation( JoinColumns.class );
		JoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 2, joinColumns.length );
		assertEquals( "", joinColumns[0].name() );
		assertEquals( "", joinColumns[0].referencedColumnName() );
		assertEquals( "", joinColumns[0].table() );
		assertEquals( "", joinColumns[0].columnDefinition() );
		assertTrue( joinColumns[0].insertable() );
		assertTrue( joinColumns[0].updatable() );
		assertTrue( joinColumns[0].nullable() );
		assertFalse( joinColumns[0].unique() );
		assertEquals( "col1", joinColumns[1].name() );
		assertEquals( "col2", joinColumns[1].referencedColumnName() );
		assertEquals( "table1", joinColumns[1].table() );
		assertEquals( "int", joinColumns[1].columnDefinition() );
		assertFalse( joinColumns[1].insertable() );
		assertFalse( joinColumns[1].updatable() );
		assertFalse( joinColumns[1].nullable() );
		assertTrue( joinColumns[1].unique() );
	}

	@Test
	public void testCascadeAll() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm20.xml" );
		assertAnnotationPresent( OneToMany.class );
		OneToMany relAnno = reader.getAnnotation( OneToMany.class );
		assertEquals( 1, relAnno.cascade().length );
		assertEquals( CascadeType.ALL, relAnno.cascade()[0] );
	}

	@Test
	public void testCascadeSomeWithDefaultPersist() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm21.xml" );
		assertAnnotationPresent( OneToMany.class );
		OneToMany relAnno = reader.getAnnotation( OneToMany.class );
		assertEquals( 4, relAnno.cascade().length );
		assertEquals( CascadeType.REMOVE, relAnno.cascade()[0] );
		assertEquals( CascadeType.REFRESH, relAnno.cascade()[1] );
		assertEquals( CascadeType.DETACH, relAnno.cascade()[2] );
		assertEquals( CascadeType.PERSIST, relAnno.cascade()[3] );
	}

	/**
	 * Make sure that it doesn't break the handler when {@link CascadeType#ALL}
	 * is specified in addition to a default cascade-persist or individual
	 * cascade settings.
	 */
	@Test
	public void testCascadeAllPlusMore() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm22.xml" );
		assertAnnotationPresent( OneToMany.class );
		OneToMany relAnno = reader.getAnnotation( OneToMany.class );
		assertEquals( 6, relAnno.cascade().length );
		assertEquals( CascadeType.ALL, relAnno.cascade()[0] );
		assertEquals( CascadeType.PERSIST, relAnno.cascade()[1] );
		assertEquals( CascadeType.MERGE, relAnno.cascade()[2] );
		assertEquals( CascadeType.REMOVE, relAnno.cascade()[3] );
		assertEquals( CascadeType.REFRESH, relAnno.cascade()[4] );
		assertEquals( CascadeType.DETACH, relAnno.cascade()[5] );
	}

	@Test
	public void testAllAttributes() throws Exception {
		reader = getReader( Entity2.class, "field1", "one-to-many.orm23.xml" );
		assertAnnotationPresent( OneToMany.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationPresent( Access.class );
		OneToMany relAnno = reader.getAnnotation( OneToMany.class );
		assertEquals( 0, relAnno.cascade().length );
		assertEquals( FetchType.EAGER, relAnno.fetch() );
		assertEquals( "field2", relAnno.mappedBy() );
		assertTrue( relAnno.orphanRemoval() );
		assertEquals( Entity3.class, relAnno.targetEntity() );
		assertEquals(
				AccessType.PROPERTY, reader.getAnnotation( Access.class )
				.value()
		);
	}

}
