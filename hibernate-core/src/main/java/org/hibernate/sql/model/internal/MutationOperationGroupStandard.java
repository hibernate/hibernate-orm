/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * @author Steve Ebersole
 */
public class MutationOperationGroupStandard extends AbstractMutationOperationGroup {
	private final List<MutationOperation> operations;

	public MutationOperationGroupStandard(MutationType mutationType, MutationTarget<?> mutationTarget, List<MutationOperation> operations) {
		super( mutationType, mutationTarget );
		this.operations = operations;
	}

	@Override
	public int getNumberOfOperations() {
		return operations.size();
	}

	@Override
	public <O extends MutationOperation> O getSingleOperation() {
		if ( operations.size() == 1 ) {
			//noinspection unchecked
			return (O) operations.get( 0 );
		}
		throw new IllegalStateException(
				String.format(
						Locale.ROOT,
						"Group contains multiple table mutations - %s : %s ",
						getMutationType().name(),
						getMutationTarget().getNavigableRole()
				)
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends MutationOperation> List<O> getOperations() {
		//noinspection rawtypes
		return (List) operations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends MutationOperation> O getOperation(String tableName) {
		for ( int i = 0; i < operations.size(); i++ ) {
			final MutationOperation operation = operations.get( i );
			if ( operation.getTableDetails().getTableName().equals( tableName ) ) {
				return (O) operation;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends MutationOperation> void forEachOperation(BiConsumer<Integer, O> action) {
		for ( int i = 0; i < operations.size(); i++ ) {
			action.accept( i, (O) operations.get( i ) );
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends MutationOperation> boolean hasMatching(BiFunction<Integer, O, Boolean> matcher) {
		for ( int i = 0; i < operations.size(); i++ ) {
			if ( matcher.apply( i, (O) operations.get( i ) ) ) {
				return true;
			}
		}
		return false;
	}
}
