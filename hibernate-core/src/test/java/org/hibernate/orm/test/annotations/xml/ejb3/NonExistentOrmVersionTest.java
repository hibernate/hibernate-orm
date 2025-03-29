/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Test;

import static org.junit.Assert.fail;

@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-6271" ),
		@JiraKey( value = "HHH-14529" )
} )
public class NonExistentOrmVersionTest extends BaseUnitTestCase {
	@Test
	public void testNonExistentOrmVersion() {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			new MetadataSources( serviceRegistry )
					.addResource( "org/hibernate/orm/test/annotations/xml/ejb3/orm5.xml" )
					.buildMetadata();
			fail( "Expecting failure due to unsupported xsd version" );
		}
		catch ( InvalidMappingException expected ) {
		}
	}
}
