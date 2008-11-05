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
package org.hibernate.envers.test.integration.onetoone.unidirectional;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnidirectionalWithNulls extends AbstractEntityTest {
    private Integer ed1_id;
    private Integer ed2_id;

    private Integer ing1_id;
    private Integer ing2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(UniRefEdEntity.class);
        cfg.addAnnotatedClass(UniRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        UniRefEdEntity ed1 = new UniRefEdEntity(1, "data_ed_1");
        UniRefEdEntity ed2 = new UniRefEdEntity(2, "data_ed_2");

        UniRefIngEntity ing1 = new UniRefIngEntity(3, "data_ing_1", ed1);
        UniRefIngEntity ing2 = new UniRefIngEntity(4, "data_ing_2", null);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);

        em.persist(ing1);
        em.persist(ing2);

        em.getTransaction().commit();

        // Revision 2

        em = getEntityManager();
        em.getTransaction().begin();

        ing1 = em.find(UniRefIngEntity.class, ing1.getId());

        ing1.setReference(null);

        em.getTransaction().commit();

        // Revision 3

        em = getEntityManager();
        em.getTransaction().begin();

        ing2 = em.find(UniRefIngEntity.class, ing2.getId());
        ed2 = em.find(UniRefEdEntity.class, ed2.getId());

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
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(UniRefEdEntity.class, ed1_id));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(UniRefEdEntity.class, ed2_id));

        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(UniRefIngEntity.class, ing1_id));
        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(UniRefIngEntity.class, ing2_id));
    }

    @Test
    public void testHistoryOfIngId1() {
        UniRefEdEntity ed1 = getEntityManager().find(UniRefEdEntity.class, ed1_id);

        UniRefIngEntity rev1 = getAuditReader().find(UniRefIngEntity.class, ing1_id, 1);
        UniRefIngEntity rev2 = getAuditReader().find(UniRefIngEntity.class, ing1_id, 2);
        UniRefIngEntity rev3 = getAuditReader().find(UniRefIngEntity.class, ing1_id, 3);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference() == null;
        assert rev3.getReference() == null;
    }

    @Test
    public void testHistoryOfIngId2() {
        UniRefEdEntity ed2 = getEntityManager().find(UniRefEdEntity.class, ed2_id);

        UniRefIngEntity rev1 = getAuditReader().find(UniRefIngEntity.class, ing2_id, 1);
        UniRefIngEntity rev2 = getAuditReader().find(UniRefIngEntity.class, ing2_id, 2);
        UniRefIngEntity rev3 = getAuditReader().find(UniRefIngEntity.class, ing2_id, 3);

        assert rev1.getReference() == null;
        assert rev2.getReference() == null;
        assert rev3.getReference().equals(ed2);
    }
}