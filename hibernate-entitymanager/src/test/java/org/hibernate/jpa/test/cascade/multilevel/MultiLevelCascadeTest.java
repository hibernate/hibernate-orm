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
package org.hibernate.jpa.test.cascade.multilevel;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class MultiLevelCascadeTest extends BaseEntityManagerFunctionalTestCase {

	@TestForIssue( jiraKey = "HHH-5299" )
    @Test
    public void test() {
		EntityManager em = getOrCreateEntityManager( );
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Top top = new Top();
        em.persist( top );
        // Flush 1
        em.flush();
 
        Middle middle = new Middle( 1l );
        top.addMiddle( middle );
		middle.setTop( top );
		Bottom bottom = new Bottom();
		middle.setBottom( bottom );
		bottom.setMiddle( middle );
 
        Middle middle2 = new Middle( 2l );
		top.addMiddle(middle2);
		middle2.setTop( top );
		Bottom bottom2 = new Bottom();
        middle2.setBottom( bottom2 );
		bottom2.setMiddle( middle2 );
        // Flush 2
        em.flush();
        tx.commit();
        em.close();

        em = getOrCreateEntityManager();
        tx = em.getTransaction();
        tx.begin();

        top = em.find(Top.class, top.getId());

        assertEquals(2, top.getMiddles().size());
        for (Middle loadedMiddle : top.getMiddles()) {
            assertSame(top, loadedMiddle.getTop());
            assertNotNull(loadedMiddle.getBottom());
        }
		em.remove( top );
        em.close();
    }

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Top.class, Middle.class, Bottom.class };
	}

}
