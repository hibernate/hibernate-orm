/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.agroal.util;

import org.hibernate.HibernateException;
import org.hibernate.agroal.internal.AgroalConnectionProvider;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings;

/**
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingAgroalConnectionProvider extends AgroalConnectionProvider {
	@Override
	public void configure(@NonNull Map<String, Object> properties) throws HibernateException {
		resolveFromSettings( properties );
		super.configure( properties );
	}
}
