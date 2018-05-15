/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.NonSelectSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteToSqlAstConverterMultiTable extends BaseSqmToSqlAstConverter {

	private final QuerySpec idTableSelect;
	private final EntityTypeDescriptor entityDescriptor;
	private final EntityTableGroup entityTableGroup;
	private final NonSelectSqlExpressionResolver expressionResolver;

	public static List<SqlAstUpdateDescriptor> interpret(
			SqmDeleteStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstProducerContext producerContext) {

		final SqmDeleteToSqlAstConverterMultiTable walker = new SqmDeleteToSqlAstConverterMultiTable(
				sqmStatement,
				idTableSelect,
				queryOptions,
				producerContext
		);

		walker.visitDeleteStatement( sqmStatement );

		// todo (6.0) : finish this code
		// see SqmUpdateToSqlAstConverterMultiTable#interpret
//		return walker.updateStatementBuilderMap.entrySet().stream()
//				.map( entry -> entry.getValue().createUpdateDescriptor() )
//				.collect( Collectors.toList() );
		throw new NotYetImplementedFor6Exception();
	}

	public SqmDeleteToSqlAstConverterMultiTable(
			SqmDeleteStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstProducerContext producerContext) {
		super( producerContext, queryOptions );
		this.idTableSelect = idTableSelect;

		this.entityDescriptor = sqmStatement.getEntityFromElement()
				.getNavigableReference()
				.getExpressableType()
				.getEntityDescriptor();

		final NavigablePath path = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityTableGroup = entityDescriptor.createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return sqmStatement.getEntityFromElement().getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return sqmStatement.getEntityFromElement().getIdentificationVariable();
					}

					@Override
					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
						return sqmStatement.getEntityFromElement().getIntrinsicSubclassEntityMetadata();
					}

					@Override
					public NavigablePath getNavigablePath() {
						return path;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
					}

					@Override
					public QuerySpec getQuerySpec() {
						return null;
					}

					@Override
					public TableSpace getTableSpace() {
						return null;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return getSqlAliasBaseManager();
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return JoinType.INNER;
					}

					@Override
					public LockOptions getLockOptions() {
						return queryOptions.getLockOptions();
					}
				}
		);

		getFromClauseIndex().crossReference( sqmStatement.getEntityFromElement(), entityTableGroup );

		this.expressionResolver = new NonSelectSqlExpressionResolver(
				getSessionFactory(),
				() -> getQuerySpecStack().getCurrent(),
				this::normalizeSqlExpression,
				this::collectSelection
		);
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return expressionResolver;
	}

	@Override
	protected SqlExpressionResolver getSqlExpressionResolver() {
		return expressionResolver;
	}
}
