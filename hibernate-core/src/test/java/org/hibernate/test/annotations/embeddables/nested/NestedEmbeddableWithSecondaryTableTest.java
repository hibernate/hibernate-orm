/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.test.annotations.embeddables.nested;

import static org.junit.Assert.assertEquals;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class NestedEmbeddableWithSecondaryTableTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-8021")
	public void testCase() {
		EntityA e = new EntityA();
		e.embedA.embedAttrA = "foo1";
		e.embedA.embedB.embedAttrB = "foo2";

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( e );
		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		s.getTransaction().begin();
		e = (EntityA) s.get( EntityA.class, e.id );
		assertEquals( "foo1", e.embedA.embedAttrA );
		assertEquals( "foo2", e.embedA.embedB.embedAttrB );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EmbeddableA.class, EmbeddableB.class };
	}

	@Entity
	@Table(name = "TableA")
	@SecondaryTables({ @SecondaryTable(name = "TableB") })
	public static class EntityA {

		@Id
		@GeneratedValue
		public Integer id;

		@Embedded
		@AttributeOverrides({ @AttributeOverride(name = "embedAttrA", column = @Column(table = "TableB")) })
		public EmbeddableA embedA = new EmbeddableA();
	}

	@Embeddable
	public static class EmbeddableA {

		@Embedded
		@AttributeOverrides({ @AttributeOverride(name = "embedAttrB", column = @Column(table = "TableB")) })
		public EmbeddableB embedB = new EmbeddableB();

		public String embedAttrA;
	}

	@Embeddable
	public static class EmbeddableB {

		public String embedAttrB;
	}
}
