/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.type;

import org.hibernate.Query;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleksandr Dukhno
 */
public class AttributeConverterEnumTest extends BaseCoreFunctionalTestCase {

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithConvertibleField.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8866")
	public void testEnumConverter() {
		Session s = openSession();
		s.getTransaction().begin();
		EntityWithConvertibleField entity = new EntityWithConvertibleField();
		entity.setId( "testEnumID" );
		entity.setTestEnum( ConvertibleEnum.VALUE );
		String entityID = entity.getId();

		s.persist( entity );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();

		s.beginTransaction();
		entity = (EntityWithConvertibleField) s.load( EntityWithConvertibleField.class, entityID );
		assertEquals( ConvertibleEnum.VALUE, entity.getTestEnum() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8866")
	public void testHqlQueryEnumConverter() {
		Session s = openSession();
		s.beginTransaction();
		EntityWithConvertibleField entity = new EntityWithConvertibleField();
		entity.setId( "testHqlQueryID" );
		entity.setTestEnum( ConvertibleEnum.VALUE );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Query q = s.createQuery(
				"SELECT ewcf " +
						"FROM EntityWithConvertibleField ewcf " +
						"WHERE ewcf.testEnum = org.hibernate.test.type.ConvertibleEnum.VALUE"
		);
		entity = (EntityWithConvertibleField) q.list().iterator().next();
		assertEquals( ConvertibleEnum.VALUE, entity.getTestEnum() );
		s.getTransaction().commit();
		s.close();
	}

}
