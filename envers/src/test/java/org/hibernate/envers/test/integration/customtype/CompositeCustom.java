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
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.customtype.Component;
import org.hibernate.envers.test.entities.customtype.CompositeCustomTypeEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeCustom extends AbstractEntityTest {
    private Integer ccte_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(CompositeCustomTypeEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        CompositeCustomTypeEntity ccte = new CompositeCustomTypeEntity();

        // Revision 1 (persisting 1 entity)
        em.getTransaction().begin();

        ccte.setComponent(new Component("a", 1));

        em.persist(ccte);

        em.getTransaction().commit();

        // Revision 2 (changing the component)
        em.getTransaction().begin();

        ccte = em.find(CompositeCustomTypeEntity.class, ccte.getId());

        ccte.getComponent().setProp1("b");

        em.getTransaction().commit();

        // Revision 3 (replacing the component)
        em.getTransaction().begin();

        ccte = em.find(CompositeCustomTypeEntity.class, ccte.getId());

        ccte.setComponent(new Component("c", 3));

        em.getTransaction().commit();

        //

        ccte_id = ccte.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(CompositeCustomTypeEntity.class, ccte_id));
    }

    @Test
    public void testHistoryOfCcte() {
        CompositeCustomTypeEntity rev1 = getAuditReader().find(CompositeCustomTypeEntity.class, ccte_id, 1);
        CompositeCustomTypeEntity rev2 = getAuditReader().find(CompositeCustomTypeEntity.class, ccte_id, 2);
        CompositeCustomTypeEntity rev3 = getAuditReader().find(CompositeCustomTypeEntity.class, ccte_id, 3);

        assert rev1.getComponent().equals(new Component("a", 1));
        assert rev2.getComponent().equals(new Component("b", 1));
        assert rev3.getComponent().equals(new Component("c", 3));
    }
}