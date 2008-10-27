package org.jboss.envers.test.integration.cache;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.IntTestEntity;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
public class QueryCache extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        IntTestEntity ite = new IntTestEntity(10);
        em.persist(ite);
        id1 = ite.getId();
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        ite = em.find(IntTestEntity.class, id1);
        ite.setNumber(20);
        em.getTransaction().commit();
    }

    @Test
    public void testCacheFindAfterRevisionsOfEntityQuery() {
        List entsFromQuery = getVersionsReader().createQuery()
                .forRevisionsOfEntity(IntTestEntity.class, true, false)
                .getResultList();

        IntTestEntity entFromFindRev1 = getVersionsReader().find(IntTestEntity.class, id1, 1);
        IntTestEntity entFromFindRev2 = getVersionsReader().find(IntTestEntity.class, id1, 2);

        assert entFromFindRev1 == entsFromQuery.get(0);
        assert entFromFindRev2 == entsFromQuery.get(1);
    }

    @Test
    public void testCacheFindAfterEntitiesAtRevisionQuery() {
        IntTestEntity entFromQuery = (IntTestEntity) getVersionsReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 1)
                .getSingleResult();

        IntTestEntity entFromFind = getVersionsReader().find(IntTestEntity.class, id1, 1);

        assert entFromFind == entFromQuery;
    }
}