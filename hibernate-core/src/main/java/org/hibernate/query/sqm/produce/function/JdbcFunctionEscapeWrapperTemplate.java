/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.List;
import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmJdbcFunctionEscapeWrapper;

/**
 * Acts as a wrapper to another SqmFunctionTemplate - upon rendering uses the
 * standard JDBC escape sequence (i.e. `{fn blah}`) when rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class JdbcFunctionEscapeWrapperTemplate
		extends AbstractSqmFunctionTemplate {
	private final SqmFunctionTemplate wrapped;

	public JdbcFunctionEscapeWrapperTemplate(SqmFunctionTemplate wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		final SqmExpression wrappedSqmExpression = wrapped.makeSqmFunctionExpression( arguments, impliedResultType );
		if ( !SqmFunction.class.isInstance( wrappedSqmExpression ) ) {
			throw new SqmProductionException(
					String.format(
							Locale.ROOT,
							"Expected expression to wrap in a JDBC escape wrapper to be a %s, but was %s",
							SqmFunction.class.getName(),
							wrappedSqmExpression.asLoggableText()
					)
			);
		}
		return new SqmJdbcFunctionEscapeWrapper( (SqmFunction) wrappedSqmExpression );
	}
}
