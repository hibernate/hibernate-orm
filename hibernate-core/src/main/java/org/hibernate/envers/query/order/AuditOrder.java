/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.order;

import org.hibernate.envers.boot.AuditService;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface AuditOrder {
	/**
	 * @param auditService The audit service
	 *
	 * @return the order data.
	 */
	OrderData getData(AuditService auditService);

	class OrderData {

		private final String alias;
		private final String propertyName;
		private final boolean ascending;

		public OrderData(String alias, String propertyName, boolean ascending) {
			this.alias = alias;
			this.propertyName = propertyName;
			this.ascending = ascending;
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

	}

}
