/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Steve Ebersole
 */
public class CustomNamingStrategyProvider implements SettingProvider.Provider<PhysicalNamingStrategy> {
	@Override
	public PhysicalNamingStrategy getSetting() {
		return new CustomNamingStrategy();
	}
}
