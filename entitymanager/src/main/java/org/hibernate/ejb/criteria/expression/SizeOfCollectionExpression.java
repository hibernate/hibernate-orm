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
package org.hibernate.ejb.criteria.expression;

import java.util.Collection;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Represents a "size of" expression in regards to a persistent collection; the implication is
 * that of a subquery.
 *
 * @author Steve Ebersole
 */
public class SizeOfCollectionExpression<C extends Collection>
		extends ExpressionImpl<Integer> {
	private final CollectionExpression<C> collectionExpression;

	public SizeOfCollectionExpression(
			QueryBuilderImpl queryBuilder,
			CollectionExpression<C> collectionExpression) {
		super(queryBuilder, Integer.class);
		this.collectionExpression = collectionExpression;
	}

	public CollectionExpression<C> getCollectionExpression() {
		return collectionExpression;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do
	}

}
