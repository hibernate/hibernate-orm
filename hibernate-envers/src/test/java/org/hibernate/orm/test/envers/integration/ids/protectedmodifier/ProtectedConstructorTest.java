/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.protectedmodifier;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
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
@JiraKey(value = "HHH-7934")
@EnversTest
@Jpa(annotatedClasses = {WrappedStringId.class, ProtectedConstructorEntity.class})
public class ProtectedConstructorTest {
	private final ProtectedConstructorEntity testEntity = new ProtectedConstructorEntity(
			new WrappedStringId( "embeddedStringId" ),
			"string"
	);

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			em.persist( testEntity );
		} );
	}

	@Test
	public void testAuditEntityInstantiation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List result = auditReader.createQuery()
					.forEntitiesAtRevision( ProtectedConstructorEntity.class, 1 )
					.getResultList();
			assertEquals( Arrays.asList( testEntity ), result );
		} );
	}
}
