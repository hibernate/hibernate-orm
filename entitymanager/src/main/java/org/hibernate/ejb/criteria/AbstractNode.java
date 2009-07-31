/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.criteria;

/**
 * All nodes in a criteria query tree will generally need access to the {@link QueryBuilderImpl} from which they
 * come.  This base class provides convenient, consistent support for that.
 *
 * @author Steve Ebersole
 */
public class AbstractNode {
	private final QueryBuilderImpl queryBuilder;

	public AbstractNode(QueryBuilderImpl queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	/**
	 * Provides protected access to the underlying {@link QueryBuilderImpl}.
	 *
	 * @return The underlying {@link QueryBuilderImpl} instance.
	 */
	protected QueryBuilderImpl queryBuilder() {
		return queryBuilder;
	}
}
