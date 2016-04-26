/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.multitenancy;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class SchemaMultiTenancyTest extends AbstractMultiTenancyTest {

	public static final String SCHEMA_TOKEN = ";INIT=CREATE SCHEMA IF NOT EXISTS %1$s\\;SET SCHEMA %1$s";

	@Override
	protected MultiTenancyStrategy multiTenancyStrategy() {
		return MultiTenancyStrategy.SCHEMA;
	}

	@Override
	protected String tenantUrl(String originalUrl, String tenantIdentifier) {
		return originalUrl + String.format( SCHEMA_TOKEN, tenantIdentifier );
	}

}
