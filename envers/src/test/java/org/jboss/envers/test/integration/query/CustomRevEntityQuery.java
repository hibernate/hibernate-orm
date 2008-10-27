package org.jboss.envers.test.integration.query;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrIntTestEntity;
import org.jboss.envers.test.entities.reventity.CustomRevEntity;
import org.jboss.envers.query.VersionsRestrictions;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class CustomRevEntityQuery extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(CustomRevEntity.class);
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 15);

        em.persist(site1);
        em.persist(site2);

        id1 = site1.getId();
        id2 = site2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);

        site1.setStr1("c");

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsOfId1Query() {
        List<Object[]> result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(VersionsRestrictions.idEq(id1))
                .getResultList();

        assert result.get(0)[0].equals(new StrIntTestEntity("a", 10, id1));
        assert result.get(0)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(0)[1]).getCustomId() == 1;

        assert result.get(1)[0].equals(new StrIntTestEntity("c", 10, id1));
        assert result.get(1)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(1)[1]).getCustomId() == 2;
    }

    @Test
    public void testRevisionsOfId2Query() {
        List<Object[]> result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(VersionsRestrictions.idEq(id2))
                .getResultList();

        assert result.get(0)[0].equals(new StrIntTestEntity("b", 15, id2));
        assert result.get(0)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(0)[1]).getCustomId() == 1;
    }
}