package org.jboss.envers.test.integration.properties;

import java.util.Arrays;
import java.util.Iterator;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.jboss.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Nicolas Doroskevich
 */
public class UnversionedOptimisticLockingField extends AbstractEntityTest {
	private Integer id1;

	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(UnversionedOptimisticLockingFieldEntity.class);

		cfg.setProperty("org.jboss.envers.unversionedOptimisticLockingField", "true");
	}

	@BeforeClass(dependsOnMethods = "init")
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		UnversionedOptimisticLockingFieldEntity olfe = new UnversionedOptimisticLockingFieldEntity("x");
		em.persist(olfe);
		id1 = olfe.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		olfe = em.find(UnversionedOptimisticLockingFieldEntity.class, id1);
		olfe.setStr("y");
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionCounts() {
		assert Arrays.asList(1, 2).equals(
				getVersionsReader().getRevisions(UnversionedOptimisticLockingFieldEntity.class,
						id1));
	}

	@Test
	public void testHistoryOfId1() {
		UnversionedOptimisticLockingFieldEntity ver1 = new UnversionedOptimisticLockingFieldEntity(id1, "x");
		UnversionedOptimisticLockingFieldEntity ver2 = new UnversionedOptimisticLockingFieldEntity(id1, "y");
		
		assert getVersionsReader().find(UnversionedOptimisticLockingFieldEntity.class, id1, 1)
				.equals(ver1);
		assert getVersionsReader().find(UnversionedOptimisticLockingFieldEntity.class, id1, 2)
				.equals(ver2);
	}
	
	@Test
	public void testMapping() {
		PersistentClass pc = getCfg().getClassMapping(UnversionedOptimisticLockingFieldEntity.class.getName() + "_versions");
		Iterator pi = pc.getPropertyIterator();
		while(pi.hasNext()) {
			Property p = (Property) pi.next();
			assert !"optLocking".equals(p.getName());
		}
	}
}
