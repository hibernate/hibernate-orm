/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.tools;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7106")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class})
public class SchemaExportTest {
	private Integer id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Populate database with test data.
		scope.inTransaction(em -> {
			StrTestEntity entity = new StrTestEntity("data");
			em.persist(entity);
			id = entity.getId();
		});
	}

	@Test
	public void testAuditDataRetrieval(EntityManagerFactoryScope scope) {
		scope.inEntityManager(em -> {
			final var auditReader = AuditReaderFactory.get(em);
			assertEquals(Arrays.asList(1), auditReader.getRevisions(StrTestEntity.class, id));
			assertEquals(new StrTestEntity("data", id), auditReader.find(StrTestEntity.class, id, 1));
		});
	}
}
