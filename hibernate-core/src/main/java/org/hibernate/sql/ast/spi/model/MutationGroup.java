/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.function.BiConsumer;

import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;

/**
 * Grouping of table mutations for the given target for
 * the given type of mutation
 *
 * @author Steve Ebersole
 */
public interface MutationGroup {
	MutationType getMutationType();

	MutationTarget getMutationTarget();

	int getNumberOfTableMutations();

	TableMutation getSingleTableMutation();

	@Deprecated(forRemoval = true)
	<O extends MutationOperation, M extends TableMutation<O>> M getTableMutation(String tableName);

	@Deprecated(forRemoval = true)
	<O extends MutationOperation, M extends TableMutation<O>> void forEachTableMutation(BiConsumer<Integer, M> action);

	TableMutation getTableMutation(int i);
}
