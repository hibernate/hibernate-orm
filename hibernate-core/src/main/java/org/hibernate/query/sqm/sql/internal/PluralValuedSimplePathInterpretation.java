/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartSelectionPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.collection.internal.DetachedCollectionDomainResult;

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
				tableGroup,
				sqmPath instanceof SqmPluralPartSelectionPath<?> pluralPartSelectionPath
						? pluralPartSelectionPath.getSelectedPartNature()
						: null
		);
	}

	private final Expression sqlExpression;
	private final CollectionPart.Nature selectedPartNature;

	private PluralValuedSimplePathInterpretation(
			Expression sqlExpression,
			NavigablePath navigablePath,
			PluralAttributeMapping mapping,
			TableGroup tableGroup,
			CollectionPart.Nature selectedPartNature) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
		this.selectedPartNature = selectedPartNature;
	}

	@Override
	public Expression getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return new DetachedCollectionDomainResult<>(
				getNavigablePath(),
				(PluralAttributeMapping) getExpressionType(),
				resultVariable,
				getTableGroup(),
				selectedPartNature,
				creationState
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException();
	}
}
