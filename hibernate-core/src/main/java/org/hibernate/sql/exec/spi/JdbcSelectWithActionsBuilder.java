/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/// Contract used while building a [JdbcSelect] which might potentially
/// include [pre-][PreAction] and/or [post-][PostAction] actions.
///
/// @author Steve Ebersole
/// @author Andrea Boriero
@Incubating
// Used by Hibernate Reactive
public interface JdbcSelectWithActionsBuilder {
	/// The primary selection.
	JdbcSelectWithActionsBuilder setPrimaryAction(JdbcSelect primaryAction);

	/// Collector of loaded values for post-processing.
	JdbcSelectWithActionsBuilder setLoadedValuesCollector(LoadedValuesCollector loadedValuesCollector);

	///  Lock-timeout handling type.
	JdbcSelectWithActionsBuilder setLockTimeoutType(LockTimeoutType lockTimeoutType);

	/// Dialect's support for locking.
	JdbcSelectWithActionsBuilder setLockingSupport(LockingSupport lockingSupport);

	/// Requested lock options.
	JdbcSelectWithActionsBuilder setLockOptions(LockOptions lockOptions);

	/// QuerySpec (selection) which is the target of locking.
	JdbcSelectWithActionsBuilder setLockingTarget(QuerySpec lockingTarget);

	/// Access to locking details - used for paths to lock, mainly.
	JdbcSelectWithActionsBuilder setLockingClauseStrategy(LockingClauseStrategy lockingClauseStrategy);

	/// Whether follow-on locking should be used.
	JdbcSelectWithActionsBuilder setIsFollowOnLockStrategy(boolean isFollowOnLockStrategy);

	JdbcSelectWithActionsBuilder appendPreAction(PreAction... actions);

	JdbcSelectWithActionsBuilder prependPreAction(PreAction... actions);

	JdbcSelectWithActionsBuilder appendPostAction(PostAction... actions);

	JdbcSelectWithActionsBuilder prependPostAction(PostAction... actions);

	JdbcSelectWithActionsBuilder addSecondaryActionPair(SecondaryAction action);

	JdbcSelectWithActionsBuilder addSecondaryActionPair(PreAction preAction, PostAction postAction);

	/// Build the appropriate JdbcSelect.
	JdbcSelect build();
}
