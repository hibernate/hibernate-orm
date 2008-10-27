package org.jboss.envers.test.integration.collection;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.jboss.envers.test.entities.collection.StringListEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringList extends AbstractEntityTest {
    private Integer sle1_id;
    private Integer sle2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StringListEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        StringListEntity sle1 = new StringListEntity();
        StringListEntity sle2 = new StringListEntity();

        // Revision 1 (sle1: initialy empty, sle2: initialy 2 elements)
        em.getTransaction().begin();

        sle2.getStrings().add("sle2_string1");
        sle2.getStrings().add("sle2_string2");

        em.persist(sle1);
        em.persist(sle2);

        em.getTransaction().commit();

        // Revision 2 (sle1: adding 2 elements, sle2: adding an existing element)
        em.getTransaction().begin();

        sle1 = em.find(StringListEntity.class, sle1.getId());
        sle2 = em.find(StringListEntity.class, sle2.getId());

        sle1.getStrings().add("sle1_string1");
        sle1.getStrings().add("sle1_string2");

        sle2.getStrings().add("sle2_string1");

        em.getTransaction().commit();

        // Revision 3 (sle1: replacing an element at index 0, sle2: removing an element at index 0)
        em.getTransaction().begin();

        sle1 = em.find(StringListEntity.class, sle1.getId());
        sle2 = em.find(StringListEntity.class, sle2.getId());

        sle1.getStrings().set(0, "sle1_string3");

        sle2.getStrings().remove(0);

        em.getTransaction().commit();

        //

        sle1_id = sle1.getId();
        sle2_id = sle2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getVersionsReader().getRevisions(StringListEntity.class, sle1_id));
        assert Arrays.asList(1, 2, 3).equals(getVersionsReader().getRevisions(StringListEntity.class, sle2_id));
    }

    @Test
    public void testHistoryOfSle1() {
        StringListEntity rev1 = getVersionsReader().find(StringListEntity.class, sle1_id, 1);
        StringListEntity rev2 = getVersionsReader().find(StringListEntity.class, sle1_id, 2);
        StringListEntity rev3 = getVersionsReader().find(StringListEntity.class, sle1_id, 3);

        assert rev1.getStrings().equals(Collections.EMPTY_LIST);
        assert rev2.getStrings().equals(TestTools.makeList("sle1_string1", "sle1_string2"));
        assert rev3.getStrings().equals(TestTools.makeList("sle1_string3", "sle1_string2"));
    }

    @Test
    public void testHistoryOfSse2() {
        StringListEntity rev1 = getVersionsReader().find(StringListEntity.class, sle2_id, 1);
        StringListEntity rev2 = getVersionsReader().find(StringListEntity.class, sle2_id, 2);
        StringListEntity rev3 = getVersionsReader().find(StringListEntity.class, sle2_id, 3);

        assert rev1.getStrings().equals(TestTools.makeList("sle2_string1", "sle2_string2"));
        assert rev2.getStrings().equals(TestTools.makeList("sle2_string1", "sle2_string2", "sle2_string1"));
        assert rev3.getStrings().equals(TestTools.makeList("sle2_string2", "sle2_string1"));
    }
}