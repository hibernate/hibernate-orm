/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.spi.TreatedNavigablePath;

import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

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

	protected AbstractSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( referencedPathSource, nodeBuilder );
		this.navigablePath = navigablePath;
		this.lhs = lhs;
	}

	protected void copyTo(AbstractSqmPath<T> target, SqmCopyContext context) {
		assert navigablePathsMatch( target );
		super.copyTo( target, context );
	}

	// meant for assertions only
	private boolean navigablePathsMatch(AbstractSqmPath<T> target) {
		final SqmPath<?> lhs = getLhs() != null ? getLhs() : findRoot();
		final SqmPath<?> targetLhs = target.getLhs() != null ? target.getLhs() : target.findRoot();
		return lhs == null || lhs.getNavigablePath() == targetLhs.getNavigablePath()
				|| getRoot( lhs ).getNodeType() instanceof SqmPolymorphicRootDescriptor;
	}

	private SqmPath<?> getRoot(SqmPath<?> lhs) {
		return lhs.getLhs() == null ? lhs : getRoot( lhs.getLhs() );
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return (SqmPathSource<T>) NullnessUtil.castNonNull( super.getNodeType() );
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return (SqmPathSource<T>) NullnessUtil.castNonNull( super.getNodeType() );
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

		final String relativeName = path.getNavigablePath().getLocalName();

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
	public SqmPathSource<?> getResolvedModel() {
		final DomainType<?> lhsType;
		final SqmPathSource<T> pathSource = getReferencedPathSource();
		if ( pathSource.isGeneric() && ( lhsType = getLhs().getResolvedModel().getSqmPathType() ) instanceof ManagedDomainType ) {
			final PersistentAttribute<?, ?> concreteAttribute = ( (ManagedDomainType<?>) lhsType ).findConcreteGenericAttribute(
					pathSource.getPathName()
			);
			if ( concreteAttribute != null ) {
				return (SqmPathSource<?>) concreteAttribute;
			}
		}
		return getModel();
	}

	@Override
	public SqmExpressible<T> getExpressible() {
		//noinspection unchecked
		return (SqmExpressible<T>) getResolvedModel();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Class<? extends T>> type() {
		final SqmPathSource<T> referencedPathSource = getReferencedPathSource();
		final SqmPathSource<?> subPathSource = referencedPathSource.findSubPathSource( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME );

		if ( subPathSource == null ) {
			return new SqmLiteral<>(
					referencedPathSource.getBindableJavaType(),
					nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Class.class ),
					nodeBuilder()
			);
		}
		return (SqmExpression<Class<? extends T>>) resolvePath( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME, subPathSource );
	}

	@Override
	public <Y> SqmPath<Y> get(String attributeName) {
		//noinspection unchecked
		final SqmPathSource<Y> subNavigable = (SqmPathSource<Y>)
				getResolvedModel().getSubPathSource( attributeName, nodeBuilder().getJpaMetamodel() );
		return resolvePath( attributeName, subNavigable );
	}

	protected <X> SqmPath<X> resolvePath(PersistentAttribute<?, X> attribute) {
		//noinspection unchecked
		return resolvePath( attribute.getName(), (SqmPathSource<X>) attribute );
	}

	protected <X> SqmPath<X> resolvePath(String attributeName, SqmPathSource<X> pathSource) {
		if ( reusablePaths == null ) {
			reusablePaths = new HashMap<>();
			final SqmPath<X> path = pathSource.createSqmPath( this, getResolvedModel().getIntermediatePathSource( pathSource ) );
			reusablePaths.put( attributeName, path );
			return path;
		}
		else {
			//noinspection unchecked
			return (SqmPath<X>) reusablePaths.computeIfAbsent(
					attributeName,
					name -> pathSource.createSqmPath( this, getResolvedModel().getIntermediatePathSource( pathSource ) )
			);
		}
	}

	protected <S extends T> SqmTreatedPath<T, S> getTreatedPath(ManagedDomainType<S> treatTarget) {
		final NavigablePath treat = getNavigablePath().treatAs( treatTarget.getTypeName() );
		//noinspection unchecked
		SqmTreatedPath<T, S> path = (SqmTreatedPath<T, S>) getLhs().getReusablePath( treat.getLocalName() );
		if ( path == null ) {
			if ( treatTarget instanceof EntityDomainType<?> ) {
				path = new SqmTreatedEntityValuedSimplePath<>( this, (EntityDomainType<S>) treatTarget, nodeBuilder() );
			}
			else {
				path = new SqmTreatedEmbeddedValuedSimplePath<>( this, (EmbeddableDomainType<S>) treatTarget );
			}
			getLhs().registerReusablePath( path );
		}
		return path;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) {
		return getTreatedPath( treatTarget );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return getTreatedPath( treatTarget );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		return null;
	}

	/**
	 * Utility that checks if this path's parent navigable path is compatible with the specified SQM parent,
	 * and if not creates a copy of the navigable path with the correct parent.
	 */
	protected NavigablePath getNavigablePathCopy(SqmPath<?> parent) {
		final NavigablePath realParentPath = getRealParentPath(
				castNonNull( navigablePath.getRealParent() ),
				parent.getNavigablePath()
		);
		if ( realParentPath != null ) {
			return realParentPath.append( navigablePath.getLocalName(), navigablePath.getAlias() );
		}
		return navigablePath;
	}

	private NavigablePath getRealParentPath(NavigablePath realParent, NavigablePath parent) {
		if ( parent == realParent ) {
			return null;
		}
		else if ( realParent instanceof EntityIdentifierNavigablePath ) {
			parent = getRealParentPath( castNonNull( realParent.getRealParent() ), parent );
			if ( parent != null ) {
				parent = new EntityIdentifierNavigablePath(
						parent,
						( (EntityIdentifierNavigablePath) realParent ).getIdentifierAttributeName()
				);
			}
		}
		else if ( realParent.getAlias() == null && realParent instanceof TreatedNavigablePath ) {
			// This might be an implicitly treated parent path, check with the non-treated parent
			parent = getRealParentPath( castNonNull( realParent.getRealParent() ), parent );
			if ( parent != null ) {
				parent = parent.treatAs( realParent.getLocalName().substring( 1 ) );
			}
		}
		else if ( CollectionPart.Nature.fromNameExact( realParent.getLocalName() ) != null ) {
			if ( parent == realParent.getRealParent() ) {
				return null;
			}
			else {
				parent = parent.append( realParent.getLocalName() );
			}
		}
		return parent;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SqmPath<Y> get(SingularAttribute<? super T, Y> jpaAttribute) {
		return (SqmPath<Y>) resolvePath( (PersistentAttribute<?, ?>) jpaAttribute );
	}

	@Override
	public <E, C extends Collection<E>> SqmExpression<C> get(PluralAttribute<? super T, C, E> attribute) {
		//noinspection unchecked
		return resolvePath( (PersistentAttribute<T, C>) attribute );
	}

	@Override
	public <K, V, M extends Map<K, V>> SqmExpression<M> get(MapAttribute<? super T, K, V> attribute) {
		//noinspection unchecked
		return resolvePath( (PersistentAttribute<T, M>) attribute );
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ")";
	}
}
