/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entityname;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.DuplicateMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-13060" )
@ServiceRegistry
public class DuplicateEntityNameTest {
	@Test
	void testIt(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry registry = registryScope.getRegistry();
		try {
			new MetadataSources( registry )
					.addAnnotatedClasses( Purchase1.class, Purchase2.class )
					.buildMetadata();
			fail("Should throw DuplicateMappingException");
		}
		catch (DuplicateMappingException expected) {
			assertEquals(
					"Entity classes [org.hibernate.orm.test.entityname.DuplicateEntityNameTest$Purchase1] and [org.hibernate.orm.test.entityname.DuplicateEntityNameTest$Purchase2] share the entity name 'Purchase' (entity names must be distinct)",
					expected.getMessage()
			);
		}
	}

	@Entity(name = "Purchase")
	@Table(name="purchase_old")
	public static class Purchase1 {
		@Id
		public String uid;
	}

	@Entity(name = "Purchase")
	@Table(name="purchase_new")
	public static class Purchase2 {
		@Id
		public String uid;
	}

}
