package org.jboss.envers.test.integration.customtype;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.customtype.ParametrizedCustomTypeEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParametrizedCustom extends AbstractEntityTest {
    private Integer pcte_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ParametrizedCustomTypeEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        ParametrizedCustomTypeEntity pcte = new ParametrizedCustomTypeEntity();

        // Revision 1 (persisting 1 entity)
        em.getTransaction().begin();

        pcte.setStr("U");

        em.persist(pcte);

        em.getTransaction().commit();

        // Revision 2 (changing the value)
        em.getTransaction().begin();

        pcte = em.find(ParametrizedCustomTypeEntity.class, pcte.getId());

        pcte.setStr("V");

        em.getTransaction().commit();

        //

        pcte_id = pcte.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(ParametrizedCustomTypeEntity.class, pcte_id));
    }

    @Test
    public void testHistoryOfCcte() {
        ParametrizedCustomTypeEntity rev1 = getVersionsReader().find(ParametrizedCustomTypeEntity.class, pcte_id, 1);
        ParametrizedCustomTypeEntity rev2 = getVersionsReader().find(ParametrizedCustomTypeEntity.class, pcte_id, 2);

        assert "xUy".equals(rev1.getStr());
        assert "xVy".equals(rev2.getStr());
    }
}