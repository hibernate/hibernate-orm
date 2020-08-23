/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import antlr.RecognitionException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectValues;
import org.hibernate.sql.Update;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Executes HQL bulk updates against a single table, using a subselect
 * against multiple tables to collect ids, which is needed when the
 * where condition of the query touches columns from multiple tables.
 *
 * @author Gavin King
 */
public class IdSubselectUpdateExecutor extends BasicExecutor {

	public IdSubselectUpdateExecutor(HqlSqlWalker walker) {
		super( walker.getFinalFromClause().getFromElement().getQueryable() );

		Dialect dialect = walker.getDialect();
		UpdateStatement updateStatement = (UpdateStatement) walker.getAST();
		List<AssignmentSpecification> assignments = walker.getAssignmentSpecifications();

		String whereClause;
		if ( updateStatement.getWhereClause().getNumberOfChildren() == 0 ) {
			whereClause = "";
		}
		else {
			try {
				SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
				gen.whereClause( updateStatement.getWhereClause() );
				gen.getParseErrorHandler().throwQueryException();
				whereClause = gen.getSQL().substring( 7 );  // strip the " where "
			}
			catch ( RecognitionException e ) {
				throw new HibernateException( "Unable to generate id select for DML operation", e );
			}
		}
		String tableAlias = updateStatement.getFromClause().getFromElement().getTableAlias();
		String idSelect = generateIdSelect( tableAlias, whereClause, dialect, persister );

		String[] tableNames = persister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = persister.getContraintOrderedTableKeyColumnClosure();

		int[] affectedTables =
				IntStream.range( 0, tableNames.length ).filter(
						table -> assignments.stream().anyMatch(
								assign -> assign.affectsTable( tableNames[table] )
						)
				).toArray();
		if ( affectedTables.length > 1 ) {
			throw new AssertionFailure("more than one affected table");
		}
		int affectedTable = affectedTables[0];

		String tableName = tableNames[affectedTable];
		String idColumnNames = String.join( ", ", columnNames[affectedTable] );
		Update update = new Update( dialect ).setTableName( tableName );
		if ( dialect instanceof MySQLDialect) {
			//MySQL needs an extra subselect to hack the query optimizer
			String selectedIdColumns = String.join( ", ", persister.getIdentifierColumnNames() );
			update.setWhere( "(" + idColumnNames + ") in (select " + selectedIdColumns + " from ("  + idSelect + ") as ht_ids)" );
		}
		else {
			update.setWhere( "(" + idColumnNames + ") in ("  + idSelect + ")" );
		}
		for ( AssignmentSpecification assignment: assignments ) {
			update.appendAssignmentFragment( assignment.getSqlAssignmentFragment() );
		}
		sql = update.toStatementString();

		// now collect the parameters from the whole query
		// parameters included in the list
		try {
			SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
			gen.statement( walker.getAST() );
			parameterSpecifications = gen.getCollectedParameters();
		}
		catch ( RecognitionException e ) {
			throw QuerySyntaxException.convert( e );
		}

	}

	//TODO: this is a copy/paste of a method from AbstractTableBasedBulkIdHandler
	private String generateIdSelect(String tableAlias, String whereClause, Dialect dialect, Queryable queryable) {
		Select select = new Select( dialect );
		SelectValues selectClause = new SelectValues( dialect ).addColumns(
				tableAlias,
				queryable.getIdentifierColumnNames(),
				queryable.getIdentifierColumnNames()
		);
		select.setSelectClause( selectClause.render() );

		String rootTableName = queryable.getTableName();
		String fromJoinFragment = queryable.fromJoinFragment( tableAlias, true, false );
		String whereJoinFragment = queryable.whereJoinFragment( tableAlias, true, false );

		select.setFromClause( rootTableName + ' ' + tableAlias + fromJoinFragment );

		if ( whereJoinFragment == null ) {
			whereJoinFragment = "";
		}
		else {
			whereJoinFragment = whereJoinFragment.trim();
			if ( whereJoinFragment.startsWith( "and" ) ) {
				whereJoinFragment = whereJoinFragment.substring( 4 );
			}
		}

		if ( !whereClause.isEmpty() ) {
			if ( whereJoinFragment.length() > 0 ) {
				whereJoinFragment += " and ";
			}
		}
		select.setWhereClause( whereJoinFragment + whereClause );
		return select.toStatementString();
	}
}
