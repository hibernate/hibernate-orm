/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaTextExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmTextExpression extends SqmExpression<String>, JpaTextExpression {
	@Override
	SqmTextExpression coalesce(Expression<? extends String> y);

	@Override
	SqmTextExpression coalesce(String y);

	@Override
	SqmTextExpression nullif(Expression<? extends String> y);

	@Override
	SqmTextExpression nullif(String y);
}
