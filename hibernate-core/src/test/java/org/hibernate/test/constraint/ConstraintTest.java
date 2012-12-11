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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * HHH-7797 re-wrote the way dialects handle unique constraints.  Test as
 * many variations of unique, not null, and primary key constraints as possible.
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
		// nothing yet -- more interested in DDL creation
	}
	
	// Primary key w/ not null and unique
	@Entity
	public static class Entity1 {
		@Id
		@GeneratedValue
//		@Column( nullable = false, unique = true)
		public long id;
		
		@Column( nullable = false, unique = true)
		public String foo;
	}
}