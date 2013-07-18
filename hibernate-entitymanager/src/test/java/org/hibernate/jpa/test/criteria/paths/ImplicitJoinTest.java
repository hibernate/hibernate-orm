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
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

/**
 * @author Steve Ebersole
 */
public class ImplicitJoinTest extends AbstractMetamodelSpecificTest {
	@Test
	public void testImplicitJoinFromExplicitCollectionJoin() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
		Root<Order> orderRoot = criteria.from( Order.class );
		Join<Order, LineItem> lineItemsJoin = orderRoot.join( Order_.lineItems );
		criteria.where( criteriaBuilder.lt( lineItemsJoin.get( LineItem_.quantity ), 2 ) );
		criteria.select( orderRoot ).distinct( true );
		TypedQuery<Order> query = em.createQuery( criteria );
		query.getResultList();

		em.getTransaction().commit();
		em.close();
	}
}
