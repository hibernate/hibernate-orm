/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;

/**
 * @author Steve Ebersole
 */
public class EntityValuedNavigableReference implements NavigableContainerReference {
	private final NavigablePath navigablePath;
	private final EntityValuedNavigable navigable;
	private final LockMode lockMode;

	public EntityValuedNavigableReference(
			NavigablePath navigablePath,
			EntityValuedNavigable navigable,
			LockMode lockMode,
			@SuppressWarnings("unused") SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;
	}


	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedNavigable getNavigable() {
		return navigable;
	}

	@Override
	public String toString() {
		return '`' + getNavigablePath().getFullPath() + '`';
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// These were meant to help with re-usable implicit joins.

}
