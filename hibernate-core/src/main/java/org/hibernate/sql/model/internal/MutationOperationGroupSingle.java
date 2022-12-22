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

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MutationOperationGroupSingle extends AbstractMutationOperationGroup {
	private final MutationOperation operation;

	public MutationOperationGroupSingle(MutationType mutationType, MutationTarget<?> mutationTarget, MutationOperation operation) {
		super( mutationType, mutationTarget );
		this.operation = operation;
	}

	public MutationOperationGroupSingle(MutationGroup mutationGroup, MutationOperation operation) {
		this( mutationGroup.getMutationType(), mutationGroup.getMutationTarget(), operation );
	}

	@Override
	public int getNumberOfOperations() {
		return 1;
	}

	@Override
	public <O extends MutationOperation> O getSingleOperation() {
		//noinspection unchecked
		return (O) operation;
	}

	@Override
	public <O extends MutationOperation> List<O> getOperations() {
		//noinspection unchecked
		return Collections.singletonList( (O) operation );
	}

	@Override
	public <O extends MutationOperation> O getOperation(String tableName) {
		if ( !tableName.equals( operation.getTableDetails().getTableName() ) ) {
			MODEL_MUTATION_LOGGER.debugf(
					"Unexpected table name mismatch : `%s` - `%s`",
					tableName,
					operation.getTableDetails().getTableName()
			);
		}

		//noinspection unchecked
		return (O) operation;
	}

	@Override
	public <O extends MutationOperation> void forEachOperation(BiConsumer<Integer, O> action) {
		//noinspection unchecked
		action.accept( 0, (O) operation );
	}

	@Override
	public <O extends MutationOperation> boolean hasMatching(BiFunction<Integer, O, Boolean> matcher) {
		//noinspection unchecked
		return matcher.apply( 0, (O) operation );
	}
}
