/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.collectionpk;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.hbm.index.JournalingSchemaToolingTarget;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-10206")
@BaseUnitTest
public class CollectionPkTest {
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

		assertThat( Arrays.stream( expectedName ).anyMatch( target::containedText ) )
				.describedAs( "Expected foreign-key name [" + Arrays.toString(
						expectedName ) + "] not seen in schema creation output" )
				.isTrue();
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
