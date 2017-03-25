/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;
import java.sql.Time;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>CURRENT_TIME</tt> function.
 *
 * @author Steve Ebersole
 */
public class CurrentTimeFunction
		extends BasicFunctionExpression<Time> 
		implements Serializable {
	public static final String NAME = "current_time";

	public CurrentTimeFunction(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder, Time.class, NAME );
	}
}
