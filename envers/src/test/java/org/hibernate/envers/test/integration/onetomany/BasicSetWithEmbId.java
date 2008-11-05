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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.ids.EmbId;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefIngEmbIdEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSetWithEmbId extends AbstractEntityTest {
    private EmbId ed1_id;
    private EmbId ed2_id;

    private EmbId ing1_id;
    private EmbId ing2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SetRefEdEmbIdEntity.class);
        cfg.addAnnotatedClass(SetRefIngEmbIdEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        ed1_id = new EmbId(0, 1);
        ed2_id = new EmbId(2, 3);

        ing2_id = new EmbId(4, 5);
        ing1_id = new EmbId(6, 7);

        EntityManager em = getEntityManager();

        SetRefEdEmbIdEntity ed1 = new SetRefEdEmbIdEntity(ed1_id, "data_ed_1");
        SetRefEdEmbIdEntity ed2 = new SetRefEdEmbIdEntity(ed2_id, "data_ed_2");

        SetRefIngEmbIdEntity ing1 = new SetRefIngEmbIdEntity(ing1_id, "data_ing_1", ed1);
        SetRefIngEmbIdEntity ing2 = new SetRefIngEmbIdEntity(ing2_id, "data_ing_2", ed1);

        // Revision 1
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);

        em.persist(ing1);
        em.persist(ing2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ing1 = em.find(SetRefIngEmbIdEntity.class, ing1.getId());
        ed2 = em.find(SetRefEdEmbIdEntity.class, ed2.getId());

        ing1.setReference(ed2);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        ing2 = em.find(SetRefIngEmbIdEntity.class, ing2.getId());
        ed2 = em.find(SetRefEdEmbIdEntity.class, ed2.getId());

        ing2.setReference(ed2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(SetRefEdEmbIdEntity.class, ed1_id));
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(SetRefEdEmbIdEntity.class, ed2_id));

        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(SetRefIngEmbIdEntity.class, ing1_id));
        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(SetRefIngEmbIdEntity.class, ing2_id));
    }

    private <T> Set<T> makeSet(T... objects) {
        Set<T> ret = new HashSet<T>();
        //noinspection ManualArrayToCollectionCopy
        for (T obj : objects) { ret.add(obj); }
        return ret;
    }

    @Test
    public void testHistoryOfEdId1() {
        SetRefIngEmbIdEntity ing1 = getEntityManager().find(SetRefIngEmbIdEntity.class, ing1_id);
        SetRefIngEmbIdEntity ing2 = getEntityManager().find(SetRefIngEmbIdEntity.class, ing2_id);

        SetRefEdEmbIdEntity rev1 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed1_id, 1);
        SetRefEdEmbIdEntity rev2 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed1_id, 2);
        SetRefEdEmbIdEntity rev3 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed1_id, 3);

        assert rev1.getReffering().equals(makeSet(ing1, ing2));
        assert rev2.getReffering().equals(makeSet(ing2));
        assert rev3.getReffering().equals(Collections.EMPTY_SET);
    }

    @Test
    public void testHistoryOfEdId2() {
        SetRefIngEmbIdEntity ing1 = getEntityManager().find(SetRefIngEmbIdEntity.class, ing1_id);
        SetRefIngEmbIdEntity ing2 = getEntityManager().find(SetRefIngEmbIdEntity.class, ing2_id);

        SetRefEdEmbIdEntity rev1 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed2_id, 1);
        SetRefEdEmbIdEntity rev2 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed2_id, 2);
        SetRefEdEmbIdEntity rev3 = getAuditReader().find(SetRefEdEmbIdEntity.class, ed2_id, 3);

        assert rev1.getReffering().equals(Collections.EMPTY_SET);
        assert rev2.getReffering().equals(makeSet(ing1));
        assert rev3.getReffering().equals(makeSet(ing1, ing2));
    }

    @Test
    public void testHistoryOfEdIng1() {
        SetRefEdEmbIdEntity ed1 = getEntityManager().find(SetRefEdEmbIdEntity.class, ed1_id);
        SetRefEdEmbIdEntity ed2 = getEntityManager().find(SetRefEdEmbIdEntity.class, ed2_id);

        SetRefIngEmbIdEntity rev1 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing1_id, 1);
        SetRefIngEmbIdEntity rev2 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing1_id, 2);
        SetRefIngEmbIdEntity rev3 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing1_id, 3);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference().equals(ed2);
        assert rev3.getReference().equals(ed2);
    }

    @Test
    public void testHistoryOfEdIng2() {
        SetRefEdEmbIdEntity ed1 = getEntityManager().find(SetRefEdEmbIdEntity.class, ed1_id);
        SetRefEdEmbIdEntity ed2 = getEntityManager().find(SetRefEdEmbIdEntity.class, ed2_id);

        SetRefIngEmbIdEntity rev1 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing2_id, 1);
        SetRefIngEmbIdEntity rev2 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing2_id, 2);
        SetRefIngEmbIdEntity rev3 = getAuditReader().find(SetRefIngEmbIdEntity.class, ing2_id, 3);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference().equals(ed1);
        assert rev3.getReference().equals(ed2);
    }
}