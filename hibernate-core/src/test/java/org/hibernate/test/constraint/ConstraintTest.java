/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.constraint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class ConstraintTest extends BaseCoreFunctionalTestCase {
	
	private static final int MAX_NAME_LENGTH = 30;
	
	private static final String EXPLICIT_FK_NAME = "fk_explicit";
	
	private static final String EXPLICIT_UK_NAME = "uk_explicit";
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DataPoint.class, DataPoint2.class
		};
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7797" )
	public void testUniqueConstraints() {
		Column column = (Column) configuration().getClassMapping( DataPoint.class.getName() )
				.getProperty( "foo1" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) configuration().getClassMapping( DataPoint.class.getName() )
				.getProperty( "foo2" ).getColumnIterator().next();
		assertTrue( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) configuration().getClassMapping( DataPoint.class.getName() )
				.getProperty( "id" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-1904" )
	public void testConstraintNameLength() {
		Iterator<org.hibernate.mapping.Table> tableItr = configuration().getTableMappings();
		while (tableItr.hasNext()) {
			org.hibernate.mapping.Table table = tableItr.next();
			
			Iterator fkItr = table.getForeignKeyIterator();
			while (fkItr.hasNext()) {
				ForeignKey fk = (ForeignKey) fkItr.next();
				assertTrue( fk.getName().length() <= MAX_NAME_LENGTH );
				
				// ensure the randomly generated constraint name doesn't
				// happen if explicitly given
				Column column = fk.getColumn( 0 );
				if ( column.getName().equals( "explicit" ) ) {
					assertEquals( fk.getName(), EXPLICIT_FK_NAME );
				}
			}
			
			Iterator ukItr = table.getUniqueKeyIterator();
			while (ukItr.hasNext()) {
				UniqueKey uk = (UniqueKey) ukItr.next();
				assertTrue( uk.getName().length() <= MAX_NAME_LENGTH );
				
				// ensure the randomly generated constraint name doesn't
				// happen if explicitly given
				Column column = uk.getColumn( 0 );
				if ( column.getName().equals( "explicit" ) ) {
					assertEquals( uk.getName(), EXPLICIT_UK_NAME );
				}
			}
		}
	}
	
	@Entity
	@Table( name = "DataPoint", uniqueConstraints = {
			@UniqueConstraint( name = EXPLICIT_UK_NAME, columnNames = { "explicit" } )
	} )
	public static class DataPoint {
		@Id
		@GeneratedValue
		@javax.persistence.Column( nullable = false, unique = true)
		public long id;
		
		@javax.persistence.Column( nullable = false, unique = true)
		public String foo1;
		
		@javax.persistence.Column( nullable = true, unique = true)
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
		@org.hibernate.annotations.ForeignKey(name = EXPLICIT_FK_NAME)
		public DataPoint explicit;
	}
}