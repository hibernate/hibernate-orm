package org.jboss.envers.test.integration.basic;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.exception.RevisionDoesNotExistException;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SingleOperationInTransaction extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;
    private Integer id3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity1.class);
    }

    private Integer addNewEntity(String str, long lng) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity1 bte1 = new BasicTestEntity1(str, lng);
        em.persist(bte1);
        em.getTransaction().commit();

        return bte1.getId();
    }

    private void modifyEntity(Integer id, String str, long lng) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity1 bte1 = em.find(BasicTestEntity1.class, id);
        bte1.setLong1(lng);
        bte1.setStr1(str);
        em.getTransaction().commit();
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = addNewEntity("x", 1); // rev 1
        id2 = addNewEntity("y", 20); // rev 2
        id3 = addNewEntity("z", 30); // rev 3

        modifyEntity(id1, "x2", 2); // rev 4
        modifyEntity(id2, "y2", 20); // rev 5
        modifyEntity(id1, "x3", 3); // rev 6
        modifyEntity(id1, "x3", 3); // no rev
        modifyEntity(id2, "y3", 21); // rev 7
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 4, 6).equals(getVersionsReader().getRevisions(BasicTestEntity1.class, id1));

        assert Arrays.asList(2, 5, 7).equals(getVersionsReader().getRevisions(BasicTestEntity1.class, id2));

        assert Arrays.asList(3).equals(getVersionsReader().getRevisions(BasicTestEntity1.class, id3));
    }

    @Test
    public void testRevisionsDates() {
        for (int i=1; i<7; i++) {
            assert getVersionsReader().getRevisionDate(i).getTime() <=
                    getVersionsReader().getRevisionDate(i+1).getTime();
        }
    }

    @Test(expectedExceptions = RevisionDoesNotExistException.class)
    public void testNotExistingRevision() {
        getVersionsReader().getRevisionDate(8);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalRevision() {
        getVersionsReader().getRevisionDate(0);
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id1, "x", 1);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id1, "x2", 2);
        BasicTestEntity1 ver3 = new BasicTestEntity1(id1, "x3", 3);

        assert getVersionsReader().find(BasicTestEntity1.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 2).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 3).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 4).equals(ver2);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 5).equals(ver2);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 6).equals(ver3);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 7).equals(ver3);
    }

    @Test
    public void testHistoryOfId2() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id2, "y", 20);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id2, "y2", 20);
        BasicTestEntity1 ver3 = new BasicTestEntity1(id2, "y3", 21);

        assert getVersionsReader().find(BasicTestEntity1.class, id2, 1) == null;
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 2).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 3).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 4).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 5).equals(ver2);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 6).equals(ver2);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 7).equals(ver3);
    }

    @Test
    public void testHistoryOfId3() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id3, "z", 30);

        assert getVersionsReader().find(BasicTestEntity1.class, id3, 1) == null;
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 2) == null;
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 3).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 4).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 5).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 6).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id3, 7).equals(ver1);
    }

    @Test
    public void testHistoryOfNotExistingEntity() {
        assert getVersionsReader().find(BasicTestEntity1.class, id1+id2+id3, 1) == null;
        assert getVersionsReader().find(BasicTestEntity1.class, id1+id2+id3, 7) == null;
    }

    @Test
    public void testRevisionsOfNotExistingEntity() {
        assert getVersionsReader().getRevisions(BasicTestEntity1.class, id1+id2+id3).size() == 0;
    }
}
