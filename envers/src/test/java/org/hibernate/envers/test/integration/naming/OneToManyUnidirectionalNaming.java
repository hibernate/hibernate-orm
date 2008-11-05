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
package org.hibernate.envers.test.integration.naming;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.Column;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class OneToManyUnidirectionalNaming extends AbstractEntityTest {
    private Integer uni1_id;
    private Integer str1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(DetachedNamingTestEntity.class);
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        DetachedNamingTestEntity uni1 = new DetachedNamingTestEntity(1, "data1");
        StrTestEntity str1 = new StrTestEntity("str1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        uni1.setCollection(new HashSet<StrTestEntity>());
        em.persist(uni1);
        em.persist(str1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        uni1 = em.find(DetachedNamingTestEntity.class, uni1.getId());
        str1 = em.find(StrTestEntity.class, str1.getId());
        uni1.getCollection().add(str1);

        em.getTransaction().commit();

        //

        uni1_id = uni1.getId();
        str1_id = str1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(DetachedNamingTestEntity.class, uni1_id));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(StrTestEntity.class, str1_id));
    }

    @Test
    public void testHistoryOfUniId1() {
        StrTestEntity str1 = getEntityManager().find(StrTestEntity.class, str1_id);

        DetachedNamingTestEntity rev1 = getAuditReader().find(DetachedNamingTestEntity.class, uni1_id, 1);
        DetachedNamingTestEntity rev2 = getAuditReader().find(DetachedNamingTestEntity.class, uni1_id, 2);

        assert rev1.getCollection().equals(TestTools.makeSet());
        assert rev2.getCollection().equals(TestTools.makeSet(str1));

        assert "data1".equals(rev1.getData());
        assert "data1".equals(rev2.getData());
    }

    private final static String MIDDLE_VERSIONS_ENTITY_NAME = "UNI_NAMING_TEST_AUD";
    @Test
    public void testTableName() {
        assert MIDDLE_VERSIONS_ENTITY_NAME.equals(
                getCfg().getClassMapping(MIDDLE_VERSIONS_ENTITY_NAME).getTable().getName());
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testJoinColumnName() {
        Iterator<Column> columns =
                getCfg().getClassMapping(MIDDLE_VERSIONS_ENTITY_NAME).getTable().getColumnIterator();

        boolean id1Found = false;
        boolean id2Found = false;

        while (columns.hasNext()) {
            Column column = columns.next();
            if ("ID_1".equals(column.getName())) {
                id1Found = true;
            }

            if ("ID_2".equals(column.getName())) {
                id2Found = true;
            }
        }

        assert id1Found && id2Found;
    }
}
