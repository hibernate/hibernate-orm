/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.criteria.basic;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicCriteriaUsageTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Wall.class };
	}

	@Test
	public void testParameterCollection() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
		Root<Wall> from = criteria.from( Wall.class );
		ParameterExpression param = em.getCriteriaBuilder().parameter( String.class );
		SingularAttribute<? super Wall,?> colorAttribute = em.getMetamodel()
				.entity( Wall.class )
				.getDeclaredSingularAttribute( "color" );
		assertNotNull( "metamodel returned null singular attribute", colorAttribute );
		Predicate predicate = em.getCriteriaBuilder().equal( from.get( colorAttribute ), param );
		criteria.where( predicate );
		assertEquals( 1, criteria.getParameters().size() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTrivialCompilation() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
		criteria.from( Wall.class );
		em.createQuery( criteria ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
