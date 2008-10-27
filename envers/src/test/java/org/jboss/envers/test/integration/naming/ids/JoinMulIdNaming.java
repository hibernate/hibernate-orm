package org.jboss.envers.test.integration.naming.ids;

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
public class JoinMulIdNaming extends AbstractEntityTest {
    private MulIdNaming ed_id1;
    private MulIdNaming ed_id2;
    private MulIdNaming ing_id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(JoinMulIdNamingRefEdEntity.class);
        cfg.addAnnotatedClass(JoinMulIdNamingRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        ed_id1 = new MulIdNaming(10, 20);
        ed_id2 = new MulIdNaming(11, 21);
        ing_id1 = new MulIdNaming(12, 22);

        JoinMulIdNamingRefEdEntity ed1 = new JoinMulIdNamingRefEdEntity(ed_id1, "data1");
        JoinMulIdNamingRefEdEntity ed2 = new JoinMulIdNamingRefEdEntity(ed_id2, "data2");

        JoinMulIdNamingRefIngEntity ing1 = new JoinMulIdNamingRefIngEntity(ing_id1, "x", ed1);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ed1);
        em.persist(ed2);
        em.persist(ing1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ed2 = em.find(JoinMulIdNamingRefEdEntity.class, ed_id2);

        ing1 = em.find(JoinMulIdNamingRefIngEntity.class, ing_id1);
        ing1.setData("y");
        ing1.setReference(ed2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinMulIdNamingRefEdEntity.class, ed_id1));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinMulIdNamingRefEdEntity.class, ed_id2));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(JoinMulIdNamingRefIngEntity.class, ing_id1));
    }

    @Test
    public void testHistoryOfEdId1() {
        JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity(ed_id1, "data1");

        assert getVersionsReader().find(JoinMulIdNamingRefEdEntity.class, ed_id1, 1).equals(ver1);
        assert getVersionsReader().find(JoinMulIdNamingRefEdEntity.class, ed_id1, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfEdId2() {
        JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity(ed_id2, "data2");

        assert getVersionsReader().find(JoinMulIdNamingRefEdEntity.class, ed_id2, 1).equals(ver1);
        assert getVersionsReader().find(JoinMulIdNamingRefEdEntity.class, ed_id2, 2).equals(ver1);
    }

    @Test
    public void testHistoryOfIngId1() {
        JoinMulIdNamingRefIngEntity ver1 = new JoinMulIdNamingRefIngEntity(ing_id1, "x", null);
        JoinMulIdNamingRefIngEntity ver2 = new JoinMulIdNamingRefIngEntity(ing_id1, "y", null);

        assert getVersionsReader().find(JoinMulIdNamingRefIngEntity.class, ing_id1, 1).equals(ver1);
        assert getVersionsReader().find(JoinMulIdNamingRefIngEntity.class, ing_id1, 2).equals(ver2);

        assert getVersionsReader().find(JoinMulIdNamingRefIngEntity.class, ing_id1, 1).getReference().equals(
                new JoinMulIdNamingRefEdEntity(ed_id1, "data1"));
        assert getVersionsReader().find(JoinMulIdNamingRefIngEntity.class, ing_id1, 2).getReference().equals(
                new JoinMulIdNamingRefEdEntity(ed_id2, "data2"));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testJoinColumnNames() {
        Iterator<Column> columns =
                getCfg().getClassMapping("org.jboss.envers.test.integration.naming.ids.JoinMulIdNamingRefIngEntity_versions")
                .getProperty("reference").getColumnIterator();

        boolean id1Found = false;
        boolean id2Found = false;
        while (columns.hasNext()) {
            if ("ID1_reference".equals(columns.next().getName())) {
                id1Found = true;
            }

            if ("ID2_reference".equals(columns.next().getName())) {
                id2Found = true;
            }
        }

        assert id1Found && id2Found;
    }
}