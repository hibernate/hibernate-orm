/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedNavigableReference implements NavigableContainerReference {
	private final NavigablePath navigablePath;
	private final EmbeddedValuedNavigable navigable;
	private final TableGroup ownerTableGroup;
	private final LockMode lockMode;

	public EmbeddableValuedNavigableReference(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable navigable,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.navigable = navigable;
		this.lockMode = lockMode;

		// the TableGroup for any embeddable value is defined by its container/parent
		this.ownerTableGroup = creationState.getFromClauseAccess().findTableGroup( navigablePath.getParent() );

		// but we also want to make sure it is registered under our NavigablePath as well for any de-references from the embedded
		creationState.getFromClauseAccess().registerTableGroup( navigablePath, ownerTableGroup );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainer getNavigable() {
		return navigable;
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

	@Override
	public ColumnReferenceQualifier getColumnReferenceQualifier() {
		return ownerTableGroup;
	}
}
