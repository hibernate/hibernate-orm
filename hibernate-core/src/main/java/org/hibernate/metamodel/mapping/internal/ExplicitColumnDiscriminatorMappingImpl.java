/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ExplicitColumnDiscriminatorMappingImpl extends AbstractDiscriminatorMapping {
	private final String tableExpression;
	private final String columnName;
	private final String columnFormula;
	private final boolean isPhysical;

	public ExplicitColumnDiscriminatorMappingImpl(
			EntityPersister entityDescriptor,
			DiscriminatorType<?> discriminatorType,
			String tableExpression,
			String columnExpression,
			boolean isFormula,
			boolean isPhysical,
			MappingModelCreationProcess creationProcess) {
		super( discriminatorType.getJdbcMapping(), entityDescriptor, discriminatorType, creationProcess );
		this.tableExpression = tableExpression;
		this.isPhysical = isPhysical;
		if ( isFormula ) {
			columnName = null;
			columnFormula = columnExpression;
		}
		else {
			columnName = columnExpression;
			columnFormula = null;
		}
	}

	@Override
	public Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, tableExpression );
		final String selectionExpression = getSelectionExpression();
		return expressionResolver.resolveSqlExpression(
				createColumnReferenceKey( tableReference, selectionExpression ),
				sqlAstProcessingState -> new ColumnReference(
						tableReference,
						selectionExpression,
						columnFormula != null,
						null,
						null,
						jdbcMappingToUse,
						getSessionFactory()

				)
		);
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getSelectionExpression() {
		return columnName == null ? columnFormula : columnName;
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
	public boolean isFormula() {
		return columnFormula != null;
	}

	@Override
	public boolean isPhysical() {
		return isPhysical;
	}
}
