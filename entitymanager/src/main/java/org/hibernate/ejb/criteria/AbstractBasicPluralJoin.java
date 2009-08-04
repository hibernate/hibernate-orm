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

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.PluralJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 * Convenience base class for all basic collection joins.  Mainly we handle the fact that
 * this path cannot be further de-referenced.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBasicPluralJoin<O,C,E>
		extends JoinImpl<O,E>
		implements PluralJoin<O,C,E>, Fetch<O,E> {

	public AbstractBasicPluralJoin(
			QueryBuilderImpl queryBuilder,
			Class<E> javaType,
			PathImpl<O> lhs,
			PluralAttribute<? super O, ?, ?> joinProperty,
			JoinType joinType) {
		super(queryBuilder, javaType, lhs, joinProperty, joinType);
	}

	@Override
	public PluralAttribute<? super O, C, E> getModel() {
		return ( PluralAttribute<? super O, C, E> ) super.getAttribute();
	}

	@Override
    public Expression<Class<? extends E>> type(){
		throw new BasicPathUsageException( "type() is not applicable to primitive paths.", getAttribute() );
    }

	@Override
    public <Y> Path<Y> get(SingularAttribute<? super E, Y> attribute){
		throw illegalDereference();
    }

	private BasicPathUsageException illegalDereference() {
		return new BasicPathUsageException( "Basic collection elements cannot be de-referenced", getAttribute() );
	}

    @Override
    public <Y, C extends java.util.Collection<Y>> Expression<C> get(PluralAttribute<E, C, Y> collection){
		throw illegalDereference();
    }

    @Override
    public <L, W, M extends java.util.Map<L, W>> Expression<M> get(MapAttribute<E, L, W> map){
		throw illegalDereference();
    }

    @Override
    public <Y> Path<Y> get(String attName) {
		throw illegalDereference();
    }

    @Override
    public <Y> Join<E, Y> join(SingularAttribute<? super E, Y> attribute, JoinType jt) {
        throw illegalJoin();
    }

	private BasicPathUsageException illegalJoin() {
		return new BasicPathUsageException( "Basic collection cannot be source of a join", getAttribute() );
	}

    @Override
    public <Y> CollectionJoin<E, Y> join(CollectionAttribute<? super E, Y> collection, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> SetJoin<E, Y> join(javax.persistence.metamodel.SetAttribute<? super E, Y> set, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> ListJoin<E, Y> join(ListAttribute<? super E, Y> list, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <L, W> MapJoin<E, L, W> join(MapAttribute<? super E, L, W> map, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> Join<E, Y> join(String attributeName, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> CollectionJoin<E, Y> joinCollection(String attributeName, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> ListJoin<E, Y> joinList(String attributeName, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <L, W> MapJoin<E, L, W> joinMap(String attributeName, JoinType jt) {
        throw illegalJoin();
    }

    @Override
    public <Y> SetJoin<E, Y> joinSet(String attributeName, JoinType jt) {
        throw illegalJoin();
    }

}
