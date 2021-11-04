/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Andrea Boriero
 */
public class CaseStatementDiscriminatorMappingImpl extends AbstractDiscriminatorMapping {

	private final LinkedHashMap<String,TableDiscriminatorDetails> tableDiscriminatorDetailsMap = new LinkedHashMap<>();

	public CaseStatementDiscriminatorMappingImpl(
			JoinedSubclassEntityPersister entityDescriptor,
			String[] tableNames,
			int[] notNullColumnTableNumbers,
			String[] notNullColumnNames,
			String[] discriminatorValues,
			Map<String,String> subEntityNameByTableName,
			DiscriminatorType<?> incomingDiscriminatorType,
			MappingModelCreationProcess creationProcess) {
		super(
				incomingDiscriminatorType.getUnderlyingType().getJdbcMapping(),
				entityDescriptor,
				incomingDiscriminatorType,
				creationProcess
		);

		for ( int i = 0; i < discriminatorValues.length; i++ ) {
			final String tableName = tableNames[notNullColumnTableNumbers[i]];
			final String subEntityName = subEntityNameByTableName.get( tableName );
			final String oneSubEntityColumn = notNullColumnNames[i];

			final String rawDiscriminatorValue = discriminatorValues[i];
			final Object discriminatorValue = getUnderlyingJdbcMappingType().getJavaTypeDescriptor().wrap( rawDiscriminatorValue, null );

			tableDiscriminatorDetailsMap.put(
					tableName,
					new TableDiscriminatorDetails(
							tableName,
							oneSubEntityColumn,
							discriminatorValue,
							subEntityName
					)
			);
		}
	}

	@Override
	public BasicFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);
		// Since the expression is lazy, based on the available table reference joins,
		// we need to force the initialization in case this is a fetch
		tableDiscriminatorDetailsMap.forEach(
				(tableName, tableDiscriminatorDetails) -> tableGroup.getTableReference(
						fetchablePath,
						tableName,
						false,
						true
				)
		);
		return super.generateFetch( fetchParent, fetchablePath, fetchTiming, selected, resultVariable, creationState );
	}

	@Override
	public Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		return expressionResolver.resolveSqlExpression(
				createColumnReferenceKey( tableGroup.getPrimaryTableReference(), getSelectionExpression() ),
				sqlAstProcessingState -> createCaseSearchedExpression( tableGroup )
		);
	}

	private Expression createCaseSearchedExpression(TableGroup entityTableGroup) {
		return new SelfRenderingExpression() {
			CaseSearchedExpression caseSearchedExpression;

			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> walker,
					SessionFactoryImplementor sessionFactory) {
				if ( caseSearchedExpression == null ) {
					// todo (6.0): possible optimization is to omit cases for table reference joins, that touch a super class, where a subclass is inner joined due to pruning
					caseSearchedExpression = new CaseSearchedExpression( CaseStatementDiscriminatorMappingImpl.this );
					tableDiscriminatorDetailsMap.forEach(
							(tableName, tableDiscriminatorDetails) -> {
								final TableReference tableReference = entityTableGroup.getTableReference(
										entityTableGroup.getNavigablePath(),
										tableName,
										false,
										false
								);

								if ( tableReference == null ) {
									// assume this is because it is a table that is not part of the processing entity's sub-hierarchy
									return;
								}

								final Predicate predicate = new NullnessPredicate(
										new ColumnReference(
												tableReference,
												tableDiscriminatorDetails.getCheckColumnName(),
												false,
												null,
												null,
												getJdbcMapping(),
												getSessionFactory()
										),
										true
								);

								caseSearchedExpression.when(
										predicate,
										new QueryLiteral<>(
												tableDiscriminatorDetails.getDiscriminatorValue(),
												getUnderlyingJdbcMappingType()
										)
								);
							}
					);
				}
				caseSearchedExpression.accept( walker );
			}

			@Override
			public JdbcMappingContainer getExpressionType() {
				return CaseStatementDiscriminatorMappingImpl.this;
			}
		};
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
	public String getContainingTableExpression() {
		throw new UnsupportedOperationException();
//		// this *should* only be used to create the sql-expression key, so just
//		// using the primary table expr should be fine
//		return entityDescriptor.getRootTableName();
	}

	@Override
	public String getSelectionExpression() {
		// this *should* only be used to create the sql-expression key, so just
		// using the ROLE_NAME should be fine
		return ROLE_NAME;
	}


	@Override
	public boolean isFormula() {
		return false;
	}

	private static class TableDiscriminatorDetails {
		private final String tableName;
		private final String checkColumnName;
		private final Object discriminatorValue;
		private final String subclassEntityName;

		public TableDiscriminatorDetails(String tableName, String checkColumnName, Object discriminatorValue, String subclassEntityName) {
			this.tableName = tableName;
			this.checkColumnName = checkColumnName;
			this.discriminatorValue = discriminatorValue;
			this.subclassEntityName = subclassEntityName;
		}

		String getTableExpression() {
			return tableName;
		}

		Object getDiscriminatorValue() {
			return discriminatorValue;
		}

		String getSubclassEntityName() {
			return subclassEntityName;
		}

		String getCheckColumnName() {
			return checkColumnName;
		}
	}

}
