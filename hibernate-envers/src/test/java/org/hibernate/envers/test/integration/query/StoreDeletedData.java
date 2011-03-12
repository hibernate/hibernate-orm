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

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * A test which checks if the data of a deleted entity is stored when the setting is on.
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class StoreDeletedData extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrIntTestEntity.class);
		cfg.setProperty("org.hibernate.envers.storeDataAtDelete", "true");
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);

        em.persist(site1);

        id1 = site1.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        em.remove(site1);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsPropertyEqQuery() {
        List revs_id1 = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditEntity.id().eq(id1))
                .getResultList();

        assert revs_id1.size() == 2;
        assert ((Object[]) revs_id1.get(0))[0].equals(new StrIntTestEntity("a", 10, id1));
        assert ((Object[]) revs_id1.get(1))[0].equals(new StrIntTestEntity("a", 10, id1));
    }
}