/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.fk;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.hbm.index.JournalingSchemaToolingTarget;
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
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10207" )
	public void testExplicitFkNameOnCollectionKey() {
		verifyFkNameUsed(
				"org/hibernate/test/hbm/fk/person_set.hbm.xml",
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
	@TestForIssue( jiraKey = "HHH-10207" )
	public void testExplicitFkNameOnManyToOne() {
		verifyFkNameUsed(
				"org/hibernate/test/hbm/fk/person_set.hbm.xml",
				"person_persongroup_fk"
		);
	}
}
