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
package org.hibernate.envers.test.integration.naming.ids;

import java.util.Arrays;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.Column;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinEmbIdNaming extends AbstractEntityTest {
    private EmbIdNaming ed_id1;
    private EmbIdNaming ed_id2;
    private EmbIdNaming ing_id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(JoinEmbIdNamingRefEdEntity.class);
        cfg.addAnnotatedClass(JoinEmbIdNamingRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        ed_id1 = new EmbIdNaming(10, 20);
        ed_id2 = new EmbIdNaming(11, 21);
        ing_id1 = new EmbIdNaming(12, 22);

        JoinEmbIdNamingRefEdEntity ed1 = new JoinEmbIdNamingRefEdEntity(ed_id1, "data1");
        JoinEmbIdNamingRefEdEntity ed2 = new JoinEmbIdNamingRefEdEntity(ed_id2, "data2");

        JoinEmbIdNamingRefIngEntity ing1 = new JoinEmbIdNamingRefIngEntity(ing_id1, "x", ed1);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);
        em.persist(ing1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ed2 = em.find(JoinEmbIdNamingRefEdEntity.class, ed2.getId());

        ing1 = em.find(JoinEmbIdNamingRefIngEntity.class, ing1.getId());
        ing1.setData("y");
        ing1.setReference(ed2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(JoinEmbIdNamingRefEdEntity.class, ed_id1));
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(JoinEmbIdNamingRefEdEntity.class, ed_id2));
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(JoinEmbIdNamingRefIngEntity.class, ing_id1));
    }

    @Test
    public void testHistoryOfEdId1() {
        JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity(ed_id1, "data1");

        assert getAuditReader().find(JoinEmbIdNamingRefEdEntity.class, ed_id1, 1).equals(ver1);
        assert getAuditReader().find(JoinEmbIdNamingRefEdEntity.class, ed_id1, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfEdId2() {
        JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity(ed_id2, "data2");

        assert getAuditReader().find(JoinEmbIdNamingRefEdEntity.class, ed_id2, 1).equals(ver1);
        assert getAuditReader().find(JoinEmbIdNamingRefEdEntity.class, ed_id2, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfIngId1() {
        JoinEmbIdNamingRefIngEntity ver1 = new JoinEmbIdNamingRefIngEntity(ing_id1, "x", null);
        JoinEmbIdNamingRefIngEntity ver2 = new JoinEmbIdNamingRefIngEntity(ing_id1, "y", null);

        assert getAuditReader().find(JoinEmbIdNamingRefIngEntity.class, ing_id1, 1).equals(ver1);
        assert getAuditReader().find(JoinEmbIdNamingRefIngEntity.class, ing_id1, 2).equals(ver2);

        assert getAuditReader().find(JoinEmbIdNamingRefIngEntity.class, ing_id1, 1).getReference().equals(
                new JoinEmbIdNamingRefEdEntity(ed_id1, "data1"));
        assert getAuditReader().find(JoinEmbIdNamingRefIngEntity.class, ing_id1, 2).getReference().equals(
                new JoinEmbIdNamingRefEdEntity(ed_id2, "data2"));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testJoinColumnNames() {
        Iterator<Column> columns =
                getCfg().getClassMapping("org.hibernate.envers.test.integration.naming.ids.JoinEmbIdNamingRefIngEntity_AUD")
                .getProperty("reference").getColumnIterator();

        boolean xxFound = false;
        boolean yyFound = false;
        while (columns.hasNext()) {
            if ("XX_reference".equals(columns.next().getName())) {
                xxFound = true;
            }

            if ("YY_reference".equals(columns.next().getName())) {
                yyFound = true;
            }
        }

        assert xxFound && yyFound;
    }
}