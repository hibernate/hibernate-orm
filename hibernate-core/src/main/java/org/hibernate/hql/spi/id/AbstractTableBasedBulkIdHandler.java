/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectValues;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

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

	protected static class ProcessedWhereClause {
		public static final ProcessedWhereClause NO_WHERE_CLAUSE = new ProcessedWhereClause();

		private final String userWhereClauseFragment;
		private final List<ParameterSpecification> idSelectParameterSpecifications;

		private ProcessedWhereClause() {
			this( "", Collections.emptyList() );
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

		final Dialect dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
		final Select select = generateIdSelect( tableAlias, whereClause );

		InsertSelect insert = new InsertSelect( dialect );
		if ( sessionFactory.getSessionFactoryOptions().isCommentsEnabled() ) {
			insert.setComment( "insert-select for " + getTargetedQueryable().getEntityName() + " ids" );
		}
		insert.setTableName( idTableInfo.getQualifiedIdTableName() );
		insert.setSelect( select );
		return insert.toStatementString();
	}

	protected Select generateIdSelect(
			String tableAlias,
			ProcessedWhereClause whereClause) {

		final Dialect dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();

		final Select select = new Select( dialect );
		final SelectValues selectClause = new SelectValues( dialect ).addColumns(
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
		return select;
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
		return "select " + String.join( ", ", persister.getIdentifierColumnNames() ) +
				" from " + idTableInfo.getQualifiedIdTableName();
	}

	protected String generateIdSubselect(Queryable persister, AbstractCollectionPersister cPersister, IdTableInfo idTableInfo) {
		String[] columnNames = getKeyColumnNames( persister, cPersister );
		StringBuilder selectBuilder = new StringBuilder();
		selectBuilder.append( "select " );
		appendJoined( ", ", columnNames, selectBuilder  );
		return selectBuilder.append( " from " )
			.append( idTableInfo.getQualifiedIdTableName() ).toString();
	}

	protected static String[] getKeyColumnNames(Queryable persister, AbstractCollectionPersister cPersister) {
		Type keyType = cPersister.getKeyType();
		String[] columnNames;
		if ( keyType.isComponentType() ) {
			ComponentType componentType = (ComponentType) keyType;
			List<String> columns = new ArrayList<>(componentType.getPropertyNames().length );
			for ( String propertyName : componentType.getPropertyNames() ) {
				Collections.addAll( columns, persister.toColumns( propertyName ) );
			}
			columnNames = columns.toArray( new String[columns.size()] );
		}
		else {
			columnNames = persister.getIdentifierColumnNames();
		}
		return columnNames;
	}

	protected static void appendJoined(String delimiter, String[] parts, StringBuilder sb) {
		sb.append( parts[0] );
		for ( int i = 1; i < parts.length; i++ ) {
			sb.append( delimiter );
			sb.append( parts[i] );
		}
	}

	protected void prepareForUse(Queryable persister, SharedSessionContractImplementor session) {
	}

	protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
	}
}
