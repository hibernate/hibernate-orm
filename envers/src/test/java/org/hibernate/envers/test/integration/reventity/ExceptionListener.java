/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.reventity;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ExceptionListener extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(ExceptionListenerRevEntity.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testTransactionRollback() throws InterruptedException {
        // Trying to persist an entity - however the listener should throw an exception, so the entity
		// shouldn't be persisted
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        em.getTransaction().commit();
    }

    @Test(dependsOnMethods = "testTransactionRollback")
    public void testDataNotPersisted() {
		// Checking if the entity became persisted
		EntityManager em = getEntityManager();
        em.getTransaction().begin();
        Long count = (Long) em.createQuery("select count(s) from StrTestEntity s where s.str = 'x'").getSingleResult();
		assert count == 0l;
        em.getTransaction().commit();
    }
}