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
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.UniqueConstraint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Ejb3XmlOneToOneTest extends Ejb3XmlTestCase {
	@Test
	public void testNoChildren() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm1.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( MapsId.class );
		assertAnnotationNotPresent( Id.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		assertAnnotationNotPresent( Access.class );
		OneToOne relAnno = reader.getAnnotation( OneToOne.class );
		assertEquals( 0, relAnno.cascade().length );
		assertEquals( FetchType.EAGER, relAnno.fetch() );
		assertEquals( "", relAnno.mappedBy() );
		assertTrue( relAnno.optional() );
		assertFalse( relAnno.orphanRemoval() );
		assertEquals( void.class, relAnno.targetEntity() );
	}

	/**
	 * When there's a single primary key join column, we still wrap it with
	 * a PrimaryKeyJoinColumns annotation.
	 */
	@Test
	public void testSinglePrimaryKeyJoinColumn() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm2.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationPresent( PrimaryKeyJoinColumns.class );
		PrimaryKeyJoinColumns joinColumnsAnno =
				reader.getAnnotation( PrimaryKeyJoinColumns.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		PrimaryKeyJoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 1, joinColumns.length );
		assertEquals( "col1", joinColumns[0].name() );
		assertEquals( "col2", joinColumns[0].referencedColumnName() );
		assertEquals( "int", joinColumns[0].columnDefinition() );
	}

	@Test
	public void testMultiplePrimaryKeyJoinColumn() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm3.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationPresent( PrimaryKeyJoinColumns.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		PrimaryKeyJoinColumns joinColumnsAnno =
				reader.getAnnotation( PrimaryKeyJoinColumns.class );
		PrimaryKeyJoinColumn[] joinColumns = joinColumnsAnno.value();
		assertEquals( 2, joinColumns.length );
		assertEquals( "", joinColumns[0].name() );
		assertEquals( "", joinColumns[0].referencedColumnName() );
		assertEquals( "", joinColumns[0].columnDefinition() );
		assertEquals( "col1", joinColumns[1].name() );
		assertEquals( "col2", joinColumns[1].referencedColumnName() );
		assertEquals( "int", joinColumns[1].columnDefinition() );
	}

	/**
	 * When there's a single join column, we still wrap it with a JoinColumns
	 * annotation.
	 */
	@Test
	public void testSingleJoinColumn() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm4.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
		assertAnnotationPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
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
		reader = getReader( Entity1.class, "field1", "one-to-one.orm5.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
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
	public void testJoinTableNoChildren() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm6.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
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
		reader = getReader( Entity1.class, "field1", "one-to-one.orm7.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
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

	@Test
	public void testCascadeAll() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm8.xml" );
		assertAnnotationPresent( OneToOne.class );
		OneToOne relAnno = reader.getAnnotation( OneToOne.class );
		assertEquals( 1, relAnno.cascade().length );
		assertEquals( CascadeType.ALL, relAnno.cascade()[0] );
	}

	@Test
	public void testCascadeSomeWithDefaultPersist() throws Exception {
		reader = getReader( Entity1.class, "field1", "one-to-one.orm9.xml" );
		assertAnnotationPresent( OneToOne.class );
		OneToOne relAnno = reader.getAnnotation( OneToOne.class );
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
		reader = getReader( Entity1.class, "field1", "one-to-one.orm10.xml" );
		assertAnnotationPresent( OneToOne.class );
		OneToOne relAnno = reader.getAnnotation( OneToOne.class );
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
		reader = getReader( Entity1.class, "field1", "one-to-one.orm11.xml" );
		assertAnnotationPresent( OneToOne.class );
		assertAnnotationPresent( MapsId.class );
		assertAnnotationPresent( Id.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumn.class );
		assertAnnotationNotPresent( PrimaryKeyJoinColumns.class );
		assertAnnotationNotPresent( JoinColumns.class );
		assertAnnotationNotPresent( JoinColumn.class );
		assertAnnotationNotPresent( JoinTable.class );
		assertAnnotationPresent( Access.class );
		OneToOne relAnno = reader.getAnnotation( OneToOne.class );
		assertEquals( 0, relAnno.cascade().length );
		assertEquals( FetchType.LAZY, relAnno.fetch() );
		assertEquals( "field2", relAnno.mappedBy() );
		assertFalse( relAnno.optional() );
		assertTrue( relAnno.orphanRemoval() );
		assertEquals( Entity3.class, relAnno.targetEntity() );
		assertEquals(
				AccessType.PROPERTY, reader.getAnnotation( Access.class )
				.value()
		);
		assertEquals(
				"field3", reader.getAnnotation( MapsId.class )
				.value()
		);
	}

}
