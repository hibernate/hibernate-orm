/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.query.criteria.JpaQueryableCriteria;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * The basic SQM statement contract for top-level statements
 *
 * @author Steve Ebersole
 */
public interface SqmStatement<T> extends SqmQuery<T>, JpaQueryableCriteria<T>, SqmVisitableNode {
	SqmQuerySource getQuerySource();

	/**
	 * Access to the (potentially still growing) collection of parameters for the statement.
	 *
	 */
	Set<SqmParameter<?>> getSqmParameters();

	ParameterResolutions resolveParameters();

	interface ParameterResolutions {
		ParameterResolutions EMPTY = new ParameterResolutions() {
			@Override
			public Set<SqmParameter<?>> getSqmParameters() {
				return Collections.emptySet();
			}

			@Override
			public Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions() {
				return Collections.emptyMap();
			}
		};

		static ParameterResolutions empty() {
			return EMPTY;
		}

		Set<SqmParameter<?>> getSqmParameters();
		Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions();
	}
}
