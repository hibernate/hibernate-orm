/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.jta;

import java.util.Properties;
import javax.persistence.EntityManager;
import javax.transaction.RollbackException;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.integration.reventity.ExceptionListenerRevEntity;

import org.hibernate.testing.jta.TestingJtaBootstrap;

/**
 * Same as {@link org.hibernate.envers.test.integration.reventity.ExceptionListener}, but in a JTA environment.
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaExceptionListener extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(ExceptionListenerRevEntity.class);
    }

    @Override
    public void addConfigurationProperties(Properties configuration) {
        super.addConfigurationProperties(configuration);
        TestingJtaBootstrap.prepare( configuration );
    }

    @Test(expected = RollbackException.class)
    @Priority(5) // must run before testDataNotPersisted()
    public void testTransactionRollback() throws Exception {
		TestingJtaBootstrap.INSTANCE.getTransactionManager().begin();

        try {
			newEntityManager();
			EntityManager em = getEntityManager();

            // Trying to persist an entity - however the listener should throw an exception, so the entity
		    // shouldn't be persisted
            StrTestEntity te = new StrTestEntity("x");
            em.persist(te);
        } finally {
			TestingJtaBootstrap.tryCommit();
        }
    }

    @Test
    public void testDataNotPersisted() throws Exception {
		TestingJtaBootstrap.INSTANCE.getTransactionManager().begin();

        try {
    		// Checking if the entity became persisted
            newEntityManager();
		    EntityManager em = getEntityManager();
            long count = em.createQuery("from StrTestEntity s where s.str = 'x'").getResultList().size();
		    Assert.assertEquals( 0, count );
        } finally {
			TestingJtaBootstrap.tryCommit();
        }
    }
}
