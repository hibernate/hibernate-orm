/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0.util;

import org.hibernate.HibernateException;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.testing.orm.junit.SettingProvider;

import java.util.Map;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings;

/**
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingC3P0ConnectionProvider extends C3P0ConnectionProvider {
	@Override
	public void configure(Map<String, Object> properties) throws HibernateException {
		resolveFromSettings( properties );
		super.configure( properties );
	}

	public static class SettingProviderImpl implements SettingProvider.Provider<GradleParallelTestingC3P0ConnectionProvider> {
		@Override
		public GradleParallelTestingC3P0ConnectionProvider getSetting() {
			return new GradleParallelTestingC3P0ConnectionProvider();
		}
	}
}
