/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Andrea Boriero
 */
public class JoinedSubclassDiscriminatorMappingImpl extends AbstractEntityDiscriminatorMapping {

	private final CaseSearchedExpression caseSearchedExpression;
	private final List<ColumnReference> columnReferences;

	public JoinedSubclassDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			String tableExpression,
			String mappedColumExpression,
			CaseSearchedExpression caseSearchedExpression,
			List<ColumnReference> columnReferences,
			BasicType mappingType) {
		super( entityDescriptor, tableExpression, mappedColumExpression, mappingType );

		this.caseSearchedExpression = caseSearchedExpression;
		this.columnReferences = columnReferences;
	}

	@Override
	protected SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		// need to add the columns of the ids used in the case expression
		columnReferences.forEach(
				columnReference ->
						expressionResolver.resolveSqlSelection(
								columnReference,
								getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
								creationState.getSqlAstCreationState()
										.getCreationContext()
										.getDomainModel()
										.getTypeConfiguration()
						)
		);

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						getMappedColumnExpression(),
						sqlAstProcessingState -> caseSearchedExpression
				),
				getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

}
