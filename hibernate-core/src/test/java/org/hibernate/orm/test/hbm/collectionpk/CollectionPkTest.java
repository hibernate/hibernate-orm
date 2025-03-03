/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.collectionpk;

import java.util.Arrays;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
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
@JiraKey( value = "HHH-10206" )
public class CollectionPkTest extends BaseUnitTestCase {
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
	public void testSet() {
		verifyPkNameUsed(
				"org/hibernate/orm/test/hbm/collectionpk/person_set.hbm.xml",
				"primary key (group, name)",
				"primary key (\"group\", name)"
		);
	}

	private void verifyPkNameUsed(String mappingResource, String... expectedName) {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( mappingResource )
				.buildMetadata();

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( ssr ).doCreation( metadata, false, target );

		assertTrue(
				"Expected foreign-key name [" + Arrays.toString(expectedName) + "] not seen in schema creation output",
				Arrays.stream( expectedName ).anyMatch( target::containedText )
		);
	}

	@Test
	public void testMap() {
		verifyPkNameUsed(
				"org/hibernate/orm/test/hbm/collectionpk/person_map.hbm.xml",
				"primary key (group, locale)",
				"primary key (\"group\", locale)"
		);
	}

}
