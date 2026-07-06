/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
public class ValidationTests {
	@Test
	void testOneToMany() {
		try (var serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadata(
					serviceRegistry,
					new MappingSources()
							.addManagedClass( InvalidCollectionOwner.class )
							.addManagedClass( CollectionOwned.class )
			);
		}
		catch (UnsupportedMappingException expected) {
		}
	}
}
