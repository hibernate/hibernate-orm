/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.hbmxml;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Jpa(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/hbmxml/ClassMappedMoreThanOnce.hbm.xml"
)
public class MappingClassMoreThanOnceTest {
	/**
	 * Tests that an entity manager can be created when a class is mapped more than once.
	 */
	@Test
	@JiraKey(value = "HHH-8775")
	public void testBootstrapWithClassMappedMOreThanOnce(EntityManagerFactoryScope scope) {

		EntityManagerFactory emf = null;
		try {
			emf = scope.getEntityManagerFactory();
		}
		finally {
			if ( emf != null ) {
				try {
					emf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
