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
package org.hibernate.envers.test.integration.proxy;

import org.hibernate.ejb.Ejb3Configuration;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.manytoone.unidirectional.TargetNotAuditedEntity;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;


/**
 * @author Eugene Goroschenya
 */
public class ProxyIdentifier extends AbstractEntityTest {
    private TargetNotAuditedEntity tnae1;
    private UnversionedStrTestEntity uste1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(TargetNotAuditedEntity.class);
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        uste1 = new UnversionedStrTestEntity("str1");

        // No revision
        em.getTransaction().begin();
        em.persist(uste1);
        em.getTransaction().commit();

        // Revision 1
        em.getTransaction().begin();
        uste1 = em.find(UnversionedStrTestEntity.class, uste1.getId());
        tnae1 = new TargetNotAuditedEntity(1, "tnae1", uste1);
        em.persist(tnae1);
        em.getTransaction().commit();
    }

    @Test
    public void testProxyIdentifier() {
        TargetNotAuditedEntity rev1 = getAuditReader().find(TargetNotAuditedEntity.class, tnae1.getId(), 1);

        assert rev1.getReference() instanceof HibernateProxy;

        HibernateProxy proxyCreateByEnvers = (HibernateProxy) rev1.getReference();
        LazyInitializer lazyInitializer = proxyCreateByEnvers.getHibernateLazyInitializer();

        assert lazyInitializer.isUninitialized();
        assert lazyInitializer.getIdentifier() != null;
        assert lazyInitializer.getIdentifier().equals(tnae1.getId());
        assert lazyInitializer.isUninitialized();

        assert rev1.getReference().getId().equals(uste1.getId());
        assert rev1.getReference().getStr().equals(uste1.getStr());
        assert !lazyInitializer.isUninitialized();
    }
}
