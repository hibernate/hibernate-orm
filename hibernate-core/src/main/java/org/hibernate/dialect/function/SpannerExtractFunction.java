/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import jakarta.persistence.TemporalType;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 *  uses unix_seconds function for EPOCH unit
 */
public class SpannerExtractFunction extends ExtractFunction {
	public SpannerExtractFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super( dialect, typeConfiguration );
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
		var field = (ExtractUnit) sqlAstArguments.get( 0 );
		var expression = (Expression) sqlAstArguments.get( 1 );
		var type = expression.getExpressionType();
		var temporalType = type != null ? getSqlTemporalType( type ) : null;
		if ( field.getUnit() == TemporalUnit.EPOCH ) {
			return temporalType == TemporalType.DATE
					? "unix_seconds(timestamp(?2))"
					: "unix_seconds(?2)";
		}
		return dialect.extractPattern( field.getUnit() );
	}
}
