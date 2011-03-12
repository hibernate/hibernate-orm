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

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyOperationsInTransaction extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;
    private Integer id3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity1.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        BasicTestEntity1 bte1 = new BasicTestEntity1("x", 1);
        BasicTestEntity1 bte2 = new BasicTestEntity1("y", 20);
        em.persist(bte1);
        em.persist(bte2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        bte1 = em.find(BasicTestEntity1.class, bte1.getId());
        bte2 = em.find(BasicTestEntity1.class, bte2.getId());
        BasicTestEntity1 bte3 = new BasicTestEntity1("z", 300);
        bte1.setStr1("x2");
        bte2.setLong1(21);
        em.persist(bte3);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        bte2 = em.find(BasicTestEntity1.class, bte2.getId());
        bte3 = em.find(BasicTestEntity1.class, bte3.getId());
        bte2.setStr1("y3");
        bte2.setLong1(22);
        bte3.setStr1("z3");

        em.getTransaction().commit();

        id1 = bte1.getId();
        id2 = bte2.getId();
        id3 = bte3.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(BasicTestEntity1.class, id1));

        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(BasicTestEntity1.class, id2));

        assert Arrays.asList(2, 3).equals(getAuditReader().getRevisions(BasicTestEntity1.class, id3));
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id1, "x", 1);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id1, "x2", 1);

        assert getAuditReader().find(BasicTestEntity1.class, id1, 1).equals(ver1);
        assert getAuditReader().find(BasicTestEntity1.class, id1, 2).equals(ver2);
        assert getAuditReader().find(BasicTestEntity1.class, id1, 3).equals(ver2);
    }

    @Test
    public void testHistoryOfId2() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id2, "y", 20);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id2, "y", 21);
        BasicTestEntity1 ver3 = new BasicTestEntity1(id2, "y3", 22);

        assert getAuditReader().find(BasicTestEntity1.class, id2, 1).equals(ver1);
        assert getAuditReader().find(BasicTestEntity1.class, id2, 2).equals(ver2);
        assert getAuditReader().find(BasicTestEntity1.class, id2, 3).equals(ver3);
    }

    @Test
    public void testHistoryOfId3() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id3, "z", 300);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id3, "z3", 300);

        assert getAuditReader().find(BasicTestEntity1.class, id3, 1) == null;
        assert getAuditReader().find(BasicTestEntity1.class, id3, 2).equals(ver1);
        assert getAuditReader().find(BasicTestEntity1.class, id3, 3).equals(ver2);
    }
}
