package org.jboss.envers.test.integration.query;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrIntTestEntity;
import org.jboss.envers.query.VersionsRestrictions;
import org.jboss.envers.RevisionType;
import org.jboss.envers.DefaultRevisionEntity;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DeletedEntities extends AbstractEntityTest {
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 11);

        em.persist(site1);
        em.persist(site2);

        id2 = site2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site2 = em.find(StrIntTestEntity.class, id2);
        em.remove(site2);

        em.getTransaction().commit();
    }

    @Test
    public void testProjectionsInEntitiesAtRevision() {
        assert getVersionsReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 1)
            .getResultList().size() == 2;
        assert getVersionsReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 2)
            .getResultList().size() == 1;

        assert (Long) getVersionsReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 1)
            .addProjection("count", "originalId.id").getResultList().get(0) == 2;
        assert (Long) getVersionsReader().createQuery().forEntitiesAtRevision(StrIntTestEntity.class, 2)
            .addProjection("count", "originalId.id").getResultList().get(0) == 1;
    }

    @Test
    public void testRevisionsOfEntityWithoutDelete() {
        List result = getVersionsReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, false)
                .add(VersionsRestrictions.idEq(id2))
                .getResultList();

        assert result.size() == 1;

        assert ((Object []) result.get(0))[0].equals(new StrIntTestEntity("b", 11, id2));
        assert ((DefaultRevisionEntity) ((Object []) result.get(0))[1]).getId() == 1;
        assert ((Object []) result.get(0))[2].equals(RevisionType.ADD);
    }
}
