/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fkcircularity;

import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest {

	@Test
	public void testJoinedSublcassesInPK() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadata( serviceRegistry, A.class, B.class, C.class, D.class );
		}
	}

	@Test
	public void testDeepJoinedSuclassesHierachy() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadata( serviceRegistry, ClassA.class, ClassB.class, ClassC.class, ClassD.class );
		}
	}
}
