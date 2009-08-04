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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.metamodel.ListAttribute;
import org.hibernate.ejb.criteria.expression.ListIndexExpression;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.List}, whose elements
 * are basic type.
 *
 * @author Steve Ebersole
 */
public class BasicListJoinImpl<O,E>
		extends AbstractBasicPluralJoin<O,java.util.List<E>,E> 
		implements ListJoin<O,E> {

	public BasicListJoinImpl(
			QueryBuilderImpl queryBuilder,
			Class<E> javaType,
			PathImpl<O> lhs,
			ListAttribute<? super O, ?> joinProperty,
			JoinType joinType) {
		super(queryBuilder, javaType, lhs, joinProperty, joinType);
	}

	@Override
	public ListAttribute<? super O, E> getAttribute() {
		return (ListAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public ListAttribute<? super O, E> getModel() {
        return getAttribute();
    }

	public Expression<Integer> index() {
		return new ListIndexExpression( queryBuilder(), getAttribute() );
	}
}
