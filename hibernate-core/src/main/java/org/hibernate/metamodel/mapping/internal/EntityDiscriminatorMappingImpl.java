/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class EntityDiscriminatorMappingImpl extends AbstractEntityDiscriminatorMapping {
	private final NavigableRole navigableRole;

	public EntityDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			String tableExpression,
			String mappedColumnExpression,
			boolean isFormula,
			BasicType mappingType) {
		super( entityDescriptor, tableExpression, mappedColumnExpression, isFormula, mappingType );
		this.navigableRole = entityDescriptor.getNavigableRole().append( EntityDiscriminatorMapping.ROLE_NAME );
	}

	@Override
	protected SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();

		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								getSelectionExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableReference.getIdentificationVariable(),
								this,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				getMappedType().getMappedJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
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
}
