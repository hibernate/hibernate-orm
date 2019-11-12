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
import org.hibernate.query.sqm.sql.SqmInsertSelectTranslation;
import org.hibernate.query.sqm.sql.SqmInsertSelectTranslator;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;

/**
 * @author Steve Ebersole
 */
public class StandardSqmInsertSelectTranslator
		extends BaseSqmToSqlAstConverter
		implements SqmInsertSelectTranslator {
	public StandardSqmInsertSelectTranslator(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
	}

	@Override
	public SqmInsertSelectTranslation translate(SqmInsertSelectStatement sqmStatement) {
		return new SqmInsertSelectTranslation( visitInsertSelectStatement( sqmStatement ), getJdbcParamsBySqmParam() );
	}

	@Override
	public CteStatement translate(SqmCteStatement sqmCte) {
		return visitCteStatement( sqmCte );
	}

	@Override
	public InsertSelectStatement visitInsertSelectStatement(SqmInsertSelectStatement sqmStatement) {
		final InsertSelectStatement insertSelectStatement = new InsertSelectStatement();

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

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty()
					|| rootTableGroup.getTableGroupJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			getFromClauseIndex().registerTableGroup( rootPath, rootTableGroup );

			insertSelectStatement.setTargetTable( rootTableGroup.getPrimaryTableReference() );

			insertSelectStatement.setSourceSelectStatement(
					visitQuerySpec( sqmStatement.getSelectQuerySpec() )
			);

			return insertSelectStatement;
		}
		finally {
			getProcessingStateStack().pop();
		}
	}
}
