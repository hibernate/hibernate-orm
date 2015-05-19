/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schematoolsnaming;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Tests that naming strategies are picked up by standalone schema tools.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-9467" )
public class StandaloneSchemaToolsNamingTest extends BaseUnitTestCase {
	@Test
	public void testDefaultNamingStrategy() throws Exception {
		checkNamingStrategies(
				new String[] {
						"--text",
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml"
				},
				ImplicitNamingStrategyLegacyJpaImpl.class
		);
	}

	@Test
	public void testDeprecatedNamingStrategy() throws Exception {
		// output should be the same as above test, --naming should simply produce a logged warning
		checkNamingStrategies(
				new String[] {
						"--text",
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--naming=DoesNotExist",
				},
				ImplicitNamingStrategyLegacyJpaImpl.class
		);
	}

	@Test
	public void testJpaCompliantNamingStrategy() throws Exception {
		checkNamingStrategies(
				new String[] {
						"--text",
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--implicit-naming=" + ImplicitNamingStrategyJpaCompliantImpl.class.getName(),
				},
				ImplicitNamingStrategyJpaCompliantImpl.class
		);
	}

	private void checkNamingStrategies(
			String[] args,
			Class<? extends ImplicitNamingStrategy> implicitNamingStrategyClass) throws Exception {
		// SchemaExport
		{
			MetadataImplementor exportedMetadata = SchemaExport.buildMetadataFromMainArgs( args );
			assertTyping(
					implicitNamingStrategyClass,
					exportedMetadata.getMetadataBuildingOptions().getImplicitNamingStrategy()
			);
		}

		// SchemaUpdate
		{
			MetadataImplementor exportedMetadata = SchemaUpdate.buildMetadataFromMainArgs( args );
			assertTyping(
					implicitNamingStrategyClass,
					exportedMetadata.getMetadataBuildingOptions().getImplicitNamingStrategy()
			);
		}

		// SchemaValidator
		{
			MetadataImplementor exportedMetadata = SchemaValidator.buildMetadataFromMainArgs( args );
			assertTyping(
					implicitNamingStrategyClass,
					exportedMetadata.getMetadataBuildingOptions().getImplicitNamingStrategy()
			);
		}
	}
}
