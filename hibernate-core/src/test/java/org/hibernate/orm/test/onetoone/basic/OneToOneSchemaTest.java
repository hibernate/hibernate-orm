/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.basic;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class OneToOneSchemaTest {

	@Test
	public void testUniqueKeyNotGeneratedViaAnnotations() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			Metadata metadata = MetadataBuildingTestHelper.buildMetadata( ssr, Parent.class, Child.class );

			Table childTable = metadata.getDatabase().getDefaultNamespace().locateTable( Identifier.toIdentifier(
					"CHILD" ) );
			assertFalse( childTable.getUniqueKeys().values().iterator().hasNext(), "UniqueKey was generated when it should not" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
