package org.jboss.envers.test.integration.data;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Lobs extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(LobTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        LobTestEntity lte = new LobTestEntity("abc", new byte[] { 0, 1, 2 }, new char[] { 'x', 'y', 'z' });
        em.persist(lte);
        id1 = lte.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        lte = em.find(LobTestEntity.class, id1);
        lte.setStringLob("def");
        lte.setByteLob(new byte[] { 3, 4, 5 });
        lte.setCharLob(new char[] { 'h', 'i', 'j' });
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(LobTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        LobTestEntity ver1 = new LobTestEntity(id1, "abc", new byte[] { 0, 1, 2 }, new char[] { 'x', 'y', 'z' });
        LobTestEntity ver2 = new LobTestEntity(id1, "def", new byte[] { 3, 4, 5 }, new char[] { 'h', 'i', 'j' });

        assert getVersionsReader().find(LobTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(LobTestEntity.class, id1, 2).equals(ver2);
    }
}