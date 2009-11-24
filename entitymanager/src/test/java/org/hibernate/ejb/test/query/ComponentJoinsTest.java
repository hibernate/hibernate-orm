/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb.test.query;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;
import org.hibernate.ejb.criteria.components.Client;

/**
 * Tests related to specifying joins on components (embedded values).
 *
 * @author Steve Ebersole
 */
public class ComponentJoinsTest extends TestCase {
	public Class[] getAnnotatedClasses() {
		return new Class[] { Client.class };
	}

	public void testComponentJoins() {
		// Just checking proper query construction and syntax checking via database query parser...
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		// use it in WHERE
		em.createQuery( "select c from Client c join c.name as n where n.lastName like '%'" ).getResultList();
		// use it in SELECT
		em.createQuery( "select n.lastName from Client c join c.name as n" ).getResultList();
		em.createQuery( "select n from Client c join c.name as n" ).getResultList();
		// use it in ORDER BY
		em.createQuery( "select n from Client c join c.name as n order by n.lastName" ).getResultList();
		em.createQuery( "select n from Client c join c.name as n order by c" ).getResultList();
		em.createQuery( "select n from Client c join c.name as n order by n" ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
