package org.hibernate.envers.test.integration.proxy;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.ListRefIngEntity;
import org.hibernate.proxy.HibernateProxy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test case for HHH-5750: Proxied objects lose the temporary session used to
 * initialize them.
 * 
 * @author Erik-Berndt Scheper
 * 
 */
public class AuditedCollectionProxyTest extends AbstractEntityTest {

    Integer id_ListRefEdEntity1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ListRefEdEntity.class);
        cfg.addAnnotatedClass(ListRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        ListRefEdEntity listReferencedEntity1 = new ListRefEdEntity(
                Integer.valueOf(1), "str1");
        ListRefIngEntity refingEntity1 = new ListRefIngEntity(
                Integer.valueOf(1), "refing1", listReferencedEntity1);

        // Revision 1
        em.getTransaction().begin();
        em.persist(listReferencedEntity1);
        em.persist(refingEntity1);
        em.getTransaction().commit();

        id_ListRefEdEntity1 = listReferencedEntity1.getId();

        // Revision 2
        ListRefIngEntity refingEntity2 = new ListRefIngEntity(
                Integer.valueOf(2), "refing2", listReferencedEntity1);

        em.getTransaction().begin();
        em.persist(refingEntity2);
        em.getTransaction().commit();
    }

    @Test
    public void testProxyIdentifier() {
        EntityManager em = getEntityManager();

        ListRefEdEntity listReferencedEntity1 = em.getReference(
                ListRefEdEntity.class, id_ListRefEdEntity1);

        assert listReferencedEntity1 instanceof HibernateProxy;

        // Revision 3
        ListRefIngEntity refingEntity3 = new ListRefIngEntity(
                Integer.valueOf(3), "refing3", listReferencedEntity1);

        em.getTransaction().begin();
        em.persist(refingEntity3);
        em.getTransaction().commit();

        listReferencedEntity1.getReffering().size();

    }

}
