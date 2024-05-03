/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.jpa;

import java.util.List;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for {@link JpaSelection} impls.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaSelection<T>
		extends AbstractJpaTupleElement<T>
		implements SqmSelectableNode<T>, JpaSelection<T> {
	protected AbstractJpaSelection(@Nullable SqmExpressible<? super T> sqmExpressible, NodeBuilder criteriaBuilder) {
		super( sqmExpressible, criteriaBuilder );
	}

	@Override
	public JpaSelection<T> alias(String alias) {
		setAlias( alias );
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
