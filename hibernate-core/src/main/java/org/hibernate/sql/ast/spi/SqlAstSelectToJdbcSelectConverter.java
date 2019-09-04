/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.Collections;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlTreePrinter;
import org.hibernate.sql.ast.tree.SqlAstTreeLogger;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.JdbcValuesMappingProducerStandard;

/**
 * The final phase of query translation.  Here we take the SQL-AST an
 * "interpretation".  For a select query, that means an instance of
 * {@link JdbcSelect}.
 *
 * @author Steve Ebersole
 */
public class SqlAstSelectToJdbcSelectConverter extends AbstractSqlAstToJdbcOperationConverter implements SqlSelectAstWalker {
	/**
	 * Perform interpretation of a SQL AST QuerySpec
	 *
	 * @return The interpretation result
	 */
	public static JdbcSelect interpret(QuerySpec querySpec, SessionFactoryImplementor sessionFactory) {
		final SqlAstSelectToJdbcSelectConverter walker = new SqlAstSelectToJdbcSelectConverter( sessionFactory );
		walker.visitQuerySpec( querySpec );
		return new JdbcSelect(
				walker.getSql(),
				walker.getParameterBinders(),
				new JdbcValuesMappingProducerStandard(
						querySpec.getSelectClause().getSqlSelections(),
						Collections.emptyList()
				),
				walker.getAffectedTableNames()
		);
	}

	public static JdbcSelect interpret(SelectStatement sqlSelectPlan, SessionFactoryImplementor sessionFactory) {
		if ( SqlAstTreeLogger.DEBUG_ENABLED ) {
			SqlTreePrinter.print( sqlSelectPlan );
		}

		final SqlAstSelectToJdbcSelectConverter walker = new SqlAstSelectToJdbcSelectConverter( sessionFactory );

		walker.visitSelectQuery( sqlSelectPlan );
		return new JdbcSelect(
				walker.getSql(),
				walker.getParameterBinders(),
				new JdbcValuesMappingProducerStandard(
						sqlSelectPlan.getQuerySpec().getSelectClause().getSqlSelections(),
						sqlSelectPlan.getDomainResultDescriptors()
				),
				sqlSelectPlan.getAffectedTableExpressions()
		);
	}

	private SqlAstSelectToJdbcSelectConverter(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public void visitSelectQuery(SelectStatement selectQuery) {
		visitQuerySpec( selectQuery.getQuerySpec() );
	}
}
