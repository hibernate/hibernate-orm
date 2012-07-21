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
package org.hibernate.jpa.test.util;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class GetIdentifierTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testSimpleId() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Book book = new Book();
		em.persist( book );
		em.flush();
		assertEquals( book.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( book ) );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testEmbeddedId() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Umbrella umbrella = new Umbrella();
		umbrella.setId( new Umbrella.PK() );
		umbrella.getId().setBrand( "Burberry" );
		umbrella.getId().setModel( "Red Hat" );
		em.persist( umbrella );
		em.flush();
		assertEquals( umbrella.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( umbrella ) );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testIdClass() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Sickness sick = new Sickness();

		sick.setClassification( "H1N1" );
		sick.setType("Flu");
		em.persist( sick );
		em.flush();
		Sickness.PK id = new Sickness.PK();
		id.setClassification( sick.getClassification() );
		id.setType( sick.getType() );
		assertEquals( id, em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( sick ) );
		em.getTransaction().rollback();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Umbrella.class,
				Sickness.class,
				Author.class
		};
	}
}
