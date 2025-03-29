/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Andrea Boriero
 */
public class PluralValuedSimplePathInterpretation<T> extends AbstractSqmPathInterpretation<T> {

	public static SqmPathInterpretation<?> from(SqmPluralValuedSimplePath<?> sqmPath, SqmToSqlAstConverter converter) {
		final TableGroup tableGroup = converter.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );

		final PluralAttributeMapping mapping = (PluralAttributeMapping) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		return new PluralValuedSimplePathInterpretation<>(
				null,
				sqmPath.getNavigablePath(),
				mapping,
				tableGroup
		);
	}

	private final Expression sqlExpression;

	private PluralValuedSimplePathInterpretation(
			Expression sqlExpression,
			NavigablePath navigablePath,
			PluralAttributeMapping mapping,
			TableGroup tableGroup) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	@Override
	public Expression getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		// This is only invoked when a plural attribute is a top level select, order by or group by item
		// in which case we have to produce results for the element
		return ( (PluralAttributeMapping) getExpressionType() ).getElementDescriptor().createDomainResult(
				getNavigablePath().append( CollectionPart.Nature.ELEMENT.getName() ),
				getTableGroup(),
				resultVariable,
				creationState
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException();
	}
}
