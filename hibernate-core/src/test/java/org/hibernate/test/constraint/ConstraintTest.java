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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.mapping.Column;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * HHH-7797 re-wrote the way dialects handle unique constraints.  Test
 * variations of unique & not null to ensure the constraints are created
 * correctly for each dialect.
 * 
 * @author Brett Meyer
 */
@TestForIssue( jiraKey = "HHH-7797" )
public class ConstraintTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Entity1.class
		};
	}
	
	@Test
	public void testConstraints() {
		Column column = (Column) configuration().getClassMapping( Entity1.class.getName() )
				.getProperty( "foo1" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) configuration().getClassMapping( Entity1.class.getName() )
				.getProperty( "foo2" ).getColumnIterator().next();
		assertTrue( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) configuration().getClassMapping( Entity1.class.getName() )
				.getProperty( "id" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );
	}
	
	@Entity
	@Table( name = "Entity1" )
	public static class Entity1 {
		@Id
		@GeneratedValue
		@javax.persistence.Column( nullable = false, unique = true)
		public long id;
		
		@javax.persistence.Column( nullable = false, unique = true)
		public String foo1;
		
		@javax.persistence.Column( nullable = true, unique = true)
		public String foo2;
	}
}