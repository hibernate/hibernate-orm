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

/**
 * Specialized MutationOperationGroup for case of no operations
 *
 * @author Steve Ebersole
 */
public class MutationOperationGroupNone extends AbstractMutationOperationGroup {
	public MutationOperationGroupNone(MutationType mutationType, MutationTarget<?> mutationTarget) {
		super( mutationType, mutationTarget );
	}

	@Override
	public int getNumberOfOperations() {
		return 0;
	}

	@Override
	public <O extends MutationOperation> O getSingleOperation() {
		return null;
	}

	@Override
	public <O extends MutationOperation> List<O> getOperations() {
		return Collections.emptyList();
	}

	@Override
	public <O extends MutationOperation> O getOperation(String tableName) {
		return null;
	}

	@Override
	public <O extends MutationOperation> void forEachOperation(BiConsumer<Integer, O> action) {
	}

	@Override
	public <O extends MutationOperation> boolean hasMatching(BiFunction<Integer, O, Boolean> matcher) {
		return false;
	}
}
