/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util.dtd;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@BaseUnitTest
public class EntityResolverTest {
	@Test
	public void testEntityIncludeResolution(ServiceRegistryScope registryScope) {
		// Parent.hbm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/test/util/dtd/child.xml">
		// which we are expecting the Hibernate custom entity resolver to be able to resolve
		// locally via classpath lookup.
		new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/util/dtd/Parent.hbm.xml" )
				.buildMetadata();
	}
}
