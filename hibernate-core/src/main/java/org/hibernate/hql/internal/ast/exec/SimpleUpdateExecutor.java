/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import antlr.RecognitionException;
import org.hibernate.AssertionFailure;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.SqlGenerator;

/**
 * Executes HQL bulk updates against a single table, where the
 * query only touches columns from the table it's updating, and
 * so we don't need to use a subselect.
 *
 * @author Gavin King
 */
public class SimpleUpdateExecutor extends BasicExecutor {
	public SimpleUpdateExecutor(HqlSqlWalker walker) {
		super( walker.getFinalFromClause().getFromElement().getQueryable() );

		if ( persister.isMultiTable() && walker.getQuerySpaces().size() > 1 ) {
			throw new AssertionFailure("not a simple update");
		}

		try {
			SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
			gen.statement( walker.getAST() );
			gen.getParseErrorHandler().throwQueryException();
			// workaround for a problem where HqlSqlWalker actually generates
			// broken SQL with undefined aliases in the where clause, because
			// that is what MultiTableUpdateExecutor is expecting to get
			String alias = walker.getFinalFromClause().getFromElement().getTableAlias();
			sql = gen.getSQL().replace( alias + ".", "" );
			parameterSpecifications = gen.getCollectedParameters();
		}
		catch ( RecognitionException e ) {
			throw QuerySyntaxException.convert( e );
		}
	}
}
