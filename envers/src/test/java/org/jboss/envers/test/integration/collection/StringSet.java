package org.jboss.envers.test.integration.collection;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.jboss.envers.test.entities.collection.StringSetEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringSet extends AbstractEntityTest {
    private Integer sse1_id;
    private Integer sse2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StringSetEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        StringSetEntity sse1 = new StringSetEntity();
        StringSetEntity sse2 = new StringSetEntity();

        // Revision 1 (sse1: initialy empty, sse2: initialy 2 elements)
        em.getTransaction().begin();

        sse2.getStrings().add("sse2_string1");
        sse2.getStrings().add("sse2_string2");

        em.persist(sse1);
        em.persist(sse2);

        em.getTransaction().commit();

        // Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
        em.getTransaction().begin();

        sse1 = em.find(StringSetEntity.class, sse1.getId());
        sse2 = em.find(StringSetEntity.class, sse2.getId());

        sse1.getStrings().add("sse1_string1");
        sse1.getStrings().add("sse1_string2");

        sse2.getStrings().add("sse2_string1");

        em.getTransaction().commit();

        // Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
        em.getTransaction().begin();

        sse1 = em.find(StringSetEntity.class, sse1.getId());
        sse2 = em.find(StringSetEntity.class, sse2.getId());

        sse1.getStrings().remove("sse1_string3");
        sse2.getStrings().remove("sse2_string1");

        em.getTransaction().commit();

        //

        sse1_id = sse1.getId();
        sse2_id = sse2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(StringSetEntity.class, sse1_id));
        assert Arrays.asList(1, 3).equals(getVersionsReader().getRevisions(StringSetEntity.class, sse2_id));
    }

    @Test
    public void testHistoryOfSse1() {
        StringSetEntity rev1 = getVersionsReader().find(StringSetEntity.class, sse1_id, 1);
        StringSetEntity rev2 = getVersionsReader().find(StringSetEntity.class, sse1_id, 2);
        StringSetEntity rev3 = getVersionsReader().find(StringSetEntity.class, sse1_id, 3);

        assert rev1.getStrings().equals(Collections.EMPTY_SET);
        assert rev2.getStrings().equals(TestTools.makeSet("sse1_string1", "sse1_string2"));
        assert rev3.getStrings().equals(TestTools.makeSet("sse1_string1", "sse1_string2"));
    }

    @Test
    public void testHistoryOfSse2() {
        StringSetEntity rev1 = getVersionsReader().find(StringSetEntity.class, sse2_id, 1);
        StringSetEntity rev2 = getVersionsReader().find(StringSetEntity.class, sse2_id, 2);
        StringSetEntity rev3 = getVersionsReader().find(StringSetEntity.class, sse2_id, 3);

        assert rev1.getStrings().equals(TestTools.makeSet("sse2_string1", "sse2_string2"));
        assert rev2.getStrings().equals(TestTools.makeSet("sse2_string1", "sse2_string2"));
        assert rev3.getStrings().equals(TestTools.makeSet("sse2_string2"));
    }
}