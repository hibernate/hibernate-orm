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
package org.hibernate.envers.test.integration.naming;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicNaming extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(NamingTestEntity1.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        NamingTestEntity1 nte1 = new NamingTestEntity1("data1");
        NamingTestEntity1 nte2 = new NamingTestEntity1("data2");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(nte1);
        em.persist(nte2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        nte1 = em.find(NamingTestEntity1.class, nte1.getId());
        nte1.setData("data1'");

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        nte2 = em.find(NamingTestEntity1.class, nte2.getId());
        nte2.setData("data2'");

        em.getTransaction().commit();

        //

        id1 = nte1.getId();
        id2 = nte2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(NamingTestEntity1.class, id1));

        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(NamingTestEntity1.class, id2));
    }

    @Test
    public void testHistoryOfId1() {
        NamingTestEntity1 ver1 = new NamingTestEntity1(id1, "data1");
        NamingTestEntity1 ver2 = new NamingTestEntity1(id1, "data1'");

        assert getAuditReader().find(NamingTestEntity1.class, id1, 1).equals(ver1);
        assert getAuditReader().find(NamingTestEntity1.class, id1, 2).equals(ver2);
        assert getAuditReader().find(NamingTestEntity1.class, id1, 3).equals(ver2);
    }

    @Test
    public void testHistoryOfId2() {
        NamingTestEntity1 ver1 = new NamingTestEntity1(id2, "data2");
        NamingTestEntity1 ver2 = new NamingTestEntity1(id2, "data2'");

        assert getAuditReader().find(NamingTestEntity1.class, id2, 1).equals(ver1);
        assert getAuditReader().find(NamingTestEntity1.class, id2, 2).equals(ver1);
        assert getAuditReader().find(NamingTestEntity1.class, id2, 3).equals(ver2);
    }

    @Test
    public void testTableName() {
        assert "naming_test_entity_1_versions".equals(
                getCfg().getClassMapping("org.hibernate.envers.test.integration.naming.NamingTestEntity1_AUD")
                        .getTable().getName());
    }
}
