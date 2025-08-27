/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class NestedEmbeddableAttributeOverrideTest extends BaseCoreFunctionalTestCase {

	@Test
	@JiraKey(value="HHH-8021")
	public void testAttributeOverride() {
		EmbeddableB embedB = new EmbeddableB();
		embedB.setEmbedAttrB( "B" );

		EmbeddableA embedA = new EmbeddableA();
		embedA.setEmbedAttrA("A");
		embedA.setEmbedB(embedB);

		EntityWithNestedEmbeddables entity = new EntityWithNestedEmbeddables();
		entity.setEmbedA(embedA);

		Session s = openSession();
		s.beginTransaction();
		s.persist( entity );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithNestedEmbeddables.class };
	}
}
