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

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DeletedEntities extends AbstractEntityTest {
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 11);

        em.persist(site1);
        em.persist(site2);

        id2 = site2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site2 = em.find(StrIntTestEntity.class, id2);
        em.remove(site2);

        em.getTransaction().commit();
    }

    @Test
    public void testProjectionsInEntitiesAtRevision() {
        assert getAuditReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 1)
            .getResultList().size() == 2;
        assert getAuditReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 2)
            .getResultList().size() == 1;

        assert (Long) getAuditReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 1)
            .addProjection(AuditEntity.id().count("id")).getResultList().get(0) == 2;
        assert (Long) getAuditReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 2)
            .addProjection(AuditEntity.id().count("id")).getResultList().get(0) == 1;
    }

    @Test
    public void testRevisionsOfEntityWithoutDelete() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, false)
                .add(AuditEntity.id().eq(id2))
                .getResultList();

        assert result.size() == 1;

        assert ((Object []) result.get(0))[0].equals(new StrIntTestEntity("b", 11, id2));
        assert ((DefaultRevisionEntity) ((Object []) result.get(0))[1]).getId() == 1;
        assert ((Object []) result.get(0))[2].equals(RevisionType.ADD);
    }
}
