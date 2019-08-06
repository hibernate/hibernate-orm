/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;

/**
 * Options for semantic analysis
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationOptions {
	/**
	 * Should we interpret the query strictly according to the JPA specification?  In
	 * other words, should Hibernate "extensions" to the query language be disallowed?
	 *
	 * @see StrictJpaComplianceViolation
	 */
	boolean useStrictJpaCompliance();
}
