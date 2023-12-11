/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import jakarta.persistence.criteria.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The general contract for INSERT statements.  At the moment only the INSERT-SELECT
 * forms is implemented/supported.
 *
 * @author Steve Ebersole
 */
public interface SqmInsertStatement<T> extends SqmDmlStatement<T>, JpaCriteriaInsert<T> {
	@Override
	List<SqmPath<?>> getInsertionTargetPaths();

	@Override
	SqmInsertStatement<T> setInsertionTargetPaths(Path<?>... insertionTargetPaths);

	@Override
	SqmInsertStatement<T> setInsertionTargetPaths(List<? extends Path<?>> insertionTargetPaths);

	@Override
	SqmInsertStatement<T> copy(SqmCopyContext context);

	void visitInsertionTargetPaths(Consumer<SqmPath<?>> consumer);

	@Nullable SqmConflictClause<T> getConflictClause();
}
