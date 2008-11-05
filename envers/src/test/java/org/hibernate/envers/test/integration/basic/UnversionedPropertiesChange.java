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
public class UnversionedPropertiesChange extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity2.class);
    }

    private Integer addNewEntity(String str1, String str2) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity2 bte2 = new BasicTestEntity2(str1, str2);
        em.persist(bte2);
        em.getTransaction().commit();

        return bte2.getId();
    }

    private void modifyEntity(Integer id, String str1, String str2) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity2 bte2 = em.find(BasicTestEntity2.class, id);
        bte2.setStr1(str1);
        bte2.setStr2(str2);
        em.getTransaction().commit();
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = addNewEntity("x", "a"); // rev 1
        modifyEntity(id1, "x", "a"); // no rev
        modifyEntity(id1, "y", "b"); // rev 2
        modifyEntity(id1, "y", "c"); // no rev
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(BasicTestEntity2.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity2 ver1 = new BasicTestEntity2(id1, "x", null);
        BasicTestEntity2 ver2 = new BasicTestEntity2(id1, "y", null);

        assert getAuditReader().find(BasicTestEntity2.class, id1, 1).equals(ver1);
        assert getAuditReader().find(BasicTestEntity2.class, id1, 2).equals(ver2);
    }
}
