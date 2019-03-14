/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class EntityValuedNavigableReference implements NavigableContainerReference {
	private final NavigablePath navigablePath;
	private final EntityValuedNavigable navigable;
	private final TableGroup tableGroup;
	private final LockMode lockMode;

	public EntityValuedNavigableReference(
			NavigablePath navigablePath,
			EntityValuedNavigable navigable,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;

		this.tableGroup = creationState.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				no -> {
					final TableGroup parentTableGroup = creationState.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
					creationState.getFromClauseAccess().registerTableGroup( navigablePath, parentTableGroup );
					return parentTableGroup;
				}
		);
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
	public ColumnReferenceQualifier getColumnReferenceQualifier() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public TableGroup getAssociatedTableGroup() {
		return tableGroup;
	}

	@Override
	public String toString() {
		return "EntityValuedNavigableReference(" + getNavigable().getEntityName() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// These were meant to help with re-usable implicit joins.

	@Override
	public NavigableReference findNavigableReference(String navigableName) {
		return null;
	}

	@Override
	public void addNavigableReference(NavigableReference reference) {

	}
}
