/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-6271"),
		@JiraKey(value = "HHH-14529")
})
@BaseUnitTest
public class NonExistentOrmVersionTest {

	@Test
	public void testNonExistentOrmVersion() {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			assertThrows( InvalidMappingException.class, () -> new MetadataSources( serviceRegistry )
							.addResource( "org/hibernate/orm/test/annotations/xml/ejb3/orm5.xml" )
							.buildMetadata()
					, "Expecting failure due to unsupported xsd version"
			);
		}
	}
}
