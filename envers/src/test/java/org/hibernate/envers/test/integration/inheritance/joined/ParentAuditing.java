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

package org.hibernate.envers.test.integration.inheritance.joined;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParentAuditing extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildEntity.class);
        cfg.addAnnotatedClass(ParentEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        id1 = 1;

        // Rev 1
        em.getTransaction().begin();
        ParentEntity pe = new ParentEntity(id1, "x");
        em.persist(pe);
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        pe = em.find(ParentEntity.class, id1);
        pe.setData("y");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(ParentEntity.class, id1));
    }

    @Test
    public void testHistoryOfChildId1() {
        assert getAuditReader().find(ChildEntity.class, id1, 1) == null;
        assert getAuditReader().find(ChildEntity.class, id1, 2) == null;
    }

    @Test
    public void testHistoryOfParentId1() {
        ParentEntity ver1 = new ParentEntity(id1, "x");
        ParentEntity ver2 = new ParentEntity(id1, "y");

        assert getAuditReader().find(ParentEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(ParentEntity.class, id1, 2).equals(ver2);
    }

    @Test
    public void testPolymorphicQuery() {
        ParentEntity parentVer1 = new ParentEntity(id1, "x");

        assert getAuditReader().createQuery().forEntitiesAtRevision(ParentEntity.class, 1).getSingleResult()
                .equals(parentVer1);
        assert getAuditReader().createQuery().forEntitiesAtRevision(ChildEntity.class, 1)
                .getResultList().size() == 0;
    }
}