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

@Incubating
// Used by Hibernate Reactive
public interface JdbcSelectWithActionsBuilder {

	JdbcSelectWithActionsBuilder setPrimaryAction(JdbcSelect primaryAction);

	JdbcSelectWithActionsBuilder setLoadedValuesCollector(LoadedValuesCollector loadedValuesCollector);

	JdbcSelectWithActionsBuilder setLockTimeoutType(LockTimeoutType lockTimeoutType);

	JdbcSelectWithActionsBuilder setLockingSupport(LockingSupport lockingSupport);

	JdbcSelectWithActionsBuilder setLockOptions(LockOptions lockOptions);

	JdbcSelectWithActionsBuilder setLockingTarget(QuerySpec lockingTarget);

	JdbcSelectWithActionsBuilder setLockingClauseStrategy(LockingClauseStrategy lockingClauseStrategy);

	JdbcSelectWithActionsBuilder setIsFollowOnLockStrategy(boolean isFollonOnLockStrategy);

	JdbcSelectWithActionsBuilder appendPreAction(PreAction... actions);

	JdbcSelectWithActionsBuilder prependPreAction(PreAction... actions);

	JdbcSelectWithActionsBuilder appendPostAction(PostAction... actions);

	JdbcSelectWithActionsBuilder prependPostAction(PostAction... actions);

	JdbcSelectWithActionsBuilder addSecondaryActionPair(SecondaryAction action);

	JdbcSelectWithActionsBuilder addSecondaryActionPair(PreAction preAction, PostAction postAction);

	JdbcSelect build();

}
