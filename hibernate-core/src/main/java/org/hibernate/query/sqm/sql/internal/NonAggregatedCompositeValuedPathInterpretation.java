/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Andrea Boriero
 */
public class NonAggregatedCompositeValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> {

	public static <T> NonAggregatedCompositeValuedPathInterpretation<T> from(
			NonAggregatedCompositeSimplePath<T> sqmPath,
			SqmToSqlAstConverter converter,
			SqmToSqlAstConverter sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final NonAggregatedIdentifierMappingImpl mapping = (NonAggregatedIdentifierMappingImpl) tableGroup.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );

		return new NonAggregatedCompositeValuedPathInterpretation<>(
				mapping.toSqlExpression(
						tableGroup,
						converter.getCurrentClauseStack().getCurrent(),
						converter,
						converter
				),
				sqmPath,
				mapping,
				tableGroup
		);
	}

	private final SqlTuple sqlExpression;

	public NonAggregatedCompositeValuedPathInterpretation(
			SqlTuple sqlExpression,
			SqmPath<T> sqmPath,
			ModelPart mapping,
			TableGroup tableGroup) {
		super( sqmPath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	public SqlTuple getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

}
