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
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.hbm.index.JournalingSchemaToolingTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class CollectionKeyFkNameTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey( value = "HHH-10207" )
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

		assertTrue(
				"Expected foreign-key name [" + expectedName + "] not seen in schema creation output",
				target.containedText( expectedName )
		);
	}

	@Test
	@JiraKey( value = "HHH-10207" )
	public void testExplicitFkNameOnManyToOne() {
		verifyFkNameUsed(
				"org/hibernate/orm/test/hbm/fk/person_set.hbm.xml",
				"person_persongroup_fk"
		);
	}
}
