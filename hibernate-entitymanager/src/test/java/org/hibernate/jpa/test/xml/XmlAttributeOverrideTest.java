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
package org.hibernate.jpa.test.xml;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class XmlAttributeOverrideTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testAttributeOverriding() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

        Employee e = new Employee();
        e.setId(Long.valueOf(100));
        e.setName("Bubba");
        e.setHomeAddress(new Address("123 Main St", "New York", "NY", "11111"));
        e.setMailAddress(new Address("P.O. Box 123", "New York", "NY", "11111"));

        em.persist(e);

		em.flush();

		em.getTransaction().rollback();
		em.close();
	}

	@Test
	@FailureExpectedWithNewUnifiedXsd
    public void testDefaultEventListener() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CounterListener.reset();

		Employee e = new Employee();
        e.setId(Long.valueOf(100));
        e.setName("Bubba");
        e.setHomeAddress(new Address("123 Main St", "New York", "NY", "11111"));
        e.setMailAddress(new Address("P.O. Box 123", "New York", "NY", "11111"));

        em.persist(e);

		em.flush();

		em.clear();

		em.find( Employee.class, e.getId() ).setName( "Bibo" );

		em.flush();

		em.clear();

		em.remove( em.find( Employee.class, e.getId() ) );

		em.flush();


		em.getTransaction().rollback();
		em.close();

		assertEquals( 1, CounterListener.insert );
		assertEquals( 1, CounterListener.update );
		assertEquals( 1, CounterListener.delete );
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/jpa/test/xml/orm3.xml"
		};
	}
}
