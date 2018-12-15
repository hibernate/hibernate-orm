/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class MultiSelectSelection<T> extends CompoundSelection<T> {
	public MultiSelectSelection(
			List<? extends SelectionImplementor<?>> selectionItems,
			Class<T> resultType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( selectionItems, resultType, criteriaBuilder );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitMultiSelect( this );
	}
}
