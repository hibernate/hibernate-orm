/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import org.hibernate.envers.internal.EnversMessageLogger;

import org.jboss.logging.Logger;

/**
 * Deprecated Audit strategy class.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @deprecated use {@link ValidityAuditStrategy} instead.
 */
@Deprecated
public class ValidTimeAuditStrategy extends ValidityAuditStrategy {

	public static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			ValidTimeAuditStrategy.class.getName()
	);

	/**
	 * Default constructor. Log a warn message that this class is deprecated.
	 */
	public ValidTimeAuditStrategy() {
		LOG.validTimeAuditStrategyDeprecated();
	}

}
