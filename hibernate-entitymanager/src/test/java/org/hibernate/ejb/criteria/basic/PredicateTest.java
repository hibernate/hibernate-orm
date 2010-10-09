/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria.basic;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Order;

/**
 * Test the various predicates.
 *
 * @author Steve Ebersole
 */
public class PredicateTest extends AbstractMetamodelSpecificTest {
	public void testEmptyConjunction() {
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaBuilder builder = factory.getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
        CriteriaQuery<Order> orderCriteria = builder.createQuery(Order.class);
		Root<Order> orderRoot = orderCriteria.from(Order.class);
		orderCriteria.select(orderRoot);
		orderCriteria.where( builder.isTrue( builder.conjunction() ) );
		em.createQuery( orderCriteria ).getResultList();
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyDisjunction() {
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaBuilder builder = factory.getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
        CriteriaQuery<Order> orderCriteria = builder.createQuery(Order.class);
		Root<Order> orderRoot = orderCriteria.from(Order.class);
		orderCriteria.select(orderRoot);
		orderCriteria.where( builder.isFalse( builder.disjunction() ) );
		em.createQuery( orderCriteria ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
