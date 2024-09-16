/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
