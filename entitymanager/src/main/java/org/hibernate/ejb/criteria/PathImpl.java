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

import java.util.Collection;
import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.hibernate.ejb.criteria.expression.AbstractExpression;

/**
 * A {@link Path} models an individual portion of a join expression.
 *
 * @author Steve Ebersole
 */
public class PathImpl<X> extends AbstractExpression<X> implements Path<X> {
	private final PathImpl<?> origin;
	private Bindable<X> model;

	protected PathImpl(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			PathImpl<?> origin,
			Bindable<X> model) {
        super( queryBuilder, javaType );
		this.origin = origin;
		this.model = model;
	}

	/**
	 * {@inheritDoc}
	 */
    public PathImpl<?> getParentPath() {
        return origin;
    }

	/**
	 * {@inheritDoc}
	 */
    public Bindable<X> getModel() {
        if ( model == null ) {
            throw new IllegalStateException( this + " represents a basic path and not a bindable" );
        }
		return model;
    }

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public Expression<Class<? extends X>> type() {
		throw new IllegalStateException( "type() is not applicable to a primitive path node." );	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <Y> Path<Y> get(SingularAttribute<? super X, Y> attribute) {
		throw new IllegalStateException( this + " is a primitive path node." );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> collection) {
		throw new IllegalStateException( this + " is a primitive path node." );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> map) {
		throw new IllegalStateException( this + " is a primitive path node." );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <Y> Path<Y> get(String attributeName) {
		throw new IllegalStateException( this + " is a primitive path node." );
	}

}
