/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

import org.junit.Test;

/**
 * Similar to {@link org.hibernate.test.jpa.ql.OnKeywordTest}, but here testing from JPA criteria queries.
 *
 * @author Steve Ebersole
 */
public class OnKeywordTest extends AbstractMetamodelSpecificTest {
	@Test
	public void basicTest() {
		EntityManager em = getOrCreateEntityManager();
		CriteriaQuery<Order> criteria = em.getCriteriaBuilder().createQuery( Order.class );
		Root<Order> root = criteria.from( Order.class );
		criteria.select( root );
		CollectionJoin<Order,LineItem> lineItemsJoin = root.join( Order_.lineItems );
		lineItemsJoin.on(
				em.getCriteriaBuilder().gt(
						lineItemsJoin.get( LineItem_.quantity ),
						em.getCriteriaBuilder().literal( 20 )
				)
		);
		em.createQuery( criteria ).getResultList();
		em.close();
	}
}
