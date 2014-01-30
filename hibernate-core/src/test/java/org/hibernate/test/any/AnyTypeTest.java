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
package org.hibernate.test.any;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.hql.internal.ast.QuerySyntaxException;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-1663" )
public class AnyTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "any/Person.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		// having second level cache causes a condition whereby the original test case would not fail...
		return null;
	}

	@Test
	public void testFlushProcessing() {
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		Address address = new Address();
		person.setData( address );
		session.saveOrUpdate(person);
		session.saveOrUpdate(address);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
        person = (Person) session.load( Person.class, person.getId() );
        person.setName("makingpersondirty");
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( person );
		session.getTransaction().commit();
		session.close();
	}

	@Test( expected = QuerySyntaxException.class )
	@FailureExpectedWithNewMetamodel
	public void testJoinFetchOfAnAnyTypeAttribute() {
		// Query translator should dis-allow join fetching of an <any/> mapping.  Let's make sure it does...
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "select p from Person p join fetch p.data" ).list();
		session.getTransaction().commit();
		session.close();
	}
}
