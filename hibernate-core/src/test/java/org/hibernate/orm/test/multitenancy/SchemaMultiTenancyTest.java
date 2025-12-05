/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class SchemaMultiTenancyTest extends AbstractMultiTenancyTest {

	public static final String SCHEMA_TOKEN = ";INIT=CREATE SCHEMA IF NOT EXISTS %1$s\\;SET SCHEMA %1$s";

	@Override
	protected String tenantUrl(String originalUrl, String tenantIdentifier) {
		return originalUrl + String.format(SCHEMA_TOKEN, tenantIdentifier);
	}

}
