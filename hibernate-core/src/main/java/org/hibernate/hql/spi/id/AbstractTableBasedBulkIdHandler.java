/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectValues;

import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * Convenience base class for {@link MultiTableBulkIdStrategy.UpdateHandler}
 * and {@link MultiTableBulkIdStrategy.DeleteHandler} implementations through
 * {@link TableBasedUpdateHandlerImpl} and {@link TableBasedDeleteHandlerImpl} respectively.
 * <p/>
 * Mainly supports common activities like:<ul>
 *     <li>processing the original {@code WHERE} clause (if one) - {@link #processWhereClause}</li>
 *     <li>generating the proper {@code SELECT} clause for the id-table insert - {@link #generateIdInsertSelect}</li>
 *     <li>generating the sub-select from the id-table - {@link #generateIdSubselect}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedBulkIdHandler {
	private final SessionFactoryImplementor sessionFactory;
	private final HqlSqlWalker walker;

	public AbstractTableBasedBulkIdHandler(
			SessionFactoryImplementor sessionFactory,
			HqlSqlWalker walker) {
		this.sessionFactory = sessionFactory;
		this.walker = walker;
	}

	protected SessionFactoryImplementor factory() {
		return sessionFactory;
	}

	protected HqlSqlWalker walker() {
		return walker;
	}

	public abstract Queryable getTargetedQueryable();

	protected JDBCException convert(SQLException e, String message, String sql) {
		throw factory().getSQLExceptionHelper().convert( e, message, sql );
	}

	protected static class ProcessedWhereClause {
		public static final ProcessedWhereClause NO_WHERE_CLAUSE = new ProcessedWhereClause();

		private final String userWhereClauseFragment;
		private final List<ParameterSpecification> idSelectParameterSpecifications;

		private ProcessedWhereClause() {
			this( "", Collections.<ParameterSpecification>emptyList() );
		}

		public ProcessedWhereClause(String userWhereClauseFragment, List<ParameterSpecification> idSelectParameterSpecifications) {
			this.userWhereClauseFragment = userWhereClauseFragment;
			this.idSelectParameterSpecifications = idSelectParameterSpecifications;
		}

		public String getUserWhereClauseFragment() {
			return userWhereClauseFragment;
		}

		public List<ParameterSpecification> getIdSelectParameterSpecifications() {
			return idSelectParameterSpecifications;
		}
	}

	/**
	 * Interprets the {@code WHERE} clause from the user-defined update/delete  query
	 *
	 * @param whereClause The user-defined {@code WHERE} clause
	 *
	 * @return The bulk-id-ready {@code WHERE} clause representation
	 */
	@SuppressWarnings("unchecked")
	protected ProcessedWhereClause processWhereClause(AST whereClause) {
		if ( whereClause.getNumberOfChildren() != 0 ) {
			// If a where clause was specified in the update/delete query, use it to limit the
			// ids that will be returned and inserted into the id table...
			try {
				SqlGenerator sqlGenerator = new SqlGenerator( sessionFactory );
				sqlGenerator.whereClause( whereClause );
				String userWhereClause = sqlGenerator.getSQL().substring( 7 );  // strip the " where "
				List<ParameterSpecification> idSelectParameterSpecifications = sqlGenerator.getCollectedParameters();

				return new ProcessedWhereClause( userWhereClause, idSelectParameterSpecifications );
			}
			catch ( RecognitionException e ) {
				throw new HibernateException( "Unable to generate id select for DML operation", e );
			}
		}
		else {
			return ProcessedWhereClause.NO_WHERE_CLAUSE;
		}
	}

	/**
	 * Generate the {@code INSERT}-{@code SELECT} statement for holding matching ids.  This is the
	 * {@code INSERT} used to populate the bulk-id table with ids matching the restrictions defined in the
	 * original {@code WHERE} clause
	 *
	 * @param tableAlias The table alias to use for the entity
	 * @param whereClause The processed representation for the user-defined {@code WHERE} clause.
	 *
	 * @return The {@code INSERT}-{@code SELECT} for populating the bulk-id table.
	 */
	protected String generateIdInsertSelect(
			String tableAlias,
			IdTableInfo idTableInfo,
			ProcessedWhereClause whereClause) {

		Select select = new Select( sessionFactory.getDialect() );
		SelectValues selectClause = new SelectValues( sessionFactory.getDialect() ).addColumns(
				tableAlias,
				getTargetedQueryable().getIdentifierColumnNames(),
				getTargetedQueryable().getIdentifierColumnNames()
		);
		addAnyExtraIdSelectValues( selectClause );
		select.setSelectClause( selectClause.render() );

		String rootTableName = getTargetedQueryable().getTableName();
		String fromJoinFragment = getTargetedQueryable().fromJoinFragment( tableAlias, true, false );
		String whereJoinFragment = getTargetedQueryable().whereJoinFragment( tableAlias, true, false );

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

		if ( whereClause.getUserWhereClauseFragment().length() > 0 ) {
			if ( whereJoinFragment.length() > 0 ) {
				whereJoinFragment += " and ";
			}
		}
		select.setWhereClause( whereJoinFragment + whereClause.getUserWhereClauseFragment() );

		InsertSelect insert = new InsertSelect( sessionFactory.getDialect() );
		if ( sessionFactory.getSettings().isCommentsEnabled() ) {
			insert.setComment( "insert-select for " + getTargetedQueryable().getEntityName() + " ids" );
		}
		insert.setTableName( idTableInfo.getQualifiedIdTableName() );
		insert.setSelect( select );
		return insert.toStatementString();
	}

	/**
	 * Used from {@link #generateIdInsertSelect} to allow subclasses to define any extra
	 * values to be selected (and therefore stored into the bulk-id table).  Used to store
	 * session identifier, e.g.
	 *
	 * @param selectClause The SelectValues that defines the select clause of the insert statement.
	 */
	protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
	}

	protected String generateIdSubselect(Queryable persister, IdTableInfo idTableInfo) {
		return "select " + StringHelper.join( ", ", persister.getIdentifierColumnNames() ) +
				" from " + idTableInfo.getQualifiedIdTableName();
	}

	protected void prepareForUse(Queryable persister, SessionImplementor session) {
	}

	protected void releaseFromUse(Queryable persister, SessionImplementor session) {
	}
}
