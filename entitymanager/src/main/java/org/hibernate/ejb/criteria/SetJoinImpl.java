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

import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.SetAttribute;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.Set}, whose elements
 * are associations.
 *
 * @author Steve Ebersole
 */
public class SetJoinImpl<O,E>
		extends JoinImpl<O,E>
		implements JoinImplementors.SetJoinImplementor<O,E> {

	public SetJoinImpl(
			QueryBuilderImpl queryBuilder,
			Class<E> javaType, 
			PathImpl<O> lhs,
			SetAttribute<? super O, ?> joinProperty,
			JoinType joinType) {
		super(queryBuilder, javaType, lhs, joinProperty, joinType);
	}

	@Override
	public SetAttribute<? super O, E> getModel() {
        return (SetAttribute<? super O, E>) super.getAttribute();
	}

}
