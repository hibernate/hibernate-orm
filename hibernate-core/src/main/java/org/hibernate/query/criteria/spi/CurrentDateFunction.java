/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;
import java.sql.Date;

/**
 * Models the ANSI SQL <tt>CURRENT_DATE</tt> function.
 *
 * @author Steve Ebersole
 */
public class CurrentDateFunction extends AbstractStandardFunction<Date> {
	public static final String NAME = "current_date";

	public CurrentDateFunction(CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Date.class, criteriaBuilder );
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitCurrentDateFunction( this );
	}
}
