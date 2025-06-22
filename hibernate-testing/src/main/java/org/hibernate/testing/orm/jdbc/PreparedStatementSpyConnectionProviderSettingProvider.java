/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.jdbc;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Steve Ebersole
 */
public class PreparedStatementSpyConnectionProviderSettingProvider implements SettingProvider.Provider<PreparedStatementSpyConnectionProvider> {
	@Override
	public PreparedStatementSpyConnectionProvider getSetting() {
		return new PreparedStatementSpyConnectionProvider();
	}
}
