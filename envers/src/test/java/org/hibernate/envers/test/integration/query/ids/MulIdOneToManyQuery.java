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
package org.hibernate.envers.test.integration.query.ids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefIngMulIdEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class MulIdOneToManyQuery extends AbstractEntityTest {
    private MulId id1;
    private MulId id2;

    private MulId id3;
    private MulId id4;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SetRefEdMulIdEntity.class);
        cfg.addAnnotatedClass(SetRefIngMulIdEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = new MulId(0, 1);
        id2 = new MulId(10, 11);
        id3 = new MulId(20, 21);
        id4 = new MulId(30, 31);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        SetRefIngMulIdEntity refIng1 = new SetRefIngMulIdEntity(id1, "x", null);
        SetRefIngMulIdEntity refIng2 = new SetRefIngMulIdEntity(id2, "y", null);

        em.persist(refIng1);
        em.persist(refIng2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        SetRefEdMulIdEntity refEd3 = new SetRefEdMulIdEntity(id3, "a");
        SetRefEdMulIdEntity refEd4 = new SetRefEdMulIdEntity(id4, "a");

        em.persist(refEd3);
        em.persist(refEd4);

        refIng1 = em.find(SetRefIngMulIdEntity.class, id1);
        refIng2 = em.find(SetRefIngMulIdEntity.class, id2);

        refIng1.setReference(refEd3);
        refIng2.setReference(refEd4);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        refEd3 = em.find(SetRefEdMulIdEntity.class, id3);
        refIng2 = em.find(SetRefIngMulIdEntity.class, id2);
        refIng2.setReference(refEd3);

        em.getTransaction().commit();
    }

    @Test
    public void testEntitiesReferencedToId3() {
        Set rev1_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 1)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .getResultList());

        Set rev1 = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 1)
                .add(AuditEntity.property("reference").eq(new SetRefEdMulIdEntity(id3, null)))
                .getResultList());

        Set rev2_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 2)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .getResultList());

        Set rev2 = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 2)
                .add(AuditEntity.property("reference").eq(new SetRefEdMulIdEntity(id3, null)))
                .getResultList());

        Set rev3_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 3)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .getResultList());

        Set rev3 = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 3)
                .add(AuditEntity.property("reference").eq(new SetRefEdMulIdEntity(id3, null)))
                .getResultList());

        assert rev1.equals(rev1_related);
        assert rev2.equals(rev2_related);
        assert rev3.equals(rev3_related);

        assert rev1.equals(TestTools.makeSet());
        assert rev2.equals(TestTools.makeSet(new SetRefIngMulIdEntity(id1, "x", null)));
        assert rev3.equals(TestTools.makeSet(new SetRefIngMulIdEntity(id1, "x", null),
                new SetRefIngMulIdEntity(id2, "y", null)));
    }

    @Test
    public void testEntitiesReferencedToId4() {
        Set rev1_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 1)
                .add(AuditEntity.relatedId("reference").eq(id4))
                .getResultList());

        Set rev2_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 2)
                .add(AuditEntity.relatedId("reference").eq(id4))
                .getResultList());

        Set rev3_related = new HashSet(getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 3)
                .add(AuditEntity.relatedId("reference").eq(id4))
                .getResultList());

        assert rev1_related.equals(TestTools.makeSet());
        assert rev2_related.equals(TestTools.makeSet(new SetRefIngMulIdEntity(id2, "y", null)));
        assert rev3_related.equals(TestTools.makeSet());
    }

    @Test
    public void testEntitiesReferencedByIng1ToId3() {
        List rev1_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 1)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id1))
                .getResultList();

        Object rev2_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 2)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id1))
                .getSingleResult();

        Object rev3_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 3)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id1))
                .getSingleResult();

        assert rev1_related.size() == 0;
        assert rev2_related.equals(new SetRefIngMulIdEntity(id1, "x", null));
        assert rev3_related.equals(new SetRefIngMulIdEntity(id1, "x", null));
    }

    @Test
    public void testEntitiesReferencedByIng2ToId3() {
        List rev1_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 1)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id2))
                .getResultList();

        List rev2_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 2)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id2))
                .getResultList();

        Object rev3_related = getAuditReader().createQuery()
                .forEntitiesAtRevision(SetRefIngMulIdEntity.class, 3)
                .add(AuditEntity.relatedId("reference").eq(id3))
                .add(AuditEntity.id().eq(id2))
                .getSingleResult();

        assert rev1_related.size() == 0;
        assert rev2_related.size() == 0;
        assert rev3_related.equals(new SetRefIngMulIdEntity(id2, "y", null));
    }
}