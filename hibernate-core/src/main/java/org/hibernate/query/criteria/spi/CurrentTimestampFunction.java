/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.sql.Timestamp;

/**
 * Models the ANSI SQL <tt>CURRENT_TIMESTAMP</tt> function.
 *
 * @author Steve Ebersole
 */
public class CurrentTimestampFunction extends AbstractStandardFunction<Timestamp> {
	public static final String NAME = "current_timestamp";

	public CurrentTimestampFunction(CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Timestamp.class, criteriaBuilder );
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitCurrentTimestampFunction( this );
	}
}
