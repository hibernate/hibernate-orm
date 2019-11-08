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
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class EntityDiscriminatorMappingImpl extends AbstractEntityDiscriminatorMapping {

	public EntityDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			String tableExpression,
			String mappedColumnExpression,
			BasicType mappingType) {
		super( entityDescriptor, tableExpression, mappedColumnExpression, mappingType );
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
								getMappedColumnExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableReference.getIdentificationVariable(),
								getMappedColumnExpression(),
								getJdbcMapping(),
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}
}
