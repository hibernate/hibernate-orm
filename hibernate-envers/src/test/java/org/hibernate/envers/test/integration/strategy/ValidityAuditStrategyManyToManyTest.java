package org.hibernate.envers.test.integration.strategy;

import java.util.HashSet;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.manytomany.SetOwnedEntity;
import org.hibernate.envers.test.entities.manytomany.SetOwningEntity;
import org.hibernate.envers.test.entities.manytomany.sametable.Child1Entity;
import org.hibernate.envers.test.entities.manytomany.sametable.Child2Entity;
import org.hibernate.envers.test.entities.manytomany.sametable.ParentEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests whether the mapping of many-to-many Sets is
 * Created on: 24.05.11
 *
 * @author Oliver Lorenz
 * @since 0.0
 */
public class ValidityAuditStrategyManyToManyTest extends AbstractEntityTest {

    private Integer ing_id;

    private Integer ed_id;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SetOwningEntity.class);
        cfg.addAnnotatedClass(SetOwnedEntity.class);

		cfg.setProperty("org.hibernate.envers.audit_strategy",
                "org.hibernate.envers.strategy.ValidityAuditStrategy");
    }

	@BeforeClass(enabled = true, dependsOnMethods = "init")
	public void initData() {
		final EntityManager em = getEntityManager();

		final SetOwningEntity setOwningEntity = new SetOwningEntity(1, "parent");
		final SetOwnedEntity setOwnedEntity = new SetOwnedEntity(2, "child");

		// Initial persist
		em.getTransaction().begin();

		em.persist(setOwningEntity);
        em.persist(setOwnedEntity);

		em.getTransaction().commit();
		em.clear();

        ing_id = setOwningEntity.getId();
        ed_id = setOwnedEntity.getId();
    }

	@Test(enabled = true)
	public void testMultipleAddAndRemove() {
		final EntityManager em = getEntityManager();

        // add child for first time
        em.getTransaction().begin();

        SetOwningEntity owningEntity = getEntityManager().find(SetOwningEntity.class, ing_id);
        SetOwnedEntity ownedEntity = getEntityManager().find(SetOwnedEntity.class, ed_id);

        owningEntity.setReferences(new HashSet<SetOwnedEntity>());
        owningEntity.getReferences().add(ownedEntity);

		em.getTransaction().commit();
		em.clear();

        // remove child
        em.getTransaction().begin();

        owningEntity = getEntityManager().find(SetOwningEntity.class, ing_id);
        ownedEntity = getEntityManager().find(SetOwnedEntity.class, ed_id);

        owningEntity.getReferences().remove(ownedEntity);

		em.getTransaction().commit();
		em.clear();

        // add child again
        em.getTransaction().begin();

        owningEntity = getEntityManager().find(SetOwningEntity.class, ing_id);
        ownedEntity = getEntityManager().find(SetOwnedEntity.class, ed_id);

        owningEntity.getReferences().add(ownedEntity);

		em.getTransaction().commit();
		em.clear();

        // remove child again
        em.getTransaction().begin();

        owningEntity = getEntityManager().find(SetOwningEntity.class, ing_id);
        ownedEntity = getEntityManager().find(SetOwnedEntity.class, ed_id);

        owningEntity.getReferences().remove(ownedEntity);

		em.getTransaction().commit();
		em.clear();

        // now the set owning entity list should be empty again
        owningEntity = getEntityManager().find(SetOwningEntity.class, ing_id);
        assert owningEntity.getReferences().size() == 0;
    }


}
