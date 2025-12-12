/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.cascade;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.legacy.Holder;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
class DefaultCascadeBindingTest {

	@Test
	void testDefaultCascadeIsApplied() {
		StandardServiceRegistry ssr = ServiceRegistryBuilder.buildServiceRegistry();

		try {
			MetadataSources ms = new MetadataSources( ssr );
			ms.addResource( "org/hibernate/orm/test/hbm/cascade/default-cascade.hbm.xml" );

			Metadata metadata = ms.buildMetadata();

			PersistentClass entityBinding = metadata.getEntityBinding( Holder.class.getName() );

			// Default cascade
			assertThat( entityBinding.getProperty( "ones" ).getCascadeStyle() )
					.hasToString( "[STYLE_ALL,STYLE_DELETE_ORPHAN]" );

			// Explicit cascade
			assertThat( entityBinding.getProperty( "fooArray" ).getCascadeStyle() )
					.hasToString( "STYLE_PERSIST" );
		}
		finally {
			ServiceRegistryBuilder.destroy( ssr );
		}
	}
}
