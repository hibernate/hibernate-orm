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
package org.hibernate.jpa.test;


import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

public class CompositeIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				FirstEntityWithCompositePK.class,
				FirstCompositePK.class,
				SecondEntityWithCompositePK.class,
				SecondCompositePK.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6638" )
	public void testCompositeId() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		FirstEntityWithCompositePK entity = new FirstEntityWithCompositePK();
		FirstCompositePK pk = new FirstCompositePK();
		pk.setStringValue1( "string1" );
		pk.setStringValue2( "string2" );
		entity.setId( pk );
		entity.setStringValue( "stringValue" );
		em.persist( entity );
		em.flush();
		em.getTransaction().commit();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query query = em.createQuery("select fewc from FirstEntityWithCompositePK fewc");
		query.setMaxResults(10);
		List<FirstEntityWithCompositePK> listFEWC = query.getResultList();

		query = em.createQuery("select sewc from SecondEntityWithCompositePK sewc");
		query.setMaxResults(10);
		List<SecondEntityWithCompositePK> listSEWC = query.getResultList();
		em.getTransaction().commit();
	}

}
