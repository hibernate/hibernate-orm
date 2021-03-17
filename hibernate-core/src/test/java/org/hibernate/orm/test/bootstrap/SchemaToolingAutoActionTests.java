/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap;

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

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

		final SchemaManagementToolCoordinator.ActionGrouping actionGrouping = SchemaManagementToolCoordinator.ActionGrouping.interpret( props );

		assertThat( actionGrouping.getDatabaseAction(), is( Action.CREATE_DROP ) );

	}
}
