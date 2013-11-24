/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.spi;

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
import org.hibernate.mapping.Table;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectValues;

import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedBulkIdHandler {
	private final SessionFactoryImplementor sessionFactory;
	private final HqlSqlWalker walker;

	private final String catalog;
	private final String schema;

	public AbstractTableBasedBulkIdHandler(
			SessionFactoryImplementor sessionFactory,
			HqlSqlWalker walker,
			String catalog,
			String schema) {
		this.sessionFactory = sessionFactory;
		this.walker = walker;
		this.catalog = catalog;
		this.schema = schema;
	}

	protected SessionFactoryImplementor factory() {
		return sessionFactory;
	}

	protected HqlSqlWalker walker() {
		return walker;
	}

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

	@SuppressWarnings("unchecked")
	protected ProcessedWhereClause processWhereClause(AST whereClause) {
		if ( whereClause.getNumberOfChildren() != 0 ) {
			// If a where clause was specified in the update/delete query, use it to limit the
			// returned ids here...
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

	protected String generateIdInsertSelect(Queryable persister, String tableAlias, ProcessedWhereClause whereClause) {
		Select select = new Select( sessionFactory.getDialect() );
		SelectValues selectClause = new SelectValues( sessionFactory.getDialect() )
				.addColumns( tableAlias, persister.getIdentifierColumnNames(), persister.getIdentifierColumnNames() );
		addAnyExtraIdSelectValues( selectClause );
		select.setSelectClause( selectClause.render() );

		String rootTableName = persister.getTableName();
		String fromJoinFragment = persister.fromJoinFragment( tableAlias, true, false );
		String whereJoinFragment = persister.whereJoinFragment( tableAlias, true, false );

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
			insert.setComment( "insert-select for " + persister.getEntityName() + " ids" );
		}
		insert.setTableName( determineIdTableName( persister ) );
		insert.setSelect( select );
		return insert.toStatementString();
	}

	protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
	}

	protected String determineIdTableName(Queryable persister) {
		// todo : use the identifier/name qualifier service once we pull that over to master
		return Table.qualify( catalog, schema, persister.getTemporaryIdTableName() );
	}

	protected String generateIdSubselect(Queryable persister) {
		return "select " + StringHelper.join( ", ", persister.getIdentifierColumnNames() ) +
				" from " + determineIdTableName( persister );
	}

	protected void prepareForUse(Queryable persister, SessionImplementor session) {
	}

	protected void releaseFromUse(Queryable persister, SessionImplementor session) {
	}
}
