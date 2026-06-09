/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp.util;

import org.hibernate.HibernateException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings;

/**
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingHikariCPConnectionProvider extends HikariCPConnectionProvider {
	@Override
	public void configure(@NonNull Map<String, Object> properties) throws HibernateException {
		resolveFromSettings( properties );
		super.configure( properties );
	}
}
