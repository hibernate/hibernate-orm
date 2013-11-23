/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.criteria;

import java.io.Serializable;

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
