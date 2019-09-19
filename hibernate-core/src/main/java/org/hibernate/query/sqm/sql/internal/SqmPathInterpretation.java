/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Interpretation of a {@link SqmPath} as part of the translation to SQL AST
 *
 * @see org.hibernate.query.sqm.sql.SqmToSqlAstConverter
 * @see #getInterpretedSqmPath
 *
 * @author Steve Ebersole
 */
public interface SqmPathInterpretation<T> extends Expression, DomainResultProducer<T> {
	default NavigablePath getNavigablePath() {
		return getInterpretedSqmPath().getNavigablePath();
	}

	SqmPath<T> getInterpretedSqmPath();

	@Override
	ModelPart getExpressionType();
}
