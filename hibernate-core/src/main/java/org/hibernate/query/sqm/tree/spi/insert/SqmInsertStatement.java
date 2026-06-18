/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.insert;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Path;

/**
 * The general contract for INSERT statements.  At the moment only the INSERT-SELECT
 * forms is implemented/supported.
 *
 * @author Steve Ebersole
 */
public interface SqmInsertStatement<T> extends SqmDmlStatement<T>, JpaCriteriaInsert<T> {
	@Nonnull
	@Override
	List<SqmPath<?>> getInsertionTargetPaths();

	@Nonnull
	@Override
	SqmInsertStatement<T> setInsertionTargetPaths(@Nonnull Path<?>... insertionTargetPaths);

	@Nonnull
	@Override
	SqmInsertStatement<T> setInsertionTargetPaths(@Nonnull List<? extends Path<?>> insertionTargetPaths);

	@Override
	SqmInsertStatement<T> copy(SqmCopyContext context);

	void visitInsertionTargetPaths(Consumer<SqmPath<?>> consumer);

	@Nullable SqmConflictClause<T> getConflictClause();
}
