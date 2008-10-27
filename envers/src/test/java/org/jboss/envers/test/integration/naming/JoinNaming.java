package org.jboss.envers.test.integration.naming;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.Column;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinNaming extends AbstractEntityTest {
    private Integer ed_id1;
    private Integer ed_id2;
    private Integer ing_id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(JoinNamingRefEdEntity.class);
        cfg.addAnnotatedClass(JoinNamingRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        JoinNamingRefEdEntity ed1 = new JoinNamingRefEdEntity("data1");
        JoinNamingRefEdEntity ed2 = new JoinNamingRefEdEntity("data2");

        JoinNamingRefIngEntity ing1 = new JoinNamingRefIngEntity("x", ed1);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);
        em.persist(ing1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ed2 = em.find(JoinNamingRefEdEntity.class, ed2.getId());

        ing1 = em.find(JoinNamingRefIngEntity.class, ing1.getId());
        ing1.setData("y");
        ing1.setReference(ed2);

        em.getTransaction().commit();

        //

        ed_id1 = ed1.getId();
        ed_id2 = ed2.getId();
        ing_id1 = ing1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinNamingRefEdEntity.class, ed_id1));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinNamingRefEdEntity.class, ed_id2));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinNamingRefIngEntity.class, ing_id1));
    }

    @Test
    public void testHistoryOfEdId1() {
        JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity(ed_id1, "data1");

        assert getVersionsReader().find(JoinNamingRefEdEntity.class, ed_id1, 1).equals(ver1);
        assert getVersionsReader().find(JoinNamingRefEdEntity.class, ed_id1, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfEdId2() {
        JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity(ed_id2, "data2");

        assert getVersionsReader().find(JoinNamingRefEdEntity.class, ed_id2, 1).equals(ver1);
        assert getVersionsReader().find(JoinNamingRefEdEntity.class, ed_id2, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfIngId1() {
        JoinNamingRefIngEntity ver1 = new JoinNamingRefIngEntity(ing_id1, "x", null);
        JoinNamingRefIngEntity ver2 = new JoinNamingRefIngEntity(ing_id1, "y", null);

        assert getVersionsReader().find(JoinNamingRefIngEntity.class, ing_id1, 1).equals(ver1);
        assert getVersionsReader().find(JoinNamingRefIngEntity.class, ing_id1, 2).equals(ver2);

        assert getVersionsReader().find(JoinNamingRefIngEntity.class, ing_id1, 1).getReference().equals(
                new JoinNamingRefEdEntity(ed_id1, "data1"));
        assert getVersionsReader().find(JoinNamingRefIngEntity.class, ing_id1, 2).getReference().equals(
                new JoinNamingRefEdEntity(ed_id2, "data2"));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testJoinColumnName() {
        Iterator<Column> columns =
                getCfg().getClassMapping("org.jboss.envers.test.integration.naming.JoinNamingRefIngEntity_versions")
                .getProperty("reference").getColumnIterator();

        while (columns.hasNext()) {
            if ("jnree_column_reference".equals(columns.next().getName())) {
                return;
            }
        }

        assert false;
    }
}