/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.index;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
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
@JiraKey(value = "HHH-10208")
@BaseUnitTest
public class IndexTest {
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
//	@FailureExpected( jiraKey = "HHH-10208" )
	public void testOneToMany() throws Exception {
		verifyIndexCreated(
				"org/hibernate/orm/test/hbm/index/person_manytoone.hbm.xml",
				"person_persongroup_index"
		);
	}

	private void verifyIndexCreated(String mappingResource, String expectedIndexName) {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( mappingResource )
				.buildMetadata();

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( ssr ).doCreation( metadata, false, target );

		assertThat( target.containedText( expectedIndexName ) )
				.describedAs( "Expected index [" + expectedIndexName + "] not seen in schema creation output" )
				.isTrue();
	}

	@Test
//	@FailureExpected( jiraKey = "HHH-10208" )
	public void testProperty() throws Exception {
		verifyIndexCreated(
				"org/hibernate/orm/test/hbm/index/person_property.hbm.xml",
				"person_name_index"
		);
	}

	@Test
	public void testPropertyColumn() throws Exception {
		verifyIndexCreated(
				"org/hibernate/orm/test/hbm/index/person_propertycolumn.hbm.xml",
				"person_name_index"
		);
	}
}
