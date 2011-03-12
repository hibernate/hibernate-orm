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
package org.hibernate.envers.test.integration.onetomany.detached;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.onetomany.detached.IndexedListJoinColumnBidirectionalRefIngEntity;
import org.hibernate.envers.test.entities.onetomany.detached.IndexedListJoinColumnBidirectionalRefEdEntity;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * Test for a "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn (and thus owns the relatin),
 * and the other uses a @ManyToOne(insertable=false, updatable=false).
 * @author Adam Warski (adam at warski dot org)
 */
public class IndexedJoinColumnBidirectionalList extends AbstractEntityTest {
    private Integer ed1_id;
    private Integer ed2_id;
    private Integer ed3_id;

    private Integer ing1_id;
    private Integer ing2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IndexedListJoinColumnBidirectionalRefIngEntity.class);
        cfg.addAnnotatedClass(IndexedListJoinColumnBidirectionalRefEdEntity.class);
    }

    @Test(enabled = true)
    public void createData() {
        EntityManager em = getEntityManager();

        IndexedListJoinColumnBidirectionalRefEdEntity ed1 = new IndexedListJoinColumnBidirectionalRefEdEntity("ed1", null);
        IndexedListJoinColumnBidirectionalRefEdEntity ed2 = new IndexedListJoinColumnBidirectionalRefEdEntity("ed2", null);
        IndexedListJoinColumnBidirectionalRefEdEntity ed3 = new IndexedListJoinColumnBidirectionalRefEdEntity("ed3", null);

        IndexedListJoinColumnBidirectionalRefIngEntity ing1 = new IndexedListJoinColumnBidirectionalRefIngEntity("coll1", ed1, ed2, ed3);
        IndexedListJoinColumnBidirectionalRefIngEntity ing2 = new IndexedListJoinColumnBidirectionalRefIngEntity("coll1");

        // Revision 1 (ing1: ed1, ed2, ed3)
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);
        em.persist(ed3);
        em.persist(ing1);
        em.persist(ing2);

        em.getTransaction().commit();

        // Revision 2 (ing1: ed1, ed3, ing2: ed2)
        em.getTransaction().begin();

        ing1 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId());
        ing2 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId());
        ed2 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId());

        ing1.getReferences().remove(ed2);
        ing2.getReferences().add(ed2);

        em.getTransaction().commit();
        em.clear();

        // Revision 3 (ing1: ed3, ed1, ing2: ed2)
        em.getTransaction().begin();

        ing1 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId());
        ing2 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId());
        ed1 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1.getId());
        ed2 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId());
        ed3 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3.getId());

        ing1.getReferences().remove(ed3);
        ing1.getReferences().add(0, ed3);

        em.getTransaction().commit();
        em.clear();

        // Revision 4 (ing1: ed2, ed3, ed1)
        em.getTransaction().begin();

        ing1 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1.getId());
        ing2 = em.find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2.getId());
        ed1 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1.getId());
        ed2 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2.getId());
        ed3 = em.find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3.getId());

        ing2.getReferences().remove(ed2);
        ing1.getReferences().add(0, ed2);

        em.getTransaction().commit();
        em.clear();

        //

        ing1_id = ing1.getId();
        ing2_id = ing2.getId();

        ed1_id = ed1.getId();
        ed2_id = ed2.getId();
        ed3_id = ed3.getId();
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1, 2, 3, 4), getAuditReader().getRevisions(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id));
        assertEquals(Arrays.asList(1, 2, 4), getAuditReader().getRevisions(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id));

        assertEquals(Arrays.asList(1, 3, 4), getAuditReader().getRevisions(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id));
        assertEquals(Arrays.asList(1, 2, 4), getAuditReader().getRevisions(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id));
        assertEquals(Arrays.asList(1, 2, 3, 4), getAuditReader().getRevisions(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id));
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testHistoryOfIng1() {
        IndexedListJoinColumnBidirectionalRefEdEntity ed1 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id);
        IndexedListJoinColumnBidirectionalRefEdEntity ed2 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id);
        IndexedListJoinColumnBidirectionalRefEdEntity ed3 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id);

        IndexedListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id, 1);
        IndexedListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id, 2);
        IndexedListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id, 3);
        IndexedListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id, 4);

        assertEquals(rev1.getReferences().size(), 3);
        assertEquals(rev1.getReferences().get(0), ed1);
        assertEquals(rev1.getReferences().get(1), ed2);
        assertEquals(rev1.getReferences().get(2), ed3);

        assertEquals(rev2.getReferences().size(), 2);
        assertEquals(rev2.getReferences().get(0), ed1);
        assertEquals(rev2.getReferences().get(1), ed3);

        assertEquals(rev3.getReferences().size(), 2);
        assertEquals(rev3.getReferences().get(0), ed3);
        assertEquals(rev3.getReferences().get(1), ed1);

        assertEquals(rev4.getReferences().size(), 3);
        assertEquals(rev4.getReferences().get(0), ed2);
        assertEquals(rev4.getReferences().get(1), ed3);
        assertEquals(rev4.getReferences().get(2), ed1);
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testHistoryOfIng2() {
        IndexedListJoinColumnBidirectionalRefEdEntity ed2 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id);

        IndexedListJoinColumnBidirectionalRefIngEntity rev1 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id, 1);
        IndexedListJoinColumnBidirectionalRefIngEntity rev2 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id, 2);
        IndexedListJoinColumnBidirectionalRefIngEntity rev3 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id, 3);
        IndexedListJoinColumnBidirectionalRefIngEntity rev4 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id, 4);

        assertEquals(rev1.getReferences().size(), 0);

        assertEquals(rev2.getReferences().size(), 1);
        assertEquals(rev2.getReferences().get(0), ed2);

        assertEquals(rev3.getReferences().size(), 1);
        assertEquals(rev3.getReferences().get(0), ed2);

        assertEquals(rev4.getReferences().size(), 0);
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testHistoryOfEd1() {
        IndexedListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id);

        IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id, 1);
        IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id, 2);
        IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id, 3);
        IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed1_id, 4);

        assertTrue(rev1.getOwner().equals(ing1));
        assertTrue(rev2.getOwner().equals(ing1));
        assertTrue(rev3.getOwner().equals(ing1));
        assertTrue(rev4.getOwner().equals(ing1));

        assertEquals(rev1.getPosition(), new Integer(0));
        assertEquals(rev2.getPosition(), new Integer(0));
        assertEquals(rev3.getPosition(), new Integer(1));
        assertEquals(rev4.getPosition(), new Integer(2));
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testHistoryOfEd2() {
        IndexedListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id);
        IndexedListJoinColumnBidirectionalRefIngEntity ing2 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing2_id);

        IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id, 1);
        IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id, 2);
        IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id, 3);
        IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed2_id, 4);

        assertTrue(rev1.getOwner().equals(ing1));
        assertTrue(rev2.getOwner().equals(ing2));
        assertTrue(rev3.getOwner().equals(ing2));
        assertTrue(rev4.getOwner().equals(ing1));

        assertEquals(rev1.getPosition(), new Integer(1));
        assertEquals(rev2.getPosition(), new Integer(0));
        assertEquals(rev3.getPosition(), new Integer(0));
        assertEquals(rev4.getPosition(), new Integer(0));
    }

    @Test(enabled = true, dependsOnMethods = "createData")
    public void testHistoryOfEd3() {
        IndexedListJoinColumnBidirectionalRefIngEntity ing1 = getEntityManager().find(IndexedListJoinColumnBidirectionalRefIngEntity.class, ing1_id);

        IndexedListJoinColumnBidirectionalRefEdEntity rev1 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id, 1);
        IndexedListJoinColumnBidirectionalRefEdEntity rev2 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id, 2);
        IndexedListJoinColumnBidirectionalRefEdEntity rev3 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id, 3);
        IndexedListJoinColumnBidirectionalRefEdEntity rev4 = getAuditReader().find(IndexedListJoinColumnBidirectionalRefEdEntity.class, ed3_id, 4);

        assertTrue(rev1.getOwner().equals(ing1));
        assertTrue(rev2.getOwner().equals(ing1));
        assertTrue(rev3.getOwner().equals(ing1));
        assertTrue(rev4.getOwner().equals(ing1));

        assertEquals(rev1.getPosition(), new Integer(2));
        assertEquals(rev2.getPosition(), new Integer(1));
        assertEquals(rev3.getPosition(), new Integer(0));
        assertEquals(rev4.getPosition(), new Integer(1));
    }
}