/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPath<T> extends AbstractSqmExpression<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPath<?> lhs;

	/**
	 * For HQL and Criteria processing - used to track reusable paths relative to this path.
	 * E.g., given `p.mate.mate` the SqmRoot identified by `p` would
	 * have a reusable path for the `p.mate` path.
	 */
	private Map<String, SqmPath<?>> reusablePaths;

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( referencedPathSource, nodeBuilder );
		this.navigablePath = navigablePath;
		this.lhs = lhs;
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return (SqmPathSource<T>) super.getNodeType();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return (SqmPathSource<T>) super.getNodeType();
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(SqmPathSource<T> referencedPathSource, SqmPath<?> lhs, NodeBuilder nodeBuilder) {
		this(
				lhs == null
						? new NavigablePath( referencedPathSource.getPathName() )
						: lhs.getNavigablePath().append( referencedPathSource.getPathName() ),
				referencedPathSource,
				lhs,
				nodeBuilder
		);
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public SqmPath<?> getLhs() {
		return lhs;
	}

	@Override
	public List<SqmPath<?>> getReusablePaths() {
		if ( reusablePaths == null ) {
			return Collections.emptyList();
		}

		return new ArrayList<>( reusablePaths.values() );
	}

	@Override
	public void visitReusablePaths(Consumer<SqmPath<?>> consumer) {
		if ( reusablePaths != null ) {
			reusablePaths.values().forEach( consumer );
		}
	}

	@Override
	public void registerReusablePath(SqmPath<?> path) {
		assert path.getLhs() == this;

		if ( reusablePaths == null ) {
			reusablePaths = new HashMap<>();
		}

		final String relativeName = path.getNavigablePath().getUnaliasedLocalName();

		final SqmPath<?> previous = reusablePaths.put( relativeName, path );
		if ( previous != null && previous != path ) {
			throw new IllegalStateException( "Implicit-join path registration unexpectedly overrode previous registration - " + relativeName );
		}
	}

	@Override
	public SqmPath<?> getReusablePath(String name) {
		if ( reusablePaths == null ) {
			return null;
		}
		return reusablePaths.get( name );
	}

	@Override
	public String getExplicitAlias() {
		return getAlias();
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		setAlias( explicitAlias );
	}

	@Override
	public SqmPathSource<T> getModel() {
		return getReferencedPathSource();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Class<? extends T>> type() {
		return (SqmExpression<Class<? extends T>>) get( EntityDiscriminatorMapping.ROLE_NAME );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<?> get(String attributeName) {

		// todo (6.0) : this is similar to the idea of  creating an SqmExpression for a Navigable
		//		should make these stylistically consistent, either -
		//			1) add `Navigable#createCriteriaExpression` (ala, the exist `#createSqmExpression`)
		//			2) remove `Navigable#createSqmExpression` and use the approach used here instead.

		final SqmPathSource<?> subNavigable = getReferencedPathSource().findSubPathSource( attributeName );

		if ( subNavigable == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named `" + attributeName + "` relative to `" + getNavigablePath() + "`" );
		}
		return resolvePath( attributeName, subNavigable );
	}

	protected <X> SqmPath<X> resolvePath(PersistentAttribute<?, X> attribute) {
		//noinspection unchecked
		return resolvePath( attribute.getName(), (SqmPathSource<X>) attribute );
	}

	protected <X> SqmPath<X> resolvePath(String attributeName, SqmPathSource<X> pathSource) {
		if ( reusablePaths == null ) {
			reusablePaths = new HashMap<>();
			final SqmPath<X> path = pathSource.createSqmPath( this );
			reusablePaths.put( attributeName, path );
			return path;
		}
		else {
			//noinspection unchecked
			return (SqmPath<X>) reusablePaths.computeIfAbsent(
					attributeName,
					name -> pathSource.createSqmPath( this )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SqmPath<Y> get(SingularAttribute<? super T, Y> jpaAttribute) {
		return (SqmPath<Y>) resolvePath( (PersistentAttribute<?, ?>) jpaAttribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E, C extends java.util.Collection<E>> SqmPath<C> get(PluralAttribute<T, C, E> attribute) {
		return resolvePath( (PersistentAttribute<T, C>) attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V, M extends java.util.Map<K, V>> SqmPath<M> get(MapAttribute<T, K, V> map) {
		return resolvePath( (PersistentAttribute<T, M>) map );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath.getFullPath() + ")";
	}
}
