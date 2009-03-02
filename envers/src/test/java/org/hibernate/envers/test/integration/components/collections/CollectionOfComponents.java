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
package org.hibernate.envers.test.integration.components.collections;

import java.util.Arrays;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.ComponentSetTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * TODO: enable and implement
 * @author Adam Warski (adam at warski dot org)
 */
public class CollectionOfComponents extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ComponentSetTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        ComponentSetTestEntity cte1 = new ComponentSetTestEntity();

        em.persist(cte1);

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        cte1 = em.find(ComponentSetTestEntity.class, cte1.getId());

        cte1.getComps().add(new Component1("a", "b"));

        em.getTransaction().commit();

        id1 = cte1.getId();
    }

    @Test(enabled = false)
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(ComponentSetTestEntity.class, id1));
    }

    @Test(enabled = false)
    public void testHistoryOfId1() {
        assert getAuditReader().find(ComponentSetTestEntity.class, id1, 1).getComps().size() == 0;
		
		Set<Component1> comps1 = getAuditReader().find(ComponentSetTestEntity.class, id1, 2).getComps();
        assert comps1.size() == 1;
		assert comps1.contains(new Component1("a", "b"));
    }
}