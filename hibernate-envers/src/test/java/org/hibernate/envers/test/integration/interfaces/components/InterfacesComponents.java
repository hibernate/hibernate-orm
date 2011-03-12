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
package org.hibernate.envers.test.integration.interfaces.components;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InterfacesComponents extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ComponentTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        ComponentTestEntity cte1 = new ComponentTestEntity(new Component1("a"));

        em.persist(cte1);

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        cte1 = em.find(ComponentTestEntity.class, cte1.getId());

        cte1.setComp1(new Component1("b"));

        em.getTransaction().commit();

        // Revision 3
        em = getEntityManager();
        em.getTransaction().begin();

        cte1 = em.find(ComponentTestEntity.class, cte1.getId());

        cte1.getComp1().setData("c");

        em.getTransaction().commit();

        id1 = cte1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(ComponentTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        ComponentTestEntity ver1 = new ComponentTestEntity(id1, new Component1("a"));
		ComponentTestEntity ver2 = new ComponentTestEntity(id1, new Component1("b"));
		ComponentTestEntity ver3 = new ComponentTestEntity(id1, new Component1("c"));

        assert getAuditReader().find(ComponentTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(ComponentTestEntity.class, id1, 2).equals(ver2);
        assert getAuditReader().find(ComponentTestEntity.class, id1, 3).equals(ver3);
    }
}