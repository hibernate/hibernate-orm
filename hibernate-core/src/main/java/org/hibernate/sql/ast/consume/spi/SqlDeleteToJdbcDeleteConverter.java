/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.SqlAstDeleteDescriptor;
import org.hibernate.sql.ast.tree.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class SqlDeleteToJdbcDeleteConverter
		extends AbstractSqlAstToJdbcOperationConverter
		implements SqlMutationToJdbcMutationConverter {

	public static JdbcDelete interpret(
			SqlAstDeleteDescriptor sqlAstDescriptor,
			SessionFactoryImplementor sessionFactory) {
		return interpret( sqlAstDescriptor.getSqlAstStatement(), sessionFactory );
	}

	public static JdbcDelete interpret(
			DeleteStatement sqlAst,
			SessionFactoryImplementor sessionFactory) {
		final SqlDeleteToJdbcDeleteConverter walker = new SqlDeleteToJdbcDeleteConverter( sessionFactory );

		walker.processDeleteStatement( sqlAst );

		return new JdbcDelete() {
			@Override
			public String getSql() {
				return walker.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return walker.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return walker.getAffectedTableNames();
			}
		};
	}

	public static JdbcDelete interpret(
			CteStatement sqlAst,
			SessionFactoryImplementor sessionFactory) {
		assert sqlAst.getCteConsumer() instanceof DeleteStatement;

		final SqlDeleteToJdbcDeleteConverter walker = new SqlDeleteToJdbcDeleteConverter( sessionFactory );

		walker.getSqlAppender().appendSql( "with " );
		walker.getSqlAppender().appendSql( sqlAst.getCteLabel() );

		walker.getSqlAppender().appendSql( " (" );

		String separator = "";
		for ( Column cteColumn : sqlAst.getCteTable().getColumns() ) {
			walker.getSqlAppender().appendSql( separator );
			walker.getSqlAppender().appendSql( cteColumn.render() );
			separator = ", ";
		}

		walker.getSqlAppender().appendSql( ") as (" );

		walker.visitQuerySpec( sqlAst.getCteDefinition() );

		walker.getSqlAppender().appendSql( ") " );

		walker.processDeleteStatement( (DeleteStatement) sqlAst.getCteConsumer() );

		return new JdbcDelete() {
			@Override
			public String getSql() {
				return walker.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return walker.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return walker.getAffectedTableNames();
			}
		};
	}

	public SqlDeleteToJdbcDeleteConverter(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	private void processDeleteStatement(DeleteStatement deleteStatement) {
		appendSql( "delete " );

		final PhysicalTable targetTable = (PhysicalTable) deleteStatement.getTargetTable().getTable();
		final String tableName = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getQualifiedObjectNameFormatter()
				.format(
						targetTable.getQualifiedTableName(),
						getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
				);

		appendSql( tableName );

		if ( deleteStatement.getRestriction() != null ) {
			getClauseStack().push( Clause.WHERE );
			try {
				appendSql( " where " );
				deleteStatement.getRestriction().accept( this );
			}
			finally {
				getClauseStack().pop();
			}
		}
	}

}
