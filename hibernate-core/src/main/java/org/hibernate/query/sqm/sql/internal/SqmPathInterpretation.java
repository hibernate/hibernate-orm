/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.expression.Expression;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interpretation of a {@link SqmPath} as part of the translation to SQL AST.  We need specialized handling
 * for path interpretations because it can (and likely) contains multiple SqlExpressions (entity to its columns, e.g.)
 *
 * @see org.hibernate.query.sqm.sql.SqmToSqlAstConverter
 *
 * @author Steve Ebersole
 */
public interface SqmPathInterpretation<T> extends Expression, DomainResultProducer<T> {
	NavigablePath getNavigablePath();

	@Override
	ModelPart getExpressionType();

	default Expression getSqlExpression() {
		return this;
	}

	default @Nullable String getAffectedTableName() {
		return null;
	}
}
