/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.spi.AbstractSelection;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;
import org.hibernate.query.criteria.spi.SelectionImplementor;

/**
 * Base implementation of the {@link JpaCompoundSelection} contract.
 *
 * @apiNote A CompoundSelection is either a multi-select (tuple or array) or
 * a dynamic-instantiation
 *
 * @author Steve Ebersole
 */
public abstract class CompoundSelection<X>
		extends AbstractSelection<X>
		implements JpaCompoundSelection<X> {
	private List<? extends SelectionImplementor<?>> selectionItems;

	public CompoundSelection(
			List<? extends SelectionImplementor<?>> selectionItems,
			Class<X> resultType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( resultType, criteriaBuilder );
		this.selectionItems = selectionItems;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<? extends SelectionImplementor<?>> getSelectionItems() {
		return selectionItems;
	}
}
