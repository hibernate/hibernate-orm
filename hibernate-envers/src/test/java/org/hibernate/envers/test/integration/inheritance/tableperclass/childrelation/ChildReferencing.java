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

package org.hibernate.envers.test.integration.inheritance.tableperclass.childrelation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildReferencing extends AbstractEntityTest {
    private Integer re_id1;
    private Integer re_id2;
    private Integer c_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildIngEntity.class);
        cfg.addAnnotatedClass(ParentNotIngEntity.class);
        cfg.addAnnotatedClass(ReferencedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        re_id1 = 1;
        re_id2 = 10;
        c_id = 100;

        // Rev 1
        em.getTransaction().begin();

        ReferencedEntity re1 = new ReferencedEntity(re_id1);
        em.persist(re1);

        ReferencedEntity re2 = new ReferencedEntity(re_id2);
        em.persist(re2);

        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();

        re1 = em.find(ReferencedEntity.class, re_id1);

        ChildIngEntity cie = new ChildIngEntity(c_id, "y", 1l);
        cie.setReferenced(re1);
        em.persist(cie);
        c_id = cie.getId();

        em.getTransaction().commit();

        // Rev 3
        em.getTransaction().begin();

        re2 = em.find(ReferencedEntity.class, re_id2);
        cie = em.find(ChildIngEntity.class, c_id);

        cie.setReferenced(re2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(ReferencedEntity.class, re_id1));
        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(ReferencedEntity.class, re_id2));
        assert Arrays.asList(2, 3).equals(getAuditReader().getRevisions(ChildIngEntity.class, c_id));
    }

    @Test
    public void testHistoryOfReferencedCollection1() {
        assert getAuditReader().find(ReferencedEntity.class, re_id1, 1).getReferencing().size() == 0;
        assert getAuditReader().find(ReferencedEntity.class, re_id1, 2).getReferencing().equals(
                TestTools.makeSet(new ChildIngEntity(c_id, "y", 1l)));
        assert getAuditReader().find(ReferencedEntity.class, re_id1, 3).getReferencing().size() == 0;
    }

    @Test
    public void testHistoryOfReferencedCollection2() {
        assert getAuditReader().find(ReferencedEntity.class, re_id2, 1).getReferencing().size() == 0;
        assert getAuditReader().find(ReferencedEntity.class, re_id2, 2).getReferencing().size() == 0;
        assert getAuditReader().find(ReferencedEntity.class, re_id2, 3).getReferencing().equals(
                TestTools.makeSet(new ChildIngEntity(c_id, "y", 1l)));
    }

    @Test
    public void testChildHistory() {
        assert getAuditReader().find(ChildIngEntity.class, c_id, 1) == null;
        assert getAuditReader().find(ChildIngEntity.class, c_id, 2).getReferenced().equals(
                new ReferencedEntity(re_id1));
        assert getAuditReader().find(ChildIngEntity.class, c_id, 3).getReferenced().equals(
                new ReferencedEntity(re_id2));
    }
}