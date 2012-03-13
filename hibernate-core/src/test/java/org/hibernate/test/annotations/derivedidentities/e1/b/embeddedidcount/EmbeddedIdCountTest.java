/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.annotations.derivedidentities.e1.b.embeddedidcount;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class EmbeddedIdCountTest extends BaseCoreFunctionalTestCase {
    
    @Test
    public void testEmbeddedIdCount() {

        Session s = openSession();
        Transaction tx = s.beginTransaction();

        //creates the sql:
        //select count((purchaseor0_.POL_NUMBER, purchaseor0_.POL_LOCATION, purchaseor0_.POL_PO_ID)) as col_0_0_ from S_PURCH_ORDERLINE purchaseor0_
        long count = (Long) s.getNamedQuery("PurchaseOrderLine.count").uniqueResult();

        //creates the sql:
        // select count(purchaseor0_.PO_NUMBER) as col_0_0_ from S_PURCH_ORDER purchaseor0_
        long count2 = (Long) s.getNamedQuery("PurchaseOrder.count").uniqueResult();

        tx.rollback();
        s.close();
    }
    
    @Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
                PurchaseOrder.class,
                PurchaseOrderPK.class,
                PurchaseOrderLine.class,
                PurchaseOrderLinePK.class
        };
    }
}
