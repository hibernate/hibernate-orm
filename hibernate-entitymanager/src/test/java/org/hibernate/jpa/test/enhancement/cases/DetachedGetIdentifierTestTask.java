/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.cases;

import org.hibernate.persister.entity.EntityPersister;

import org.junit.Assert;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static org.junit.Assert.assertTrue;

/**
 * @author Luis Barreiro
 */
public class DetachedGetIdentifierTestTask extends AbstractExecutable {

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ SimpleEntity.class };
    }

    @Override
    protected void prepared() {
        final EntityPersister ep = getEntityManagerFactory().getSessionFactory().getEntityPersister( SimpleEntity.class.getName() );
        assertTrue( ep.getInstrumentationMetadata().isEnhancedForLazyLoading() );
    }

    public void execute() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        SimpleEntity se = new SimpleEntity();
        se.name = "test";
        se = em.merge( se );

        Assert.assertNotNull( getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( se ) );

        em.getTransaction().commit();
        em.close();

        // Call as detached entity
        Assert.assertNotNull( getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( se ) );
    }

    protected void cleanup() {
    }

    @Entity(name = "SimpleEntity")
    public static class SimpleEntity {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

    }
}
