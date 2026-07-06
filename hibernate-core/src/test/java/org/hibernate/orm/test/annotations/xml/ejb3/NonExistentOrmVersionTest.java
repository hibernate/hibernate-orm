/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.util.ServiceRegistryUtil;
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
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			assertThrows( InvalidMappingException.class, () -> MetadataBuildingTestHelper.buildMetadata(
							serviceRegistry,
							new MappingSources()
									.addMappingResource( "org/hibernate/orm/test/annotations/xml/ejb3/orm5.xml" )
					)
					, "Expecting failure due to unsupported xsd version"
			);
		}
	}
}
