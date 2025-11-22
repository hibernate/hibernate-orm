/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.discriminator.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class TenantResolver implements CurrentTenantIdentifierResolver<Long> {
		@Override
		public Long resolveCurrentTenantIdentifier() {
			return 1L;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}
	}
