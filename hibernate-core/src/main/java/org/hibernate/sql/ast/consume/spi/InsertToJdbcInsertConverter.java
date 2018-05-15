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
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.sql.ast.tree.spi.InsertStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class InsertToJdbcInsertConverter
		extends AbstractSqlAstToJdbcOperationConverter
		implements SqlMutationToJdbcMutationConverter {

	public static JdbcInsert createJdbcInsert(
			InsertStatement sqlAst,
			SessionFactoryImplementor sessionFactory) {
		final InsertToJdbcInsertConverter walker = new InsertToJdbcInsertConverter( sessionFactory );
		walker.processStatement( sqlAst );
		return new JdbcInsert() {
			@Override
			public boolean isKeyGenerationEnabled() {
				return false;
			}

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

	public InsertToJdbcInsertConverter(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	private void processStatement(InsertStatement sqlAst) {
		appendSql( "insert into " );

		final PhysicalTable targetTable = (PhysicalTable) sqlAst.getTargetTable().getTable();
		final String tableName = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getQualifiedObjectNameFormatter()
				.format(
						targetTable.getQualifiedTableName(),
						getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
				);

		appendSql( tableName );

		// todo (6.0) : need to render the target column list
		// 		for now we do not render


		appendSql( " (" );

		boolean firstPass = true;
		for ( ColumnReference columnReference : sqlAst.getTargetColumnReferences() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				appendSql( ", " );
			}

			visitColumnReference( columnReference );
		}

		appendSql( ") values (" );

		firstPass = true;
		for ( Expression expression : sqlAst.getValues() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				appendSql( ", " );
			}

			expression.accept( this );
		}

		appendSql( ")" );
	}

}
