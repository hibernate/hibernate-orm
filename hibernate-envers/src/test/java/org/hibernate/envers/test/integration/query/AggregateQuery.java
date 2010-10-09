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

import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.hibernate.envers.query.AuditEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;
/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class AggregateQuery extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        IntTestEntity ite1 = new IntTestEntity(2);
        IntTestEntity ite2 = new IntTestEntity(10);

        em.persist(ite1);
        em.persist(ite2);

        Integer id1 = ite1.getId();
        Integer id2 = ite2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        IntTestEntity ite3 = new IntTestEntity(8);
        em.persist(ite3);

        ite1 = em.find(IntTestEntity.class, id1);

        ite1.setNumber(0);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        ite2 = em.find(IntTestEntity.class, id2);

        ite2.setNumber(52);

        em.getTransaction().commit();
    }

    @Test
    public void testEntitiesAvgMaxQuery() {
        Object[] ver1 = (Object[]) getAuditReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 1)
                .addProjection(AuditEntity.property("number").max())
                .addProjection(AuditEntity.property("number").function("avg"))
                .getSingleResult();

        Object[] ver2 = (Object[]) getAuditReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 2)
                .addProjection(AuditEntity.property("number").max())
                .addProjection(AuditEntity.property("number").function("avg"))
                .getSingleResult();

        Object[] ver3 = (Object[]) getAuditReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 3)
                .addProjection(AuditEntity.property("number").max())
                .addProjection(AuditEntity.property("number").function("avg"))
                .getSingleResult();

        assert (Integer) ver1[0] == 10;
        assert (Double) ver1[1] == 6.0;

        assert (Integer) ver2[0] == 10;
        assert (Double) ver2[1] == 6.0;

        assert (Integer) ver3[0] == 52;
        assert (Double) ver3[1] == 20.0;
    }
}