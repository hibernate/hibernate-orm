/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwningEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the ValidityAuditStrategy on many-to-many Sets.
 * It was first introduced because of a bug when adding and removing the same element
 * from the set multiple times between database persists.
 * Created on: 24.05.11
 *
 * @author Oliver Lorenz
 * @since 3.6.5
 */
@EnversTest
@Jpa(annotatedClasses = {SetOwningEntity.class, SetOwnedEntity.class},
		integrationSettings = @Setting(name = EnversSettings.AUDIT_STRATEGY, value = "org.hibernate.envers.strategy.ValidityAuditStrategy"))
public class ValidityAuditStrategyManyToManyTest {

	private Integer ing_id;
	private Integer ed_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		final SetOwningEntity setOwningEntity = new SetOwningEntity(1, "parent");
		final SetOwnedEntity setOwnedEntity = new SetOwnedEntity(2, "child");

		// Revision 1: Initial persist
		scope.inTransaction(em -> {
			em.persist(setOwningEntity);
			em.persist(setOwnedEntity);
		});

		ing_id = setOwningEntity.getId();
		ed_id = setOwnedEntity.getId();

		// Revision 2: add child for first time
		scope.inTransaction(em -> {
			SetOwningEntity owningEntity = em.find(SetOwningEntity.class, ing_id);
			SetOwnedEntity ownedEntity = em.find(SetOwnedEntity.class, ed_id);

			owningEntity.setReferences(new HashSet<SetOwnedEntity>());
			owningEntity.getReferences().add(ownedEntity);
		});

		// Revision 3: remove child
		scope.inTransaction(em -> {
			SetOwningEntity owningEntity = em.find(SetOwningEntity.class, ing_id);
			SetOwnedEntity ownedEntity = em.find(SetOwnedEntity.class, ed_id);

			owningEntity.getReferences().remove(ownedEntity);
		});

		// Revision 4: add child again
		scope.inTransaction(em -> {
			SetOwningEntity owningEntity = em.find(SetOwningEntity.class, ing_id);
			SetOwnedEntity ownedEntity = em.find(SetOwnedEntity.class, ed_id);

			owningEntity.getReferences().add(ownedEntity);
		});

		// Revision 5: remove child again
		scope.inTransaction(em -> {
			SetOwningEntity owningEntity = em.find(SetOwningEntity.class, ing_id);
			SetOwnedEntity ownedEntity = em.find(SetOwnedEntity.class, ed_id);

			owningEntity.getReferences().remove(ownedEntity);
		});
	}

	@Test
	public void testMultipleAddAndRemove(EntityManagerFactoryScope scope) {
		// now the set owning entity list should be empty again
		scope.inEntityManager(em -> {
			SetOwningEntity owningEntity = em.find(SetOwningEntity.class, ing_id);
			assertEquals(0, owningEntity.getReferences().size());
		});
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager(em -> {
			final var auditReader = AuditReaderFactory.get(em);
			assertEquals(Arrays.asList(1, 2, 3, 4, 5), auditReader.getRevisions(SetOwningEntity.class, ing_id));
			assertEquals(Arrays.asList(1, 2, 3, 4, 5), auditReader.getRevisions(SetOwnedEntity.class, ed_id));
		});
	}

	@Test
	public void testHistoryOfIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager(em -> {
			final var auditReader = AuditReaderFactory.get(em);
			SetOwningEntity ver_empty = createOwningEntity();
			SetOwningEntity ver_child = createOwningEntity(new SetOwnedEntity(ed_id, "child"));

			assertEquals(ver_empty, auditReader.find(SetOwningEntity.class, ing_id, 1));
			assertEquals(ver_child, auditReader.find(SetOwningEntity.class, ing_id, 2));
			assertEquals(ver_empty, auditReader.find(SetOwningEntity.class, ing_id, 3));
			assertEquals(ver_child, auditReader.find(SetOwningEntity.class, ing_id, 4));
			assertEquals(ver_empty, auditReader.find(SetOwningEntity.class, ing_id, 5));
		});
	}

	@Test
	public void testHistoryOfEd1(EntityManagerFactoryScope scope) {
		scope.inEntityManager(em -> {
			final var auditReader = AuditReaderFactory.get(em);
			SetOwnedEntity ver_empty = createOwnedEntity();
			SetOwnedEntity ver_child = createOwnedEntity(new SetOwningEntity(ing_id, "parent"));

			assertEquals(ver_empty, auditReader.find(SetOwnedEntity.class, ed_id, 1));
			assertEquals(ver_child, auditReader.find(SetOwnedEntity.class, ed_id, 2));
			assertEquals(ver_empty, auditReader.find(SetOwnedEntity.class, ed_id, 3));
			assertEquals(ver_child, auditReader.find(SetOwnedEntity.class, ed_id, 4));
			assertEquals(ver_empty, auditReader.find(SetOwnedEntity.class, ed_id, 5));
		});
	}

	private SetOwningEntity createOwningEntity(SetOwnedEntity... owned) {
		SetOwningEntity result = new SetOwningEntity(ing_id, "parent");
		result.setReferences(new HashSet<SetOwnedEntity>());
		for (SetOwnedEntity setOwnedEntity : owned) {
			result.getReferences().add(setOwnedEntity);
		}

		return result;
	}

	private SetOwnedEntity createOwnedEntity(SetOwningEntity... owning) {
		SetOwnedEntity result = new SetOwnedEntity(ed_id, "child");
		result.setReferencing(new HashSet<SetOwningEntity>());
		for (SetOwningEntity setOwningEntity : owning) {
			result.getReferencing().add(setOwningEntity);
		}

		return result;
	}
}
