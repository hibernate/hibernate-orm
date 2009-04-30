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
package org.hibernate.envers.test.integration.reventity;

import java.util.Arrays;
import java.util.Date;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.reventity.CustomDataRevEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CustomNoListener extends AbstractEntityTest {
    private Integer id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(CustomDataRevEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {        
        EntityManager em = getEntityManager();

		// Revision 1
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        id = te.getId();

		// Setting the data on the revision entity
		CustomDataRevEntity custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, false);
		custom.setData("data1");

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);
        te.setStr("y");

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, false);
		custom.setData("data2");

        em.getTransaction().commit();

		// Revision 3 - no changes, but rev entity should be persisted
        em.getTransaction().begin();

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, true);
		custom.setData("data3");

        em.getTransaction().commit();

		// No changes, rev entity won't be persisted
        em.getTransaction().begin();

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, false);
		custom.setData("data4");

        em.getTransaction().commit();

		// Revision 4
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);
        te.setStr("z");

		// Setting the data on the revision entity
		custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, false);
		custom.setData("data5");

		custom = getAuditReader().getCurrentRevision(CustomDataRevEntity.class, false);
		custom.setData("data5bis");

        em.getTransaction().commit();
    }

    @Test
    public void testFindRevision() {
        AuditReader vr = getAuditReader();

		assert "data1".equals(vr.findRevision(CustomDataRevEntity.class, 1).getData());
		assert "data2".equals(vr.findRevision(CustomDataRevEntity.class, 2).getData());
		assert "data3".equals(vr.findRevision(CustomDataRevEntity.class, 3).getData());
		assert "data5bis".equals(vr.findRevision(CustomDataRevEntity.class, 4).getData());
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 4).equals(getAuditReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);
        StrTestEntity ver3 = new StrTestEntity("z", id);

        assert getAuditReader().find(StrTestEntity.class, id, 1).equals(ver1);
        assert getAuditReader().find(StrTestEntity.class, id, 2).equals(ver2);
        assert getAuditReader().find(StrTestEntity.class, id, 3).equals(ver2);
        assert getAuditReader().find(StrTestEntity.class, id, 4).equals(ver3);
    }
}