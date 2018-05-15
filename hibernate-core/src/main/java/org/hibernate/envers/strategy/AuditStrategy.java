/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import org.hibernate.annotations.Remove;

/**
 * Behaviours of different audit strategy for populating audit data.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 *
 * @deprecated (since 5.4), use {@link org.hibernate.envers.strategy.spi.AuditStrategy} instead.
 */
@Deprecated
@Remove
public interface AuditStrategy extends org.hibernate.envers.strategy.spi.AuditStrategy {
	/**
	 * Get the default audit strategy name.
	 * @return the default audit strategy class name.
	 */
	static String getDefaultStrategyName() {
		return DefaultAuditStrategy.class.getName();
	}
}
