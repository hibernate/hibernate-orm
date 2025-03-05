/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class DatabaseMultiTenancyTest extends AbstractMultiTenancyTest {

	@Override
	protected String tenantUrl(String originalUrl, String tenantIdentifier) {
		return originalUrl.replace("db1", tenantIdentifier);
	}
}
