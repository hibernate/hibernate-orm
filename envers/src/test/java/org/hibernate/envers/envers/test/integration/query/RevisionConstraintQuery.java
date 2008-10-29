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
package org.jboss.envers.test.integration.query;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.jboss.envers.query.RevisionProperty;
import org.jboss.envers.query.VersionsRestrictions;
import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrIntTestEntity;
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
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.distinct())
                .add(RevisionProperty.lt(3))
                .getResultList();

        assert Arrays.asList(1, 2).equals(result);
    }

    @Test
    public void testRevisionsGeQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.distinct())
                .add(RevisionProperty.ge(2))
                .getResultList();

        assert Arrays.asList(2, 3, 4).equals(result);
    }

    @Test
    public void testRevisionsLeWithPropertyQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(RevisionProperty.le(3))
                .add(VersionsRestrictions.eq("str1", "a"))
                .getResultList();

        assert Arrays.asList(1).equals(result);
    }

    @Test
    public void testRevisionsGtWithPropertyQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(RevisionProperty.gt(1))
                .add(VersionsRestrictions.lt("number", 10))
                .getResultList();

        assert Arrays.asList(3, 4).equals(result);
    }

    @Test
    public void testRevisionProjectionQuery() {
        Object[] result = (Object[]) getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.max())
                .addProjection(RevisionProperty.count())
                .addProjection(RevisionProperty.countDistinct())
                .addProjection(RevisionProperty.min())
                .add(VersionsRestrictions.idEq(id1))
                .getSingleResult();

        assert (Integer) result[0] == 4;
        assert (Long) result[1] == 4;
        assert (Long) result[2] == 4;
        assert (Integer) result[3] == 1;
    }

    @Test
    public void testRevisionOrderQuery() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.revisionNumber())
                .add(VersionsRestrictions.idEq(id1))
                .addOrder(RevisionProperty.desc())
                .getResultList();

        assert Arrays.asList(4, 3, 2, 1).equals(result);
    }

    @Test
    public void testRevisionCountQuery() {
        // The query shouldn't be ordered as always, otherwise - we get an exception.
        Object result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(RevisionProperty.count())
                .add(VersionsRestrictions.idEq(id1))
                .getSingleResult();

        assert (Long) result == 4;
    }
}