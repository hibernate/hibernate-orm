/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp.util;

import org.hibernate.HibernateException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import java.util.Map;

import static org.hibernate.testing.jdbc.GradleParallelTestingUsernameResolver.resolveFromSettings;

/**
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingHikariCPConnectionProvider extends HikariCPConnectionProvider {
	@Override
	public void configure(Map<String, Object> properties) throws HibernateException {
		resolveFromSettings( properties );
		super.configure( properties );
	}
}
