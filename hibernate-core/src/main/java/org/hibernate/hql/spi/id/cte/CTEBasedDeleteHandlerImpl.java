/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CTEBasedDeleteHandlerImpl extends AbstractCTEBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {

	private static final Logger log = Logger
			.getLogger(CTEBasedDeleteHandlerImpl.class);

	private final Queryable targetedPersister;

	private final String idSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;
	private final List<String> deletes;

	public CTEBasedDeleteHandlerImpl(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		this(factory, walker, null, null);
	}

	public CTEBasedDeleteHandlerImpl(SessionFactoryImplementor factory, HqlSqlWalker walker, String catalog, String schema) {
		super(factory, walker, catalog, schema);

		DeleteStatement deleteStatement = (DeleteStatement) walker.getAST();
		FromElement fromElement = deleteStatement.getFromClause()
				.getFromElement();

		this.targetedPersister = fromElement.getQueryable();

		final ProcessedWhereClause processedWhereClause = processWhereClause(deleteStatement
				.getWhereClause());
		this.idSelectParameterSpecifications = processedWhereClause
				.getIdSelectParameterSpecifications();

		final String bulkTargetAlias = fromElement.getTableAlias();

		this.idSelect = generateIdSelect(targetedPersister, bulkTargetAlias,
				processedWhereClause);

		final String idSubselect = generateIdSubselect(targetedPersister);
		deletes = new ArrayList<String>();

		// If many-to-many, delete the FK row in the collection table.
		// This partially overlaps with DeleteExecutor, but it instead uses the
		// temp table in the idSubselect.
		for (Type type : targetedPersister.getPropertyTypes()) {
			if (type.isCollectionType()) {
				CollectionType cType = (CollectionType) type;
				AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory
						.getCollectionPersister(cType.getRole());
				if (cPersister.isManyToMany()) {
					deletes.add(generateDelete(cPersister.getTableName(),
							cPersister.getKeyColumnNames(), idSubselect,
							"bulk delete - m2m join table cleanup"));
				}
			}
		}

		String[] tableNames = targetedPersister
				.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister
				.getContraintOrderedTableKeyColumnClosure();
		for (int i = 0; i < tableNames.length; i++) {
			// TODO : an optimization here would be to consider cascade deletes
			// and not gen those delete statements;
			// the difficulty is the ordering of the tables here vs the cascade
			// attributes on the persisters ->
			// the table info gotten here should really be self-contained (i.e.,
			// a class representation
			// defining all the needed attributes), then we could then get an
			// array of those
			deletes.add(generateDelete(tableNames[i], columnNames[i],
					idSubselect, "bulk delete"));
		}
	}

	private String generateDelete(String tableName, String[] columnNames, String idSubselect, String comment) {
		final Delete delete = new Delete().setTableName(tableName).setWhere(
				"(" + StringHelper.join(", ", columnNames) + ") IN ("
						+ idSubselect + ")");
		if (factory().getSettings().isCommentsEnabled()) {
			delete.setComment(comment);
		}
		return delete.toStatementString();
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray(new String[deletes.size()]);
	}

	private CTEValues prepareCteStatement(SharedSessionContractImplementor session, QueryParameters queryParameters) {
		CTEValues values = new CTEValues();

		PreparedStatement ps = null;

		try {
			try {
				ps = session.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement(idSelect, false);
				int sum = 1;
				sum += handlePrependedParametersOnIdSelection(ps, session, sum);
				for (ParameterSpecification parameterSpecification : idSelectParameterSpecifications) {
					sum += parameterSpecification.bind(ps, queryParameters,
							session, sum);
				}
				ResultSet rs = session
						.getJdbcCoordinator().getResultSetReturn().extract(ps);
				while (rs.next()) {
					Object[] result = new Object[targetedPersister
							.getIdentifierColumnNames().length];
					for (String columnName : targetedPersister
							.getIdentifierColumnNames()) {
						Object column = rs.getObject(columnName);
						result[rs.findColumn(columnName) - 1] = column;
					}
					values.getSelectResult().add(result);
				}

				if (values.getSelectResult().isEmpty()) {
					return values;
				}

				String idCteSelect = generateIdCteSelect(targetedPersister,
						determineIdTableName(targetedPersister),
						values.getSelectResult());
				values.setIdCteSelect(idCteSelect);

				log.tracev(
						"Generated ID-CTE-SELECT SQL (multi-table delete) : {0}",
						idCteSelect);

				return values;
			}
			finally {
				if (ps != null) {
					ps.close();
				}
			}
		}
		catch (SQLException e) {
			throw convert(e, "could not insert/select ids for bulk delete",
					idSelect);
		}
	}

	@Override
	public int execute(SharedSessionContractImplementor session, QueryParameters queryParameters) {
		prepareForUse(targetedPersister, session);
		try {
			PreparedStatement ps = null;
			int resultCount = 0;

			CTEValues values = prepareCteStatement(session, queryParameters);

			// Start performing the deletes
			for (String delete : deletes) {
				if (values == null || values.getIdCteSelect() == null) {
					continue;
				}

				delete = values.getIdCteSelect() + delete;

				try {
					try {
						ps = session
								.getJdbcCoordinator().getStatementPreparer()
								.prepareStatement(delete, false);
						int pos = 1;
						pos += handlePrependedParametersOnIdSelection(ps,
								session, pos);
						for (Object[] result : values.getSelectResult()) {
							for (Object column : result) {
								ps.setObject(pos++, column);
							}
						}
						handleAddedParametersOnDelete(ps, session);

						resultCount = session
								.getJdbcCoordinator().getResultSetReturn()
								.executeUpdate(ps);
					}
					finally {
						if (ps != null) {
							ps.close();
						}
					}
				}
				catch (SQLException e) {
					throw convert(e, "error performing bulk delete", delete);
				}
			}

			return resultCount;

		}
		finally {
			releaseFromUse(targetedPersister, session);
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SharedSessionContractImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnDelete(PreparedStatement ps, SharedSessionContractImplementor session) throws SQLException {
	}
}
