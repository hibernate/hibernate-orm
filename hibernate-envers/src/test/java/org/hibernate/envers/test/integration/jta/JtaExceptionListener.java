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
package org.hibernate.envers.test.integration.jta;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.integration.reventity.ExceptionListenerRevEntity;
import org.hibernate.testing.tm.SimpleJtaTransactionManagerImpl;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;

/**
 * Same as {@link org.hibernate.envers.test.integration.reventity.ExceptionListener}, but in a JTA environment.
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaExceptionListener extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(ExceptionListenerRevEntity.class);

        addJTAConfig(cfg);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testTransactionRollback() throws Exception {
        SimpleJtaTransactionManagerImpl.getInstance().begin();

        // Trying to persist an entity - however the listener should throw an exception, so the entity
		// shouldn't be persisted
        newEntityManager();
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);

        SimpleJtaTransactionManagerImpl.getInstance().commit();
    }

    @Test(dependsOnMethods = "testTransactionRollback")
    public void testDataNotPersisted() throws Exception {
        SimpleJtaTransactionManagerImpl.getInstance().begin();

		// Checking if the entity became persisted
        newEntityManager();
		EntityManager em = getEntityManager();
        Long count = (Long) em.createQuery("select count(s) from StrTestEntity s where s.str = 'x'").getSingleResult();
		assert count == 0l;

        SimpleJtaTransactionManagerImpl.getInstance().commit();
    }
}
