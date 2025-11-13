/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.Locking;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/// Base support for LockingClauseStrategy implementations
///
/// @author Steve Ebersole
public abstract class AbstractLockingClauseStrategy implements LockingClauseStrategy {
	protected final Locking.Scope lockingScope;
	protected final Set<NavigablePath> rootsForLocking;

	private Set<NavigablePath> pathsToLock;

	public AbstractLockingClauseStrategy(
			Locking.Scope lockingScope,
			Set<NavigablePath> rootsForLocking) {
		this.lockingScope = lockingScope;
		this.rootsForLocking = rootsForLocking == null ? Set.of() : rootsForLocking;
	}

	@Override
	public boolean registerRoot(TableGroup root) {
		if ( shouldLockRoot( root ) ) {
			trackRoot( root );
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean shouldLockRoot(TableGroup root) {
		// NOTE : the NavigablePath can be null in some cases.
		// 		we don't care about these cases, so easier to just
		// 		handle the nullness here
		return root.getNavigablePath() != null && rootsForLocking.contains( root.getNavigablePath() );
	}

	protected void trackRoot(TableGroup root) {
		if ( pathsToLock == null ) {
			pathsToLock = new HashSet<>();
		}
		pathsToLock.add( root.getNavigablePath() );
	}

	@Override
	public boolean registerJoin(TableGroupJoin join) {
		if ( shouldLockJoin( join.getJoinedGroup() ) ) {
			trackJoin( join );
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean shouldLockJoin(TableGroup joinedGroup) {
		// we only want to consider applying locks to joins in 2 cases:
		//		1) It is a root path for locking (aka occurs in the domain select-clause)
		//		2) It's left-hand side is to be locked
		if ( isRootForLocking( joinedGroup ) ) {
			return true;
		}
		else if ( isLhsLocked( joinedGroup ) ) {
			if ( lockingScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
				// if the TableGroup is an owned (aka, non-inverse) collection,
				// and we are to lock collections, track it
				if ( joinedGroup.getModelPart() instanceof PluralAttributeMapping attrMapping ) {
					if ( !attrMapping.getCollectionDescriptor().isInverse() ) {
						// owned collection element-collection
						return attrMapping.getElementDescriptor() instanceof BasicValuedCollectionPart;
					}
				}
			}
			else if ( lockingScope == Locking.Scope.INCLUDE_FETCHES ) {
				return joinedGroup.isFetched();
			}
		}

		return false;
	}

	protected boolean isRootForLocking(TableGroup joinedGroup) {
		return rootsForLocking.contains( joinedGroup.getNavigablePath() );
	}

	protected boolean isLhsLocked(TableGroup joinedGroup) {
		// todo (pessimistic-locking) : The use of NavigablePath#parent for LHS here is not ideal.
		//		However, the alternative is to change the method signature to pass the
		//		join's LHS which would have a broad impact on Dialects and translators.
		//		This will possibly miss some cases, but let's start here fow now.
		return pathsToLock != null
			&& pathsToLock.contains( joinedGroup.getNavigablePath().getParent() );
	}

	protected void trackJoin(TableGroupJoin join) {
		if ( pathsToLock == null ) {
			pathsToLock = new HashSet<>();
		}
		pathsToLock.add( join.getNavigablePath() );
	}

	@Override
	public Collection<NavigablePath> getPathsToLock() {
		return pathsToLock;
	}
}
