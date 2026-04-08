/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SchemaToolingAutoActionTests {

	@Test
	public void testLegacySettingAsAction() {
		final Properties props = new Properties();
		props.put( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		final ActionGrouping actionGrouping = ActionGrouping.interpret( props );

		assertThat( actionGrouping.databaseAction() ).isEqualTo( Action.CREATE_DROP );

	}

	@Test
	public void testSchemaActionsWithNoEntities() {
		final Properties props = new Properties();
		props.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		try (StandardServiceRegistryImpl serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			// build metadata with no entity classes
			final Metadata metadata = new MetadataSources( serviceRegistry ).buildMetadata();
			assertThat( metadata.getContributors() ).isEmpty();

			final Set<ActionGrouping> groupings = ActionGrouping.interpret( metadata, props );
			assertThat( groupings ).isNotEmpty();

			final ActionGrouping grouping = groupings.iterator().next();
			assertThat( grouping.databaseAction() ).isEqualTo( Action.CREATE_DROP );
			assertThat( grouping.scriptAction() ).isEqualTo( Action.NONE );
		}
	}
}
