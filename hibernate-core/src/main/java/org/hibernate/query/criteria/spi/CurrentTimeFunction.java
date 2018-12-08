/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.sql.Time;

/**
 * Models the ANSI SQL <tt>CURRENT_TIME</tt> function.
 *
 * @author Steve Ebersole
 */
public class CurrentTimeFunction extends AbstractStandardFunction<Time> {
	public static final String NAME = "current_time";

	public CurrentTimeFunction(CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Time.class, criteriaBuilder );
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitCurrentTimeFunction( this );
	}
}
