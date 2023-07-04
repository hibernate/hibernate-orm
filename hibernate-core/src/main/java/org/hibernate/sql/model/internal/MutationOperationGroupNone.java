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

/**
 * Specialized MutationOperationGroup for case of no operations
 *
 * @author Steve Ebersole
 */
public class MutationOperationGroupNone extends AbstractMutationOperationGroup {
	public MutationOperationGroupNone(MutationType mutationType, MutationTarget<?> mutationTarget) {
		super( mutationType, mutationTarget );
	}
	public MutationOperationGroupNone(MutationGroup mutationGroup) {
		this( mutationGroup.getMutationType(), mutationGroup.getMutationTarget() );
	}

	@Override
	public int getNumberOfOperations() {
		return 0;
	}

	@Override
	public MutationOperation getSingleOperation() {
		return null;
	}

	@Override
	public MutationOperation getOperation(int idx) {
		throw new IndexOutOfBoundsException( idx );
	}

	@Override
	public List<MutationOperation> getOperations() {
		return Collections.emptyList();
	}

	@Override
	public MutationOperation getOperation(String tableName) {
		return null;
	}

	@Override
	public void forEachOperation(BiConsumer<Integer, MutationOperation> action) {
	}

	@Override
	public boolean hasMatching(BiFunction<Integer, MutationOperation, Boolean> matcher) {
		return false;
	}

}
