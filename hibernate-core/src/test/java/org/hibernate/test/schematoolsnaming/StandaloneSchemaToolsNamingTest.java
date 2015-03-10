/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
