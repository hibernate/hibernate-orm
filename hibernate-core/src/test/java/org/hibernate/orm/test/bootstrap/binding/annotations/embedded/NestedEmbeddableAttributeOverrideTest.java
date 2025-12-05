/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				EntityWithNestedEmbeddables.class
		}
)
@SessionFactory
public class NestedEmbeddableAttributeOverrideTest {

	@Test
	@JiraKey(value = "HHH-8021")
	public void testAttributeOverride(SessionFactoryScope scope) {
		EmbeddableB embedB = new EmbeddableB();
		embedB.setEmbedAttrB( "B" );

		EmbeddableA embedA = new EmbeddableA();
		embedA.setEmbedAttrA( "A" );
		embedA.setEmbedB( embedB );

		EntityWithNestedEmbeddables entity = new EntityWithNestedEmbeddables();
		entity.setEmbedA( embedA );

		scope.inTransaction(
				session -> {
					session.persist( entity );
				}
		);
	}
}
