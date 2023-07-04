/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MutationOperationGroupSingle extends AbstractMutationOperationGroup {

	private final JdbcMutationOperation operation;

	public MutationOperationGroupSingle(MutationType mutationType, MutationTarget<?> mutationTarget, JdbcMutationOperation operation) {
		super( mutationType, mutationTarget );
		this.operation = operation;
	}

	public MutationOperationGroupSingle(MutationGroup mutationGroup, JdbcMutationOperation operation) {
		this( mutationGroup.getMutationType(), mutationGroup.getMutationTarget(), operation );
	}

	@Override
	public int getNumberOfOperations() {
		return 1;
	}

	@Override
	public JdbcMutationOperation getSingleOperation() {
		return operation;
	}

	@Override
	public MutationOperation getOperation(int idx) {
		if ( idx != 0 ) throw new IndexOutOfBoundsException( idx );
		return operation;
	}

	@Override
	public List<MutationOperation> getOperations() {
		return Collections.singletonList( operation );
	}

	@Override
	public MutationOperation getOperation(String tableName) {
		if ( !tableName.equals( operation.getTableDetails().getTableName() ) ) {
			MODEL_MUTATION_LOGGER.debugf(
					"Unexpected table name mismatch : `%s` - `%s`",
					tableName,
					operation.getTableDetails().getTableName()
			);
		}

		return operation;
	}

	@Override
	public void forEachOperation(BiConsumer<Integer, MutationOperation> action) {
		action.accept( 0, operation );
	}

	@Override
	public boolean hasMatching(BiFunction<Integer, MutationOperation, Boolean> matcher) {
		return matcher.apply( 0, operation );
	}

}
