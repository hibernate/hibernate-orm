/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Jan Schatteman
 */
public class DtoIntegratorProviderInstanceSettingProvider implements SettingProvider.Provider<DtoIntegratorProvider>  {
	@Override
	public DtoIntegratorProvider getSetting() {
		return new DtoIntegratorProvider();
	}
}
