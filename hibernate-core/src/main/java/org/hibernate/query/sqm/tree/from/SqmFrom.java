/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;

/**
 * Models a Bindable's inclusion in the {@code FROM} clause.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom extends TableGroupInfo, SqmVisitableNode, SqmTypedNode, SqmPath {
	/**
	 * The Navigable for an SqmFrom will always be a NavigableContainer
	 *
	 * {@inheritDoc}
	 */
	@Override
	NavigableContainer<?> getReferencedNavigable();

	/**
	 * The joins associated with this SqmFrom
	 */
	List<SqmJoin> getJoins();

	/**
	 * Add an associated join
	 */
	void addJoin(SqmJoin join);

	/**
	 * Visit all associated joins
	 */
	void visitJoins(Consumer<SqmJoin> consumer);

	/**
	 * Details about how this SqmFrom is used in the query.
	 */
	UsageDetails getUsageDetails();

	@Override
	default void prepareForSubNavigableReference(
			SqmPath subReference,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		// nothing to do, already prepared
	}
}
