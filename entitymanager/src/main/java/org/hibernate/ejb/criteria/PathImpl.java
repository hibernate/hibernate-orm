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

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.hibernate.ejb.criteria.expression.ExpressionImpl;

/**
 * A {@link Path} models an individual portion of a path expression.
 *
 * @author Steve Ebersole
 */
public class PathImpl<X> extends ExpressionImpl<X> implements Path<X> {
	private final PathImpl<?> origin;
	private final Attribute<?,?> attribute;
	private Object model;

	/**
	 * Constructs a path.
	 *
	 * @param queryBuilder The delegate for building query components.
	 * @param javaType The java type of this path,
	 * @param origin The source ("lhs") of this path.
	 * @param attribute The attribute defining this path element.
	 * @param model The corresponding model of this path.
	 */
	protected PathImpl(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			PathImpl<?> origin,
			Attribute<?,?> attribute,
			Object model) {
        super( queryBuilder, javaType );
		this.origin = origin;
		this.attribute = attribute;
		this.model = model;
	}

	/**
	 * {@inheritDoc}
	 */
    public PathImpl<?> getParentPath() {
        return origin;
    }

	public Attribute<?, ?> getAttribute() {
		return attribute;
	}

	/**
	 * {@inheritDoc}
	 */
    public Bindable<X> getModel() {
        if ( model == null ) {
            throw new IllegalStateException( this + " represents a basic path and not a bindable" );
        }
		return (Bindable<X>)model;
    }

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public Expression<Class<? extends X>> type() {
		throw new BasicPathUsageException( "type() is not applicable to primitive paths.", getAttribute() );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <Y> Path<Y> get(SingularAttribute<? super X, Y> attribute) {
		throw illegalDereference();
	}

	private BasicPathUsageException illegalDereference() {
		return new BasicPathUsageException( "Primitive path cannot be de-referenced", getAttribute() );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> collection) {
		throw illegalDereference();
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> map) {
		throw illegalDereference();
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Subclasses override this appropriately, but here we simply throw
	 * an {@link IllegalStateException}
	 */
	public <Y> Path<Y> get(String attributeName) {
		throw illegalDereference();
	}

}
