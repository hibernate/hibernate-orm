/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * CUBRID-specific {@code extract()}: CUBRID rejects {@code extract(millisecond from <time>)}, so for a
 * {@code TIME} operand {@code extract(second from ...)} returns only whole seconds; otherwise it falls
 * back to {@link Dialect#extractPattern(TemporalUnit)}.
 *
 * @see org.hibernate.dialect.function.OracleExtractFunction
 */
public class CUBRIDExtractFunction extends ExtractFunction {

	// ExtractFunction.dialect is package-private and not visible from this package, so keep our own
	private final Dialect dialect;

	public CUBRIDExtractFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super( dialect, typeConfiguration );
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		new PatternRenderer( extractPattern( sqlAstArguments ) ).render( sqlAppender, sqlAstArguments, walker );
	}

	@SuppressWarnings("deprecation")
	private String extractPattern(List<? extends SqlAstNode> sqlAstArguments) {
		final ExtractUnit field = (ExtractUnit) sqlAstArguments.get( 0 );
		final TemporalUnit unit = field.getUnit();
		if ( unit == SECOND ) {
			final Expression expression = (Expression) sqlAstArguments.get( 1 );
			final JdbcMappingContainer type = expression.getExpressionType();
			final TemporalType temporalType = type != null ? getSqlTemporalType( type ) : null;
			if ( temporalType == TemporalType.TIME ) {
				return "second(?2)";
			}
		}
		return dialect.extractPattern( unit );
	}
}
