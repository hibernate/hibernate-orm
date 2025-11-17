/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import java.util.Locale;
import java.util.function.BiConsumer;

import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.TableMutation;

/**
 * MutationGroup for cases where we have no mutations.  Generally
 * this is only used from the case of a single TableMutationBuilder
 *
 * @author Steve Ebersole
 */
public class MutationGroupNone implements MutationGroup {
	private final MutationType mutationType;
	private final MutationTarget<?> mutationTarget;

	public MutationGroupNone(MutationType mutationType, MutationTarget<?> mutationTarget) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
	}

	@Override
	public MutationType getMutationType() {
		return mutationType;
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public int getNumberOfTableMutations() {
		return 0;
	}

	@Override
	public TableMutation getSingleTableMutation() {
		return null;
	}

	@Override
	public TableMutation getTableMutation(String tableName) {
		return null;
	}

	@Override
	public <O extends MutationOperation, M extends TableMutation<O>> void forEachTableMutation(BiConsumer<Integer, M> action) {
	}

	@Override
	public TableMutation getTableMutation(int i) {
		return null;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationGroupNone( %s:`%s` )",
				mutationType.name(),
				mutationTarget.getNavigableRole().getFullPath()
		);
	}
}
