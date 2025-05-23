/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Brett Meyer
 */
public class ConstraintTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final int MAX_NAME_LENGTH = 30;

	private static final String EXPLICIT_FK_NAME_NATIVE = "fk_explicit_native";

	private static final String EXPLICIT_FK_NAME_JPA = "fk_explicit_jpa";

	private static final String EXPLICIT_UK_NAME = "uk_explicit";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DataPoint.class, DataPoint2.class };
	}

	@Test
	@JiraKey( value = "HHH-7797" )
	public void testUniqueConstraints() {
		Column column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo1" ).getSelectables().get( 0 );
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo2" ).getSelectables().get( 0 );
		assertTrue( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "id" ).getSelectables().get( 0 );
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );
	}

	@Test
	@JiraKey( value = "HHH-1904" )
	public void testConstraintNameLength() {
		int foundCount = 0;
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				for ( ForeignKey fk : table.getForeignKeyCollection() ) {
					assertTrue( fk.getName().length() <= MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = fk.getColumn( 0 );
					if ( column.getName().equals( "explicit_native" ) ) {
						foundCount++;
						assertEquals( EXPLICIT_FK_NAME_NATIVE, fk.getName() );
					}
					else if ( column.getName().equals( "explicit_jpa" ) ) {
						foundCount++;
						assertEquals( EXPLICIT_FK_NAME_JPA, fk.getName() );
					}
				}

				for ( UniqueKey uk : table.getUniqueKeys().values() ) {
					assertTrue( uk.getName().length() <= MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = uk.getColumn( 0 );
					if ( column.getName().equals( "explicit" ) ) {
						foundCount++;
						assertEquals( EXPLICIT_UK_NAME, uk.getName() );
					}
				}
			}

		}

		assertEquals("Could not find the necessary columns.", 3, foundCount);
	}

	@Entity
	@Table( name = "DataPoint", uniqueConstraints = {
			@UniqueConstraint( name = EXPLICIT_UK_NAME, columnNames = { "explicit" } )
	} )
	public static class DataPoint {
		@Id
		@GeneratedValue
		@jakarta.persistence.Column( nullable = false, unique = true)
		public long id;

		@jakarta.persistence.Column( nullable = false, unique = true)
		public String foo1;

		@jakarta.persistence.Column( nullable = true, unique = true)
		public String foo2;

		public String explicit;
	}

	@Entity
	@Table( name = "DataPoint2" )
	public static class DataPoint2 {
		@Id
		@GeneratedValue
		public long id;

		@OneToOne
		public DataPoint dp;

		@OneToOne
		@JoinColumn(name = "explicit_native",
				foreignKey = @jakarta.persistence.ForeignKey(name = EXPLICIT_FK_NAME_NATIVE))
		public DataPoint explicit_native;

		@OneToOne
		@JoinColumn(name = "explicit_jpa", foreignKey = @jakarta.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA))
		public DataPoint explicit_jpa;
	}
}
