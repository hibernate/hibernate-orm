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
package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotVersioned extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity3.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity3 bte1 = new BasicTestEntity3("x", "y");
        em.persist(bte1);
        id1 = bte1.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        bte1 = em.find(BasicTestEntity3.class, id1);
        bte1.setStr1("a");
        bte1.setStr2("b");
        em.getTransaction().commit();
    }

    @Test(expectedExceptions = NotAuditedException.class)
    public void testRevisionsCounts() {
        getAuditReader().getRevisions(BasicTestEntity3.class, id1);
    }

    @Test(expectedExceptions = NotAuditedException.class)
    public void testHistoryOfId1() {
        getAuditReader().find(BasicTestEntity3.class, id1, 1);
    }
}