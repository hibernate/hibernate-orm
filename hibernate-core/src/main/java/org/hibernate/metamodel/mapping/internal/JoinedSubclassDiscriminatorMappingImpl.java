/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Andrea Boriero
 */
public class JoinedSubclassDiscriminatorMappingImpl extends AbstractEntityDiscriminatorMapping {
	private final NavigableRole navigableRole;
	private final CaseSearchedExpression caseSearchedExpression;
	private final CaseSearchedExpression caseSearchedExpressionUnderlying;

	public JoinedSubclassDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			String tableExpression,
			String mappedColumnExpression,
			boolean isFormula,
			CaseSearchedExpression caseSearchedExpression,
			List<ColumnReference> columnReferences,
			DiscriminatorType<?> mappingType) {
		super( entityDescriptor, tableExpression, mappedColumnExpression, isFormula, mappingType );

		this.navigableRole = entityDescriptor.getNavigableRole().append( EntityDiscriminatorMapping.ROLE_NAME );
		this.caseSearchedExpression = caseSearchedExpression;
		final CaseSearchedExpression caseSearchedExpressionUnderlying = new CaseSearchedExpression( mappingType.getUnderlyingType() );
		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			caseSearchedExpressionUnderlying.when( whenFragment.getPredicate(), whenFragment.getResult() );
		}
		caseSearchedExpressionUnderlying.otherwise( caseSearchedExpression.getOtherwise() );
		this.caseSearchedExpressionUnderlying = caseSearchedExpressionUnderlying;
	}

	@Override
	protected SqlSelection resolveSqlSelection(
			TableGroup tableGroup,
			boolean underlyingType,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final BasicType<?> type = underlyingType ? getMappedType().getUnderlyingType() : getMappedType();

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						getSelectionExpression(),
						sqlAstProcessingState -> underlyingType ? caseSearchedExpressionUnderlying : caseSearchedExpression
				),
				type.getMappedJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return getEntityDescriptor();
	}

	@Override
	public String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
