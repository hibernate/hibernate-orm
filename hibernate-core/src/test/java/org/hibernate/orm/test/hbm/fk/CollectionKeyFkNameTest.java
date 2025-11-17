/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.fk;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.orm.test.hbm.index.JournalingSchemaToolingTarget;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class CollectionKeyFkNameTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void before() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-10207")
	public void testExplicitFkNameOnCollectionKey() {
		verifyFkNameUsed(
				"org/hibernate/orm/test/hbm/fk/person_set.hbm.xml",
				"person_persongroup_fk"
		);
	}

	private void verifyFkNameUsed(String mappingResource, String expectedName) {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( mappingResource )
				.buildMetadata();

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( ssr ).doCreation(
				metadata,
				ssr,
				ssr.getService( ConfigurationService.class ).getSettings(),
				false,
				target
		);

		assertThat( target.containedText( expectedName ) )
				.describedAs( "Expected foreign-key name [" + expectedName + "] not seen in schema creation output" )
				.isTrue();
	}

	@Test
	@JiraKey(value = "HHH-10207")
	public void testExplicitFkNameOnManyToOne() {
		verifyFkNameUsed(
				"org/hibernate/orm/test/hbm/fk/person_set.hbm.xml",
				"person_persongroup_fk"
		);
	}
}
