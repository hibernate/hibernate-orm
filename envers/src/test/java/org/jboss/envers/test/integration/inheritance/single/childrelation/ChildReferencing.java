package org.jboss.envers.test.integration.inheritance.single.childrelation;

import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildReferencing extends AbstractEntityTest {
    private Integer re_id1;
    private Integer re_id2;
    private Integer c_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildIngEntity.class);
        cfg.addAnnotatedClass(ParentNotIngEntity.class);
        cfg.addAnnotatedClass(ReferencedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Rev 1
        em.getTransaction().begin();

        ReferencedEntity re1 = new ReferencedEntity();
        em.persist(re1);
        re_id1 = re1.getId();

        ReferencedEntity re2 = new ReferencedEntity();
        em.persist(re2);
        re_id2 = re2.getId();

        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();

        re1 = em.find(ReferencedEntity.class, re_id1);

        ChildIngEntity cie = new ChildIngEntity("y", 1l);
        cie.setReferenced(re1);
        em.persist(cie);
        c_id = cie.getId();

        em.getTransaction().commit();

        // Rev 3
        em.getTransaction().begin();

        re2 = em.find(ReferencedEntity.class, re_id2);
        cie = em.find(ChildIngEntity.class, c_id);

        cie.setReferenced(re2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getVersionsReader().getRevisions(ReferencedEntity.class, re_id1));
        assert Arrays.asList(1, 3).equals(getVersionsReader().getRevisions(ReferencedEntity.class, re_id2));
        assert Arrays.asList(2, 3).equals(getVersionsReader().getRevisions(ChildIngEntity.class, c_id));
    }

    @Test
    public void testHistoryOfReferencedCollection1() {
        assert getVersionsReader().find(ReferencedEntity.class, re_id1, 1).getReferencing().size() == 0;
        assert getVersionsReader().find(ReferencedEntity.class, re_id1, 2).getReferencing().equals(
                TestTools.makeSet(new ChildIngEntity(c_id, "y", 1l)));
        assert getVersionsReader().find(ReferencedEntity.class, re_id1, 3).getReferencing().size() == 0;
    }

    @Test
    public void testHistoryOfReferencedCollection2() {
        assert getVersionsReader().find(ReferencedEntity.class, re_id2, 1).getReferencing().size() == 0;
        assert getVersionsReader().find(ReferencedEntity.class, re_id2, 2).getReferencing().size() == 0;
        assert getVersionsReader().find(ReferencedEntity.class, re_id2, 3).getReferencing().equals(
                TestTools.makeSet(new ChildIngEntity(c_id, "y", 1l)));
    }

    @Test
    public void testChildHistory() {
        assert getVersionsReader().find(ChildIngEntity.class, c_id, 1) == null;
        assert getVersionsReader().find(ChildIngEntity.class, c_id, 2).getReferenced().equals(
                new ReferencedEntity(re_id1));
        assert getVersionsReader().find(ChildIngEntity.class, c_id, 3).getReferenced().equals(
                new ReferencedEntity(re_id2));
    }
}