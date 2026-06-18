/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.criteria.JpaQueryableCriteria;
import org.hibernate.query.sqm.spi.SqmQuerySource;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;

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

	@Override
	SqmStatement<T> copy(SqmCopyContext context);

	interface ParameterResolutions {
		ParameterResolutions EMPTY = new ParameterResolutions() {
			@Override
			public Set<SqmParameter<?>> getSqmParameters() {
				return Collections.emptySet();
			}

			@Override
			public Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> getJpaCriteriaParamResolutions() {
				return Collections.emptyMap();
			}
		};

		static ParameterResolutions empty() {
			return EMPTY;
		}

		Set<SqmParameter<?>> getSqmParameters();
		Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> getJpaCriteriaParamResolutions();
	}
}
