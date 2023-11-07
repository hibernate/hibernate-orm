/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.TableMapping;

/**
 * PreparedStatementGroup implementation for cases where we
 * have just a single operation
 *
 * @author Steve Ebersole
 */
public class PreparedStatementGroupSingleTable implements PreparedStatementGroup {
	private final PreparableMutationOperation jdbcMutation;
	private final SharedSessionContractImplementor session;

	private final PreparedStatementDetails statementDetails;

	public PreparedStatementGroupSingleTable(
			PreparableMutationOperation jdbcMutation,
			SharedSessionContractImplementor session) {
		this( jdbcMutation, null, session );
	}

	public PreparedStatementGroupSingleTable(
			PreparableMutationOperation jdbcMutation,
			GeneratedValuesMutationDelegate delegate,
			SharedSessionContractImplementor session) {
		this.jdbcMutation = jdbcMutation;
		this.statementDetails = ModelMutationHelper.standardPreparation( jdbcMutation, delegate, session );
		this.session = session;
	}

	protected TableMapping getMutatingTableDetails() {
		return jdbcMutation.getTableDetails();
	}

	@Override
	public int getNumberOfStatements() {
		return 1;
	}

	@Override
	public int getNumberOfActiveStatements() {
		return statementDetails.getStatement() == null ? 0 : 1;
	}

	@Override
	public PreparedStatementDetails getSingleStatementDetails() {
		return statementDetails;
	}

	@Override
	public void forEachStatement(BiConsumer<String, PreparedStatementDetails> action) {
		action.accept( getMutatingTableDetails().getTableName(), statementDetails );
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		if ( statementDetails == null ) {
			return null;
		}

		assert getMutatingTableDetails().getTableName().equals( tableName );
		return statementDetails;
	}

	@Override
	public PreparedStatementDetails resolvePreparedStatementDetails(String tableName) {
		assert getMutatingTableDetails().getTableName().equals( tableName );
		return statementDetails;
	}

	@Override
	public boolean hasMatching(Predicate<PreparedStatementDetails> filter) {
		return filter.test( statementDetails );
	}

	@Override
	public void release() {
		if ( statementDetails != null ) {
			statementDetails.releaseStatement( session );
		}
	}
}
