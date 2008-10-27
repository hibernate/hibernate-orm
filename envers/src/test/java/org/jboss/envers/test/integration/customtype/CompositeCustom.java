package org.jboss.envers.test.integration.customtype;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.customtype.CompositeCustomTypeEntity;
import org.jboss.envers.test.entities.customtype.Component;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeCustom extends AbstractEntityTest {
    private Integer ccte_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(CompositeCustomTypeEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        CompositeCustomTypeEntity ccte = new CompositeCustomTypeEntity();

        // Revision 1 (persisting 1 entity)
        em.getTransaction().begin();

        ccte.setComponent(new Component("a", 1));

        em.persist(ccte);

        em.getTransaction().commit();

        // Revision 2 (changing the component)
        em.getTransaction().begin();

        ccte = em.find(CompositeCustomTypeEntity.class, ccte.getId());

        ccte.getComponent().setProp1("b");

        em.getTransaction().commit();

        // Revision 3 (replacing the component)
        em.getTransaction().begin();

        ccte = em.find(CompositeCustomTypeEntity.class, ccte.getId());

        ccte.setComponent(new Component("c", 3));

        em.getTransaction().commit();

        //

        ccte_id = ccte.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getVersionsReader().getRevisions(CompositeCustomTypeEntity.class, ccte_id));
    }

    @Test
    public void testHistoryOfCcte() {
        CompositeCustomTypeEntity rev1 = getVersionsReader().find(CompositeCustomTypeEntity.class, ccte_id, 1);
        CompositeCustomTypeEntity rev2 = getVersionsReader().find(CompositeCustomTypeEntity.class, ccte_id, 2);
        CompositeCustomTypeEntity rev3 = getVersionsReader().find(CompositeCustomTypeEntity.class, ccte_id, 3);

        assert rev1.getComponent().equals(new Component("a", 1));
        assert rev2.getComponent().equals(new Component("b", 1));
        assert rev3.getComponent().equals(new Component("c", 3));
    }
}