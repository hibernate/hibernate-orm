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
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Ejb3XmlElementCollectionTest extends Ejb3XmlTestCase {
	@Test
	public void testNoChildren() throws Exception {
		reader = getReader( Entity2.class, "field1", "element-collection.orm1.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( Column.class );
		assertAnnotationNotPresent( Temporal.class );
		assertAnnotationNotPresent( Enumerated.class );
		assertAnnotationNotPresent( Lob.class );
		assertAnnotationNotPresent( AttributeOverride.class );
		assertAnnotationNotPresent( AttributeOverrides.class );
		assertAnnotationNotPresent( AssociationOverride.class );
		assertAnnotationNotPresent( AssociationOverrides.class );
		assertAnnotationNotPresent( CollectionTable.class );
		assertAnnotationNotPresent( Access.class );
		ElementCollection relAnno = reader.getAnnotation( ElementCollection.class );
		assertEquals( FetchType.LAZY, relAnno.fetch() );
		assertEquals( void.class, relAnno.targetClass() );
	}

	@Test
	public void testOrderBy() throws Exception {
		reader = getReader( Entity2.class, "field1", "element-collection.orm2.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertEquals(
				"col1 ASC, col2 DESC", reader.getAnnotation( OrderBy.class )
				.value()
		);
	}

	@Test
	public void testOrderColumnNoAttributes() throws Exception {
		reader = getReader( Entity2.class, "field1", "element-collection.orm3.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity2.class, "field1", "element-collection.orm4.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm5.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm6.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm7.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm8.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm9.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm10.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm11.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm12.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm13.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm14.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
		reader = getReader( Entity3.class, "field1", "element-collection.orm15.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
	public void testColumnNoAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm16.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( Column.class );
		Column column = reader.getAnnotation( Column.class );
		assertEquals( "", column.name() );
		assertFalse( column.unique() );
		assertTrue( column.nullable() );
		assertTrue( column.insertable() );
		assertTrue( column.updatable() );
		assertEquals( "", column.columnDefinition() );
		assertEquals( "", column.table() );
		assertEquals( 255, column.length() );
		assertEquals( 0, column.precision() );
		assertEquals( 0, column.scale() );
	}

	@Test
	public void testColumnAllAttributes() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm17.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( Column.class );
		Column column = reader.getAnnotation( Column.class );
		assertEquals( "col1", column.name() );
		assertTrue( column.unique() );
		assertFalse( column.nullable() );
		assertFalse( column.insertable() );
		assertFalse( column.updatable() );
		assertEquals( "int", column.columnDefinition() );
		assertEquals( "table1", column.table() );
		assertEquals( 50, column.length() );
		assertEquals( 2, column.precision() );
		assertEquals( 1, column.scale() );
	}

	@Test
	public void testTemporal() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm18.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( Temporal.class );
		assertAnnotationNotPresent( Enumerated.class );
		assertAnnotationNotPresent( Lob.class );
		assertEquals(
				TemporalType.DATE, reader.getAnnotation(
				Temporal.class
		).value()
		);
	}

	@Test
	public void testEnumerated() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm19.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( Temporal.class );
		assertAnnotationPresent( Enumerated.class );
		assertAnnotationNotPresent( Lob.class );
		assertEquals(
				EnumType.STRING, reader.getAnnotation(
				Enumerated.class
		).value()
		);
	}

	@Test
	public void testLob() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm20.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( Temporal.class );
		assertAnnotationNotPresent( Enumerated.class );
		assertAnnotationPresent( Lob.class );
	}

	/**
	 * When there's a single attribute override, we still wrap it with an
	 * AttributeOverrides annotation.
	 */
	@Test
	public void testSingleAttributeOverride() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm21.xml" );
		assertAnnotationPresent( ElementCollection.class );
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
	public void testMultipleAttributeOverrides() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm22.xml" );
		assertAnnotationPresent( ElementCollection.class );
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

	/**
	 * Tests that map-key-attribute-override and attribute-override elements
	 * both end up in the AttributeOverrides annotation.
	 */
	@Test
	public void testMixedAttributeOverrides() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm23.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( AttributeOverride.class );
		assertAnnotationPresent( AttributeOverrides.class );
		AttributeOverrides overridesAnno = reader
				.getAnnotation( AttributeOverrides.class );
		AttributeOverride[] overrides = overridesAnno.value();
		assertEquals( 2, overrides.length );
		assertEquals( "field1", overrides[0].name() );
		assertEquals( "col1", overrides[0].column().name() );
		assertEquals( "field2", overrides[1].name() );
		assertEquals( "col2", overrides[1].column().name() );
	}

	/**
	 * When there's a single association override, we still wrap it with an
	 * AssociationOverrides annotation.
	 */
	@Test
	public void testSingleAssociationOverride() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm24.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( AssociationOverride.class );
		assertAnnotationPresent( AssociationOverrides.class );
		AssociationOverrides overridesAnno = reader.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides = overridesAnno.value();
		assertEquals( 1, overrides.length );
		assertEquals( "association1", overrides[0].name() );
		assertEquals( 0, overrides[0].joinColumns().length );
		assertEquals( "", overrides[0].joinTable().name() );
	}

	@Test
	public void testMultipleAssociationOverridesJoinColumns() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm25.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( AssociationOverride.class );
		assertAnnotationPresent( AssociationOverrides.class );
		AssociationOverrides overridesAnno = reader.getAnnotation( AssociationOverrides.class );
		AssociationOverride[] overrides = overridesAnno.value();
		assertEquals( 2, overrides.length );
		//First, an association using join table
		assertEquals( "association1", overrides[0].name() );
		assertEquals( 0, overrides[0].joinColumns().length );

		JoinTable joinTableAnno = overrides[0].joinTable();
		assertEquals( "catalog1", joinTableAnno.catalog() );
		assertEquals( "table1", joinTableAnno.name() );
		assertEquals( "schema1", joinTableAnno.schema() );

		//JoinColumns
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

		//InverseJoinColumns
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

		//UniqueConstraints
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

		//Second, an association using join columns
		assertEquals( "association2", overrides[1].name() );

		//JoinColumns
		joinColumns = overrides[1].joinColumns();
		assertEquals( 2, joinColumns.length );
		assertEquals( "", joinColumns[0].name() );
		assertEquals( "", joinColumns[0].referencedColumnName() );
		assertEquals( "", joinColumns[0].table() );
		assertEquals( "", joinColumns[0].columnDefinition() );
		assertTrue( joinColumns[0].insertable() );
		assertTrue( joinColumns[0].updatable() );
		assertTrue( joinColumns[0].nullable() );
		assertFalse( joinColumns[0].unique() );
		assertEquals( "col8", joinColumns[1].name() );
		assertEquals( "col9", joinColumns[1].referencedColumnName() );
		assertEquals( "table4", joinColumns[1].table() );
		assertEquals( "int", joinColumns[1].columnDefinition() );
		assertFalse( joinColumns[1].insertable() );
		assertFalse( joinColumns[1].updatable() );
		assertFalse( joinColumns[1].nullable() );
		assertTrue( joinColumns[1].unique() );
	}

	@Test
	public void testCollectionTableNoChildren() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm26.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( CollectionTable.class );
		CollectionTable tableAnno = reader.getAnnotation( CollectionTable.class );
		assertEquals( "", tableAnno.name() );
		assertEquals( "", tableAnno.catalog() );
		assertEquals( "", tableAnno.schema() );
		assertEquals( 0, tableAnno.joinColumns().length );
		assertEquals( 0, tableAnno.uniqueConstraints().length );
	}

	@Test
	public void testCollectionTableAllChildren() throws Exception {
		reader = getReader( Entity3.class, "field1", "element-collection.orm27.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationPresent( CollectionTable.class );
		CollectionTable tableAnno = reader.getAnnotation( CollectionTable.class );
		assertEquals( "table1", tableAnno.name() );
		assertEquals( "catalog1", tableAnno.catalog() );
		assertEquals( "schema1", tableAnno.schema() );

		//JoinColumns
		JoinColumn[] joinColumns = tableAnno.joinColumns();
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

		//UniqueConstraints
		UniqueConstraint[] uniqueConstraints = tableAnno.uniqueConstraints();
		assertEquals( 2, uniqueConstraints.length );
		assertEquals( "", uniqueConstraints[0].name() );
		assertEquals( 1, uniqueConstraints[0].columnNames().length );
		assertEquals( "col3", uniqueConstraints[0].columnNames()[0] );
		assertEquals( "uq1", uniqueConstraints[1].name() );
		assertEquals( 2, uniqueConstraints[1].columnNames().length );
		assertEquals( "col4", uniqueConstraints[1].columnNames()[0] );
		assertEquals( "col5", uniqueConstraints[1].columnNames()[1] );
	}

	@Test
	public void testAllAttributes() throws Exception {
		reader = getReader( Entity2.class, "field1", "element-collection.orm28.xml" );
		assertAnnotationPresent( ElementCollection.class );
		assertAnnotationNotPresent( OrderBy.class );
		assertAnnotationNotPresent( OrderColumn.class );
		assertAnnotationNotPresent( MapKey.class );
		assertAnnotationNotPresent( MapKeyClass.class );
		assertAnnotationNotPresent( MapKeyTemporal.class );
		assertAnnotationNotPresent( MapKeyEnumerated.class );
		assertAnnotationNotPresent( MapKeyColumn.class );
		assertAnnotationNotPresent( MapKeyJoinColumns.class );
		assertAnnotationNotPresent( MapKeyJoinColumn.class );
		assertAnnotationNotPresent( Column.class );
		assertAnnotationNotPresent( Temporal.class );
		assertAnnotationNotPresent( Enumerated.class );
		assertAnnotationNotPresent( Lob.class );
		assertAnnotationNotPresent( AttributeOverride.class );
		assertAnnotationNotPresent( AttributeOverrides.class );
		assertAnnotationNotPresent( AssociationOverride.class );
		assertAnnotationNotPresent( AssociationOverrides.class );
		assertAnnotationNotPresent( CollectionTable.class );
		assertAnnotationPresent( Access.class );
		ElementCollection relAnno = reader.getAnnotation( ElementCollection.class );
		assertEquals( FetchType.EAGER, relAnno.fetch() );
		assertEquals( Entity3.class, relAnno.targetClass() );
		assertEquals(
				AccessType.PROPERTY, reader.getAnnotation( Access.class )
				.value()
		);
	}

}
