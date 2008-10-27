package org.jboss.envers.test.integration.collection.mapkey;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.jboss.envers.test.entities.components.ComponentTestEntity;
import org.jboss.envers.test.entities.components.Component1;
import org.jboss.envers.test.entities.components.Component2;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentMapKey extends AbstractEntityTest {
    private Integer cmke_id;

    private Integer cte1_id;
    private Integer cte2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ComponentMapKeyEntity.class);
        cfg.addAnnotatedClass(ComponentTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        ComponentMapKeyEntity imke = new ComponentMapKeyEntity();

        // Revision 1 (intialy 1 mapping)
        em.getTransaction().begin();

        ComponentTestEntity cte1 = new ComponentTestEntity(new Component1("x1", "y2"), new Component2("a1", "b2"));
        ComponentTestEntity cte2 = new ComponentTestEntity(new Component1("x1", "y2"), new Component2("a1", "b2"));

        em.persist(cte1);
        em.persist(cte2);

        imke.getIdmap().put(cte1.getComp1(), cte1);

        em.persist(imke);

        em.getTransaction().commit();

        // Revision 2 (sse1: adding 1 mapping)
        em.getTransaction().begin();

        cte2 = em.find(ComponentTestEntity.class, cte2.getId());
        imke = em.find(ComponentMapKeyEntity.class, imke.getId());

        imke.getIdmap().put(cte2.getComp1(), cte2);

        em.getTransaction().commit();

        //

        cmke_id = imke.getId();

        cte1_id = cte1.getId();
        cte2_id = cte2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(ComponentMapKeyEntity.class, cmke_id));
    }

    @Test
    public void testHistoryOfImke() {
        ComponentTestEntity cte1 = getEntityManager().find(ComponentTestEntity.class, cte1_id);
        ComponentTestEntity cte2 = getEntityManager().find(ComponentTestEntity.class, cte2_id);

        // These fields are unversioned.
        cte1.setComp2(null);
        cte2.setComp2(null);

        ComponentMapKeyEntity rev1 = getVersionsReader().find(ComponentMapKeyEntity.class, cmke_id, 1);
        ComponentMapKeyEntity rev2 = getVersionsReader().find(ComponentMapKeyEntity.class, cmke_id, 2);

        assert rev1.getIdmap().equals(TestTools.makeMap(cte1.getComp1(), cte1));
        assert rev2.getIdmap().equals(TestTools.makeMap(cte1.getComp1(), cte1, cte2.getComp1(), cte2));
    }
}