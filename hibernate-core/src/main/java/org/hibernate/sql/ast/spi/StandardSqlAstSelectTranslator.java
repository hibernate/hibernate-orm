/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.Collections;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstSelectTranslator;
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
public class StandardSqlAstSelectTranslator
		extends AbstractSqlAstToJdbcOperationConverter
		implements SqlAstSelectTranslator {

	@SuppressWarnings("WeakerAccess")
	public StandardSqlAstSelectTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public JdbcSelect interpret(QuerySpec querySpec) {
		visitQuerySpec( querySpec );

		return new JdbcSelect(
				getSql(),
				getParameterBinders(),
				new JdbcValuesMappingProducerStandard(
						querySpec.getSelectClause().getSqlSelections(),
						Collections.emptyList()
				),
				getAffectedTableExpressions()
		);
	}

	@Override
	public JdbcSelect interpret(SelectStatement sqlAstSelect) {
		if ( SqlAstTreeLogger.DEBUG_ENABLED ) {
			SqlTreePrinter.print( sqlAstSelect );
		}

		visitQuerySpec( sqlAstSelect.getQuerySpec() );

		return new JdbcSelect(
				getSql(),
				getParameterBinders(),
				new JdbcValuesMappingProducerStandard(
						sqlAstSelect.getQuerySpec().getSelectClause().getSqlSelections(),
						sqlAstSelect.getDomainResultDescriptors()
				),
				getAffectedTableExpressions()
		);
	}

	@Override
	public void visitSelectQuery(SelectStatement selectQuery) {
		visitQuerySpec( selectQuery.getQuerySpec() );
	}
}
