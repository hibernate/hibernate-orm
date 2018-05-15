/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.index;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10208" )
public class IndexTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
//	@FailureExpected( jiraKey = "HHH-10208" )
	public void testOneToMany() throws Exception {
		verifyIndexCreated(
				"org/hibernate/test/hbm/index/person_manytoone.hbm.xml",
				"person_persongroup_index"
		);
	}

	private void verifyIndexCreated(String mappingResource, String expectedIndexName) {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( mappingResource )
				.buildMetadata();

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( ssr ).doCreation( metadata, false, target );

		assertTrue(
				"Expected index [" + expectedIndexName + "] not seen in schema creation output",
				target.containedText( expectedIndexName )
		);
	}

	@Test
//	@FailureExpected( jiraKey = "HHH-10208" )
	public void testProperty() throws Exception {
		verifyIndexCreated(
				"org/hibernate/test/hbm/index/person_property.hbm.xml",
				"person_name_index"
		);
	}

	@Test
	public void testPropertyColumn() throws Exception {
		verifyIndexCreated(
				"org/hibernate/test/hbm/index/person_propertycolumn.hbm.xml",
				"person_name_index"
		);
	}
}
