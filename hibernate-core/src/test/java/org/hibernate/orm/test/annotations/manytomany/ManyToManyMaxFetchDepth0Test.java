/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * Many to many tests using max_fetch_depth == 0
 *
 * @author Gail Badner
 */
@ServiceRegistry(
		settings = @Setting(name = Environment.MAX_FETCH_DEPTH, value = "0"),
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.DEFAULT_LIST_SEMANTICS,
						provider = ManyToManyTest.ListSemanticProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						provider = ManyToManyTest.ImplicitNamingStrategyProvider.class
				)
		}
)
public class ManyToManyMaxFetchDepth0Test extends ManyToManyTest {
}
