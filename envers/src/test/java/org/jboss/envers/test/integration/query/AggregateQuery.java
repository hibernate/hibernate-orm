package org.jboss.envers.test.integration.query;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.IntTestEntity;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class AggregateQuery extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        IntTestEntity ite1 = new IntTestEntity(2);
        IntTestEntity ite2 = new IntTestEntity(10);

        em.persist(ite1);
        em.persist(ite2);

        Integer id1 = ite1.getId();
        Integer id2 = ite2.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        IntTestEntity ite3 = new IntTestEntity(8);
        em.persist(ite3);

        ite1 = em.find(IntTestEntity.class, id1);

        ite1.setNumber(0);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        ite2 = em.find(IntTestEntity.class, id2);

        ite2.setNumber(52);

        em.getTransaction().commit();
    }

    @Test
    public void testEntitiesAvgMaxQuery() {
        Object[] ver1 = (Object[]) getVersionsReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 1)
                .addProjection("max", "number")
                .addProjection("avg", "number")
                .getSingleResult();

        Object[] ver2 = (Object[]) getVersionsReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 2)
                .addProjection("max", "number")
                .addProjection("avg", "number")
                .getSingleResult();

        Object[] ver3 = (Object[]) getVersionsReader().createQuery()
                .forEntitiesAtRevision(IntTestEntity.class, 3)
                .addProjection("max", "number")
                .addProjection("avg", "number")
                .getSingleResult();

        assert (Integer) ver1[0] == 10;
        assert (Double) ver1[1] == 6.0;

        assert (Integer) ver2[0] == 10;
        assert (Double) ver2[1] == 6.0;

        assert (Integer) ver3[0] == 52;
        assert (Double) ver3[1] == 20.0;
    }
}