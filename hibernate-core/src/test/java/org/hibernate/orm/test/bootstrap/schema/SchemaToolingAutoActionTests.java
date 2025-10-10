/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.schema;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.junit.jupiter.api.Test;

import java.util.Properties;

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
}
