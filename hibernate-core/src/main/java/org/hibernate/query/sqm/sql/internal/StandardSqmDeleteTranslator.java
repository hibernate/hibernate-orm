/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslation;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslator;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.JdbcDelete;

/**
 * @author Steve Ebersole
 */
public class StandardSqmDeleteTranslator
		extends BaseSqmToSqlAstConverter
		implements SimpleSqmDeleteTranslator {

	public static JdbcDelete translate(
			SqmDeleteStatement statement,
			SessionFactoryImplementor factory) {
		return null;
	}

	public StandardSqmDeleteTranslator(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
	}

	@Override
	public SimpleSqmDeleteTranslation translate(SqmDeleteStatement statement) {
		SqlAstProcessingStateImpl processingState = new SqlAstProcessingStateImpl(
				null,
				this,
				getCurrentClauseStack()::getCurrent
		);

		getProcessingStateStack().push( processingState );

		final DeleteStatement deleteStatement = visitDeleteStatement( statement );

		return new SimpleSqmDeleteTranslation(
				deleteStatement,
				getJdbcParamsBySqmParam(),
				processingState.getSqlExpressionResolver(),
				getFromClauseAccess()
		);
	}

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement statement) {
		final String entityName = statement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel().getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		try {
			final NavigablePath rootPath = statement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					rootPath,
					null,
					false,
					LockMode.WRITE,
					stem -> getSqlAliasBaseGenerator().createSqlAliasBase( stem ),
					getSqlExpressionResolver(),
					() -> predicate -> additionalRestrictions = predicate,
					getCreationContext()
			);
			getFromClauseIndex().registerTableGroup( rootPath, rootTableGroup );

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM DELETE" );
			}

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = statement.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				getCurrentClauseStack().push( Clause.WHERE );
				try {
					suppliedPredicate = (Predicate) whereClause.getPredicate().accept( this );
				}
				finally {
					getCurrentClauseStack().pop();
				}
			}

			return new DeleteStatement(
					rootTableGroup.getPrimaryTableReference(),
					SqlAstTreeHelper.combinePredicates( suppliedPredicate, additionalRestrictions )
			);
		}
		finally {
			getProcessingStateStack().pop();
		}
	}

	@Override
	public CteStatement translate(SqmCteStatement sqmCte) {
		visitCteStatement( sqmCte );
		return null;
	}
}
