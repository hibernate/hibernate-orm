/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.schema;

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SchemaToolingAutoActionTests {
	@Test
	public void testLegacySettingAsAction() {
		final Properties props = new Properties();
		props.put( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		final ActionGrouping actionGrouping = ActionGrouping.interpret( props );

		assertThat( actionGrouping.getDatabaseAction(), is( Action.CREATE_DROP ) );

	}
}
