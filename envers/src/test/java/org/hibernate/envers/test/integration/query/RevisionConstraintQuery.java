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
package org.hibernate.envers.test.integration.query;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.RevisionType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class RevisionConstraintQuery extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 15);

        em.persist(site1);
        em.persist(site2);

        id1 = site1.getId();
        Integer id2 = site2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setStr1("d");
        site2.setNumber(20);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setNumber(1);
        site2.setStr1("z");

        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setNumber(5);
        site2.setStr1("a");

        em.getTransaction().commit();
    }
    
    @Test
    public void testRevisionsLtQuery() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber().distinct())
                .add(AuditEntity.revisionNumber().lt(3))
                .getResultList();

        assert Arrays.asList(1, 2).equals(result);
    }

    @Test
    public void testRevisionsGeQuery() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber().distinct())
                .add(AuditEntity.revisionNumber().ge(2))
                .getResultList();

        assert Arrays.asList(2, 3, 4).equals(result);
    }

    @Test
    public void testRevisionsLeWithPropertyQuery() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.revisionNumber().le(3))
                .add(AuditEntity.property("str1").eq("a"))
                .getResultList();

        assert Arrays.asList(1).equals(result);
    }

    @Test
    public void testRevisionsGtWithPropertyQuery() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.revisionNumber().gt(1))
                .add(AuditEntity.property("number").lt(10))
                .getResultList();

        assert Arrays.asList(3, 4).equals(result);
    }

    @Test
    public void testRevisionProjectionQuery() {
        Object[] result = (Object[]) getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber().max())
                .addProjection(AuditEntity.revisionNumber().count())
                .addProjection(AuditEntity.revisionNumber().countDistinct())
                .addProjection(AuditEntity.revisionNumber().min())
                .add(AuditEntity.id().eq(id1))
                .getSingleResult();

        assert (Integer) result[0] == 4;
        assert (Long) result[1] == 4;
        assert (Long) result[2] == 4;
        assert (Integer) result[3] == 1;
    }

    @Test
    public void testRevisionOrderQuery() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.id().eq(id1))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        assert Arrays.asList(4, 3, 2, 1).equals(result);
    }

    @Test
    public void testRevisionCountQuery() {
        // The query shouldn't be ordered as always, otherwise - we get an exception.
        Object result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber().count())
                .add(AuditEntity.id().eq(id1))
                .getSingleResult();

        assert (Long) result == 4;
    }

    @Test
    public void testRevisionTypeEqQuery() {
        // The query shouldn't be ordered as always, otherwise - we get an exception.
        List results = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, true, true)
                .add(AuditEntity.id().eq(id1))
                .add(AuditEntity.revisionType().eq(RevisionType.MOD))
                .getResultList();
        
        assert results.size() == 3;
        assert results.get(0).equals(new StrIntTestEntity("d", 10, id1));
        assert results.get(1).equals(new StrIntTestEntity("d", 1, id1));
        assert results.get(2).equals(new StrIntTestEntity("d", 5, id1));
    }

    @Test
    public void testRevisionTypeNeQuery() {
        // The query shouldn't be ordered as always, otherwise - we get an exception.
        List results = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, true, true)
                .add(AuditEntity.id().eq(id1))
                .add(AuditEntity.revisionType().ne(RevisionType.MOD))
                .getResultList();

        assert results.size() == 1;
        assert results.get(0).equals(new StrIntTestEntity("a", 10, id1));
    }
}