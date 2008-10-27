package org.jboss.envers.test.integration.cache;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.integration.onetoone.bidirectional.BiRefEdEntity;
import org.jboss.envers.test.integration.onetoone.bidirectional.BiRefIngEntity;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
public class OneToOneCache extends AbstractEntityTest {
    private Integer ed1_id;
    private Integer ed2_id;

    private Integer ing1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BiRefEdEntity.class);
        cfg.addAnnotatedClass(BiRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        BiRefEdEntity ed1 = new BiRefEdEntity(1, "data_ed_1");
        BiRefEdEntity ed2 = new BiRefEdEntity(2, "data_ed_2");

        BiRefIngEntity ing1 = new BiRefIngEntity(3, "data_ing_1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        ing1.setReference(ed1);

        em.persist(ed1);
        em.persist(ed2);

        em.persist(ing1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ing1 = em.find(BiRefIngEntity.class, ing1.getId());
        ed2 = em.find(BiRefEdEntity.class, ed2.getId());

        ing1.setReference(ed2);

        em.getTransaction().commit();

        //

        ed1_id = ed1.getId();
        ed2_id = ed2.getId();

        ing1_id = ing1.getId();
    }

    @Test
    public void testCacheReferenceAccessAfterFindRev1() {
        BiRefEdEntity ed1_rev1 = getVersionsReader().find(BiRefEdEntity.class, ed1_id, 1);
        BiRefIngEntity ing1_rev1 = getVersionsReader().find(BiRefIngEntity.class, ing1_id, 1);

        assert ing1_rev1.getReference() == ed1_rev1;
    }

    @Test
    public void testCacheReferenceAccessAfterFindRev2() {
        BiRefEdEntity ed2_rev2 = getVersionsReader().find(BiRefEdEntity.class, ed2_id, 2);
        BiRefIngEntity ing1_rev2 = getVersionsReader().find(BiRefIngEntity.class, ing1_id, 2);

        assert ing1_rev2.getReference() == ed2_rev2;
    }
}