/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.bad;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.models.MemberResolutionException;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class BadMappingTests {
	@Test
	@ServiceRegistry
	void testBadEmbeddedName(ServiceRegistryScope registryScope) {
		try {
			MetadataBuildingTestHelper.buildMetadata(
					registryScope.getRegistry(),
					new MappingSources().addMappingResource( "mappings/models/bad/bad-embedded.xml" )
			);
			fail( "Expecting failure" );
		}
		catch (MemberResolutionException expected){
			// expected outcome
		}

	}

	@Test
	@ServiceRegistry
	void testBadEmbeddedIdName(ServiceRegistryScope registryScope) {
		try {
			MetadataBuildingTestHelper.buildMetadata(
					registryScope.getRegistry(),
					new MappingSources().addMappingResource( "mappings/models/bad/bad-embedded-id.xml" )
			);
			fail( "Expecting failure" );
		}
		catch (MemberResolutionException expected){
			// expected outcome
		}
	}
}
