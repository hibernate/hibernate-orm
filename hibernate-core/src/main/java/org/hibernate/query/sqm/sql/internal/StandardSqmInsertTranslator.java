/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SqmInsertTranslation;
import org.hibernate.query.sqm.sql.SqmInsertTranslator;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class StandardSqmInsertTranslator
		extends BaseSqmToSqlAstConverter
		implements SqmInsertTranslator, DomainResultCreationState {

	private final List<DomainResult> domainResults = CollectionHelper.arrayList( 10 );

	public StandardSqmInsertTranslator(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
	}

	@Override
	public SqmInsertTranslation translate(SqmInsertStatement sqmStatement) {
		InsertStatement sqlAst;
		if ( sqmStatement instanceof SqmInsertSelectStatement ) {
			sqlAst = visitInsertSelectStatement( (SqmInsertSelectStatement) sqmStatement );
		}
		else {
			sqlAst = visitInsertValuesStatement( (SqmInsertValuesStatement) sqmStatement );
		}
		return new SqmInsertTranslation( sqlAst, getJdbcParamsBySqmParam() );
	}

	@Override
	public CteStatement translate(SqmCteStatement sqmCte) {
		return visitCteStatement( sqmCte );
	}

	@Override
	public InsertStatement visitInsertSelectStatement(SqmInsertSelectStatement sqmStatement) {
		final InsertStatement insertStatement = new InsertStatement();

		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel().getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					false,
					LockMode.WRITE,
					stem -> getSqlAliasBaseGenerator().createSqlAliasBase( stem ),
					getSqlExpressionResolver(),
					() -> predicate -> additionalRestrictions = predicate,
					getCreationContext()
			);

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty()
					|| ! rootTableGroup.getTableGroupJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			getFromClauseIndex().registerTableGroup( rootPath, rootTableGroup );

			insertStatement.setTargetTable( rootTableGroup.getPrimaryTableReference() );

			List<SqmPath> targetPaths = sqmStatement.getInsertionTargetPaths();
			for (SqmPath target : targetPaths) {
				Assignable assignable = (Assignable) target.accept(this);
				insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
			}

			insertStatement.setSourceSelectStatement(
					visitQuerySpec( sqmStatement.getSelectQuerySpec() )
			);

			return insertStatement;
		}
		finally {
			getProcessingStateStack().pop();
		}
	}

	@Override
	public InsertStatement visitInsertValuesStatement(SqmInsertValuesStatement sqmStatement) {
		final InsertStatement insertValuesStatement = new InsertStatement();

		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel().getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					false,
					LockMode.WRITE,
					stem -> getSqlAliasBaseGenerator().createSqlAliasBase( stem ),
					getSqlExpressionResolver(),
					() -> predicate -> additionalRestrictions = predicate,
					getCreationContext()
			);

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty()
					|| ! rootTableGroup.getTableGroupJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			getFromClauseIndex().registerTableGroup( rootPath, rootTableGroup );

			insertValuesStatement.setTargetTable( rootTableGroup.getPrimaryTableReference() );

			List<SqmPath> targetPaths = sqmStatement.getInsertionTargetPaths();
			for (SqmPath target : targetPaths) {
				Assignable assignable = (Assignable) target.accept(this);
				insertValuesStatement.addTargetColumnReferences( assignable.getColumnReferences() );
			}

			List<SqmValues> valuesList = sqmStatement.getValuesList();
			for ( SqmValues sqmValues : valuesList ) {
				insertValuesStatement.getValuesList().add( visitValues( sqmValues ) );
			}

			return insertValuesStatement;
		}
		finally {
			getProcessingStateStack().pop();
		}
	}

	private DomainResultProducer resolveDomainResultProducer(SqmSelection sqmSelection) {
		return (DomainResultProducer) sqmSelection.getSelectableNode().accept( this );
	}

	@Override
	public Void visitSelection(SqmSelection sqmSelection) {
		final DomainResultProducer resultProducer = resolveDomainResultProducer( sqmSelection );

//		if ( getProcessingStateStack().depth() > 1 ) {
//			resultProducer.applySqlSelections( this );
//		}
//		else {

			final DomainResult domainResult = resultProducer.createDomainResult(
					sqmSelection.getAlias(),
					this
			);

			domainResults.add( domainResult );
//		}

		return null;
	}

	@Override
	public Values visitValues(SqmValues sqmValues) {
		Values values = new Values();
		for ( SqmExpression expression : sqmValues.getExpressions() ) {
			values.getExpressions().add( (Expression) expression.accept( this ) );
		}
		return values;
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec, domainResults );
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}
}
