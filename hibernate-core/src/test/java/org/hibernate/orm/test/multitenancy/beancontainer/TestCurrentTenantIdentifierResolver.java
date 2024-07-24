/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
