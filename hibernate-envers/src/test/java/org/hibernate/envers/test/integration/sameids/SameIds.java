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
package org.hibernate.envers.test.integration.sameids;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * A test which checks that if we add two different entities with the same ids in one revision, they
 * will both be stored.
 * @author Adam Warski (adam at warski dot org)
 */
public class SameIds extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SameIdTestEntity1.class);
        cfg.addAnnotatedClass(SameIdTestEntity2.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        SameIdTestEntity1 site1 = new SameIdTestEntity1(1, "str1");
        SameIdTestEntity2 site2 = new SameIdTestEntity2(1, "str1");

        em.persist(site1);
        em.persist(site2);
        em.getTransaction().commit();

        em.getTransaction().begin();
        site1 = em.find(SameIdTestEntity1.class, 1);
        site2 = em.find(SameIdTestEntity2.class, 1);
        site1.setStr1("str2");
        site2.setStr1("str2");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(SameIdTestEntity1.class, 1));
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(SameIdTestEntity2.class, 1));
    }

    @Test
    public void testHistoryOfSite1() {
        SameIdTestEntity1 ver1 = new SameIdTestEntity1(1, "str1");
        SameIdTestEntity1 ver2 = new SameIdTestEntity1(1, "str2");

        assert getAuditReader().find(SameIdTestEntity1.class, 1, 1).equals(ver1);
        assert getAuditReader().find(SameIdTestEntity1.class, 1, 2).equals(ver2);
    }

    @Test
    public void testHistoryOfSite2() {
        SameIdTestEntity2 ver1 = new SameIdTestEntity2(1, "str1");
        SameIdTestEntity2 ver2 = new SameIdTestEntity2(1, "str2");

        assert getAuditReader().find(SameIdTestEntity2.class, 1, 1).equals(ver1);
        assert getAuditReader().find(SameIdTestEntity2.class, 1, 2).equals(ver2);
    }
}
