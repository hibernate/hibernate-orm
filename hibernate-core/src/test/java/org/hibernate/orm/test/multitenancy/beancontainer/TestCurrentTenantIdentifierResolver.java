/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * @author Yanming Zhou
 */
public class TestCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

		public static final String FIXED_TENANT = "FIXED";

		public static final CurrentTenantIdentifierResolver<String> INSTANCE_FOR_BEAN_CONTAINER = new TestCurrentTenantIdentifierResolver();

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}

		@Override
		public String resolveCurrentTenantIdentifier() {
			return FIXED_TENANT;
		}
}