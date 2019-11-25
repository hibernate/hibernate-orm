/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.pretty.MessageHelper;

/**
 * Similar to {@link AbstractSelectingDelegate}.
 * when we have a nested generated identifier within a composite key.
 * <p>
 * The method {@link AbstractSelectingDelegate#prepareIdentifierGeneratingInsert()} is equal.
 * The method {@link AbstractSelectingDelegate#performInsert(String, SharedSessionContractImplementor, Binder)})}
 * has the same goal here, but with a composite key we have a partial key on which to store the post-insert generated values.
 *
 * @see CompositeNestedGeneratedValueGenerator
 * @see AbstractSelectingDelegate
 * @author Fabio Massimo Ercoli
 */
public class CompositeGeneratedIdentifierSelectingDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;
	private final Set<CompositeNestedGeneratedValueGenerator.PostInsertGenerationPlan> postInsertGenerationPlans;

	public CompositeGeneratedIdentifierSelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect,
			Set<CompositeNestedGeneratedValueGenerator.PostInsertGenerationPlan> postInsertGenerationPlans) {
		this.persister = persister;
		this.dialect = dialect;
		this.postInsertGenerationPlans = postInsertGenerationPlans;
	}

	@Override
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
		IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( dialect );
		for ( CompositeNestedGeneratedValueGenerator.PostInsertGenerationPlan plan : postInsertGenerationPlans ) {
			insert.addIdentityColumn( plan.columnName() );
		}
		return insert;
	}

	@Override
	public Serializable performInsert(String insertSQL, SharedSessionContractImplementor session, Binder binder) {
		throw new HibernateException( "CompositeGeneratedIdentifierSelectingDelegate#performInsert(String, SharedSessionContractImplementor, Binder) " +
				" is not supposed to be run. Use #performInsert(String, Serializable, SharedSessionContractImplementor, Binder) instead." );
	}

	@Override
	public void performInsert(String insertSQL, Serializable partialKey, SharedSessionContractImplementor session, Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( insertSQL, PreparedStatement.NO_GENERATED_KEYS );
			try {
				binder.bindValues( insert );
				session.getJdbcCoordinator().getResultSetReturn().executeUpdate( insert );
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( insert );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			PreparedStatement idSelect = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( selectSQL, false );
			try {
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( idSelect );
				try {
					applyResult( session, rs, partialKey );
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, idSelect );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( idSelect );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve generated id after insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}
	}

	private String getSelectSQL() {
		return persister.getIdentitySelectString();
	}

	private void applyResult(SharedSessionContractImplementor session, ResultSet rs, Serializable partialKey)
			throws SQLException {
		for ( CompositeNestedGeneratedValueGenerator.PostInsertGenerationPlan plan : postInsertGenerationPlans ) {
			Serializable generatedValue = IdentifierGeneratorHelper.getGeneratedIdentity(
					rs,
					plan.columnName(),
					plan.type(),
					session.getJdbcServices().getJdbcEnvironment().getDialect()
			);
			plan.set( session, generatedValue, partialKey );
		}
	}
}
