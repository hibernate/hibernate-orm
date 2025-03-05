/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Jan Schatteman
 */
public class DtoIntegratorProviderClassNameSettingProvider implements SettingProvider.Provider<String> {
	@Override
	public String getSetting() {
		return DtoIntegratorProvider.class.getName();
	}
}
