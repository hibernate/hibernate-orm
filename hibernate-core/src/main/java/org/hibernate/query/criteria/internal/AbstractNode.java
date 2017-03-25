/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;

/**
 * All nodes in a criteria query tree will generally need access to the {@link CriteriaBuilderImpl} from which they
 * come.  This base class provides convenient, consistent support for that.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNode implements Serializable {
	private final CriteriaBuilderImpl criteriaBuilder;

	public AbstractNode(CriteriaBuilderImpl criteriaBuilder) {
		this.criteriaBuilder = criteriaBuilder;
	}

	/**
	 * Provides access to the underlying {@link CriteriaBuilderImpl}.
	 *
	 * @return The underlying {@link CriteriaBuilderImpl} instance.
	 */
	public CriteriaBuilderImpl criteriaBuilder() {
		return criteriaBuilder;
	}
}
