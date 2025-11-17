/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.order;

import org.hibernate.envers.configuration.Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface AuditOrder {

	/**
	 * Specifies the null order precedence for the order-by column specification.
	 *
	 * @param nullPrecedence the null precedence, may be {@code null}.
	 * @return this {@link AuditOrder} for chaining purposes
	 */
	AuditOrder nulls(NullPrecedence nullPrecedence);

	/**
	 * @param configuration the configuration
	 * @return the order data.
	 */
	OrderData getData(Configuration configuration);

	class OrderData {

		private final String alias;
		private final String propertyName;
		private final boolean ascending;
		private final NullPrecedence nullPrecedence;

		public OrderData(String alias, String propertyName, boolean ascending, NullPrecedence nullPrecedence) {
			this.alias = alias;
			this.propertyName = propertyName;
			this.ascending = ascending;
			this.nullPrecedence = nullPrecedence;
		}

		public String getAlias(String baseAlias) {
			return alias == null ? baseAlias : alias;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public boolean isAscending() {
			return ascending;
		}

		public NullPrecedence getNullPrecedence() {
			return nullPrecedence;
		}
	}

}
