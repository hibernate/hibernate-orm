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
package org.hibernate.envers.test.integration.onetomany;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.ListRefIngEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicList extends AbstractEntityTest {
    private Integer ed1_id;
    private Integer ed2_id;

    private Integer ing1_id;
    private Integer ing2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ListRefEdEntity.class);
        cfg.addAnnotatedClass(ListRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        ListRefEdEntity ed1 = new ListRefEdEntity(1, "data_ed_1");
        ListRefEdEntity ed2 = new ListRefEdEntity(2, "data_ed_2");

        ListRefIngEntity ing1 = new ListRefIngEntity(3, "data_ing_1", ed1);
        ListRefIngEntity ing2 = new ListRefIngEntity(4, "data_ing_2", ed1);

        // Revision 1
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);

        em.persist(ing1);
        em.persist(ing2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ing1 = em.find(ListRefIngEntity.class, ing1.getId());
        ed2 = em.find(ListRefEdEntity.class, ed2.getId());

        ing1.setReference(ed2);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        ing2 = em.find(ListRefIngEntity.class, ing2.getId());
        ed2 = em.find(ListRefEdEntity.class, ed2.getId());

        ing2.setReference(ed2);

        em.getTransaction().commit();

        //

        ed1_id = ed1.getId();
        ed2_id = ed2.getId();

        ing1_id = ing1.getId();
        ing2_id = ing2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(ListRefEdEntity.class, ed1_id));
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(ListRefEdEntity.class, ed2_id));

        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(ListRefIngEntity.class, ing1_id));
        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(ListRefIngEntity.class, ing2_id));
    }
    @Test
    public void testHistoryOfEdId1() {
        ListRefIngEntity ing1 = getEntityManager().find(ListRefIngEntity.class, ing1_id);
        ListRefIngEntity ing2 = getEntityManager().find(ListRefIngEntity.class, ing2_id);

        ListRefEdEntity rev1 = getAuditReader().find(ListRefEdEntity.class, ed1_id, 1);
        ListRefEdEntity rev2 = getAuditReader().find(ListRefEdEntity.class, ed1_id, 2);
        ListRefEdEntity rev3 = getAuditReader().find(ListRefEdEntity.class, ed1_id, 3);

        assert TestTools.checkList(rev1.getReffering(), ing1, ing2);
        assert TestTools.checkList(rev2.getReffering(), ing2);
        assert TestTools.checkList(rev3.getReffering());
    }

    @Test
    public void testHistoryOfEdId2() {
        ListRefIngEntity ing1 = getEntityManager().find(ListRefIngEntity.class, ing1_id);
        ListRefIngEntity ing2 = getEntityManager().find(ListRefIngEntity.class, ing2_id);

        ListRefEdEntity rev1 = getAuditReader().find(ListRefEdEntity.class, ed2_id, 1);
        ListRefEdEntity rev2 = getAuditReader().find(ListRefEdEntity.class, ed2_id, 2);
        ListRefEdEntity rev3 = getAuditReader().find(ListRefEdEntity.class, ed2_id, 3);

        assert TestTools.checkList(rev1.getReffering());
        assert TestTools.checkList(rev2.getReffering(), ing1);
        assert TestTools.checkList(rev3.getReffering(), ing1, ing2);
    }

    @Test
    public void testHistoryOfEdIng1() {
        ListRefEdEntity ed1 = getEntityManager().find(ListRefEdEntity.class, ed1_id);
        ListRefEdEntity ed2 = getEntityManager().find(ListRefEdEntity.class, ed2_id);

        ListRefIngEntity rev1 = getAuditReader().find(ListRefIngEntity.class, ing1_id, 1);
        ListRefIngEntity rev2 = getAuditReader().find(ListRefIngEntity.class, ing1_id, 2);
        ListRefIngEntity rev3 = getAuditReader().find(ListRefIngEntity.class, ing1_id, 3);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference().equals(ed2);
        assert rev3.getReference().equals(ed2);
    }

    @Test
    public void testHistoryOfEdIng2() {
        ListRefEdEntity ed1 = getEntityManager().find(ListRefEdEntity.class, ed1_id);
        ListRefEdEntity ed2 = getEntityManager().find(ListRefEdEntity.class, ed2_id);

        ListRefIngEntity rev1 = getAuditReader().find(ListRefIngEntity.class, ing2_id, 1);
        ListRefIngEntity rev2 = getAuditReader().find(ListRefIngEntity.class, ing2_id, 2);
        ListRefIngEntity rev3 = getAuditReader().find(ListRefIngEntity.class, ing2_id, 3);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference().equals(ed1);
        assert rev3.getReference().equals(ed2);
    }
}