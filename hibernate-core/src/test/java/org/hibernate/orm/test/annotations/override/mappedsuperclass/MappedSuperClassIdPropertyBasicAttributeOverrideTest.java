/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Stanislav Gubanov
 */
@JiraKey(value = "HHH-11771")
public class MappedSuperClassIdPropertyBasicAttributeOverrideTest {

	@Test
	public void test() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {

			MetadataImplementor metadata = (MetadataImplementor) MetadataBuildingTestHelper.buildMetadata(
					ssr,
					MappedSuperClassWithUuidAsBasic.class,
					SubclassWithUuidAsId.class
			);
			try(SessionFactory sf = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )){
				fail( "Should throw exception!" );
			}
		}
		catch (MappingException expected) {
			// expected
		}
	}

}
