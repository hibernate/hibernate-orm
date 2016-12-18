/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectValues;

import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class AbstractCTEBasedBulkIdHandler {
	private final SessionFactoryImplementor sessionFactory;
	private final HqlSqlWalker walker;
	private final String catalog;
	private final String schema;

	public AbstractCTEBasedBulkIdHandler(
			SessionFactoryImplementor sessionFactory, HqlSqlWalker walker,
			String catalog, String schema) {
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
		throw factory().getSQLExceptionHelper().convert(e, message, sql);
	}

	@SuppressWarnings("unchecked")
	protected ProcessedWhereClause processWhereClause(AST whereClause) {
		if (whereClause.getNumberOfChildren() != 0) {
			// If a where clause was specified in the update/delete query, use
			// it to limit the
			// returned ids here...
			try {
				SqlGenerator sqlGenerator = new SqlGenerator(sessionFactory);
				sqlGenerator.whereClause(whereClause);
				String userWhereClause = sqlGenerator.getSQL().substring(7); // strip
				// the
				// " where "
				List<ParameterSpecification> idSelectParameterSpecifications = sqlGenerator
						.getCollectedParameters();
				return new ProcessedWhereClause(userWhereClause,
						idSelectParameterSpecifications);
			}
			catch (RecognitionException e) {
				throw new HibernateException(
						"Unable to generate id select for DML operation", e);
			}
		}
		else {
			return ProcessedWhereClause.NO_WHERE_CLAUSE;
		}
	}

	protected String generateIdCteSelect(Queryable persister, String idTableName, List<Object[]> selectResult) {
		CTE cte = new CTE(idTableName);
		cte.addColumns(persister.getIdentifierColumnNames());
		cte.setSelectResult(selectResult);
		return cte.toStatementString();
	}

	protected String generateIdSelect(Queryable persister, String tableAlias, ProcessedWhereClause whereClause) {
		Select select = new Select(sessionFactory.getDialect());
		SelectValues selectClause = new SelectValues(
				sessionFactory.getDialect()).addColumns(tableAlias,
				persister.getIdentifierColumnNames(),
				persister.getIdentifierColumnNames());
		addAnyExtraIdSelectValues(selectClause);
		select.setSelectClause(selectClause.render());
		String rootTableName = persister.getTableName();
		String fromJoinFragment = persister.fromJoinFragment(tableAlias, true,
				false);
		String whereJoinFragment = persister.whereJoinFragment(tableAlias,
				true, false);
		select.setFromClause(rootTableName + ' ' + tableAlias
				+ fromJoinFragment);
		if (whereJoinFragment == null) {
			whereJoinFragment = "";
		}
		else {
			whereJoinFragment = whereJoinFragment.trim();
			if (whereJoinFragment.startsWith("and")) {
				whereJoinFragment = whereJoinFragment.substring(4);
			}
		}
		if (whereClause.getUserWhereClauseFragment().length() > 0) {
			if (whereJoinFragment.length() > 0) {
				whereJoinFragment += " and ";
			}
		}
		select.setWhereClause(whereJoinFragment
				+ whereClause.getUserWhereClauseFragment());
		return select.toStatementString();
	}

	protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
	}

	protected String determineIdTableName(Queryable persister) {
		// todo : use the identifier/name qualifier service once we pull that
		// over to master
		return Table.qualify(catalog, schema, "HT_" + persister.getTableName());
	}

	protected String generateIdSubselect(Queryable persister) {
		return "select "
				+ StringHelper.join(", ", persister.getIdentifierColumnNames())
				+ " from " + determineIdTableName(persister);
	}

	protected void prepareForUse(Queryable persister, SharedSessionContractImplementor session) {
	}

	protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
	}

	protected static class ProcessedWhereClause {
		public static final ProcessedWhereClause NO_WHERE_CLAUSE = new ProcessedWhereClause();
		private final String userWhereClauseFragment;
		private final List<ParameterSpecification> idSelectParameterSpecifications;

		private ProcessedWhereClause() {
			this("", Collections.<ParameterSpecification>emptyList());
		}

		public ProcessedWhereClause(String userWhereClauseFragment,
									List<ParameterSpecification> idSelectParameterSpecifications) {
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
}
