package org.hibernate.envers.test.integration.notinsertable.manytoone;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ManyToOneNotInsertable extends AbstractEntityTest {
    private Integer mto_id1;
    private Integer type_id1;
    private Integer type_id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ManyToOneNotInsertableEntity.class);
        cfg.addAnnotatedClass(NotInsertableEntityType.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        mto_id1 = 1;
        type_id1 = 2;
        type_id2 = 3;

		// Rev 1
		// Preparing the types
		em.getTransaction().begin();

		NotInsertableEntityType type1 = new NotInsertableEntityType(type_id1, "type1");
		NotInsertableEntityType type2 = new NotInsertableEntityType(type_id2, "type2");

		em.persist(type1);
		em.persist(type2);

		em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();

        ManyToOneNotInsertableEntity master = new ManyToOneNotInsertableEntity(mto_id1, type_id1, type1);
        em.persist(master);

        em.getTransaction().commit();

		// Rev 2
        em.getTransaction().begin();

        master = em.find(ManyToOneNotInsertableEntity.class, mto_id1);
        master.setNumber(type_id2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionCounts() {
		assert Arrays.asList(1).equals(getAuditReader().getRevisions(NotInsertableEntityType.class, type_id1));
		assert Arrays.asList(1).equals(getAuditReader().getRevisions(NotInsertableEntityType.class, type_id2));

        assert Arrays.asList(2, 3).equals(getAuditReader().getRevisions(ManyToOneNotInsertableEntity.class, mto_id1));        
    }

    @Test
    public void testNotInsertableEntity() {
        ManyToOneNotInsertableEntity ver1 = getAuditReader().find(ManyToOneNotInsertableEntity.class, mto_id1, 1);
		ManyToOneNotInsertableEntity ver2 = getAuditReader().find(ManyToOneNotInsertableEntity.class, mto_id1, 2);
		ManyToOneNotInsertableEntity ver3 = getAuditReader().find(ManyToOneNotInsertableEntity.class, mto_id1, 3);

		NotInsertableEntityType type1 = getEntityManager().find(NotInsertableEntityType.class, type_id1);
		NotInsertableEntityType type2 = getEntityManager().find(NotInsertableEntityType.class, type_id2);

		assert ver1 == null;
		assert ver2.getType().equals(type1);
		assert ver3.getType().equals(type2);
    }
}
