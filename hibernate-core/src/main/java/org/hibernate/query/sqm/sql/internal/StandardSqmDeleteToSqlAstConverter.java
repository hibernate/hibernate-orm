/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteInterpretation;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteToSqlAstConverter;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class StandardSqmDeleteToSqlAstConverter
		extends BaseSqmToSqlAstConverter
		implements SimpleSqmDeleteToSqlAstConverter {

	public StandardSqmDeleteToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
	}

	@Override
	public SimpleSqmDeleteInterpretation interpret(SqmDeleteStatement statement) {
		final DeleteStatement deleteStatement = visitDeleteStatement( statement );
		return new SimpleSqmDeleteInterpretation(
				deleteStatement,
				getJdbcParamsBySqmParam()
		);
	}

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement statement) {
		final String entityName = statement.getTarget().getEntityName();
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
			final NavigablePath rootPath = new NavigablePath( entityName );
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					rootPath,
					null,
					JoinType.LEFT,
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
}
