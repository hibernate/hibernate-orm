/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Andrea Boriero
 */
public class JoinedSubclassDiscriminatorMappingImpl extends EntityDiscriminatorMappingImpl {

	private final CaseSearchedExpression caseSearchedExpression;

	public JoinedSubclassDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			String tableExpression,
			CaseSearchedExpression caseSearchedExpression,
			BasicType mappingType) {
		super( entityDescriptor, tableExpression, mappingType );
		this.caseSearchedExpression = caseSearchedExpression;
	}


	@Override
	protected SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();

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
