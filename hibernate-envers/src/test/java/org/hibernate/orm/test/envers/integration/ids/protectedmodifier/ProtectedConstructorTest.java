/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.protectedmodifier;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7934")
public class ProtectedConstructorTest extends BaseEnversJPAFunctionalTestCase {
	private final ProtectedConstructorEntity testEntity = new ProtectedConstructorEntity(
			new WrappedStringId(
					"embeddedStringId"
			), "string"
	);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {WrappedStringId.class, ProtectedConstructorEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.persist( testEntity );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testAuditEntityInstantiation() {
		List result = getAuditReader().createQuery()
				.forEntitiesAtRevision( ProtectedConstructorEntity.class, 1 )
				.getResultList();
		Assert.assertEquals( Arrays.asList( testEntity ), result );
	}
}
