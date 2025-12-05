/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
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
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPath<T> extends AbstractSqmExpression<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPathSource<T> referencedPathSource;
	private final @Nullable SqmPath<?> lhs;

	/**
	 * For HQL and Criteria processing - used to track reusable paths relative to this path.
	 * E.g., given {@code p.mate.mate} the {@code SqmRoot} identified by {@code p} would
	 * have a reusable path for the {@code p.mate} path.
	 */
	private Map<String, SqmPath<?>> reusablePaths;

	protected AbstractSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( referencedPathSource.getSqmType(), nodeBuilder );
		this.navigablePath = navigablePath;
		this.referencedPathSource = referencedPathSource;
		this.lhs = lhs;
//		assert super.getNodeType() == referencedPathSource;
	}

	protected void copyTo(AbstractSqmPath<T> target, SqmCopyContext context) {
		assert navigablePathsMatch( target );
		super.copyTo( target, context );
	}

	// meant for assertions only
	private boolean navigablePathsMatch(AbstractSqmPath<T> target) {
		final SqmPath<?> lhs = getLhsOrRoot();
		final SqmPath<?> targetLhs = target.getLhsOrRoot();
		return lhs == null && targetLhs == null
			|| lhs != null && targetLhs != null
				&& (lhs.getNavigablePath() == targetLhs.getNavigablePath()
					|| getRoot( lhs ).getNodeType() instanceof SqmPolymorphicRootDescriptor
				);
	}

	private @Nullable SqmPath<?> getLhsOrRoot() {
		final SqmPath<?> lhs = getLhs();
		return lhs != null ? lhs : findRoot();
	}

	private SqmPath<?> getRoot(SqmPath<?> lhs) {
		final SqmPath<?> parent = lhs.getLhs();
		return parent == null ? lhs : getRoot( parent );
	}

	@Override
	public @NonNull SqmBindableType<T> getNodeType() {
		return referencedPathSource.getPathType();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return referencedPathSource;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public @Nullable SqmPath<?> getLhs() {
		return lhs;
	}

	@Override
	public List<SqmPath<?>> getReusablePaths() {
		return reusablePaths == null ? emptyList() : new ArrayList<>( reusablePaths.values() );
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
	public @Nullable SqmPath<?> getReusablePath(String name) {
		return reusablePaths == null ? null : reusablePaths.get( name );
	}

	@Override
	public @Nullable String getExplicitAlias() {
		return getAlias();
	}

	@Override
	public void setExplicitAlias(@Nullable String explicitAlias) {
		setAlias( explicitAlias );
	}

	@Override
	public SqmPathSource<T> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public SqmPathSource<T> getResolvedModel() {
		final SqmPathSource<T> pathSource = getReferencedPathSource();
		final SqmPath<?> lhs = getLhs();
		if ( pathSource.isGeneric() && lhs != null
			&& lhs.getResolvedModel().getPathType() instanceof SqmManagedDomainType<?> lhsType ) {
			final var concreteAttribute = lhsType.findConcreteGenericAttribute( pathSource.getPathName() );
			if ( concreteAttribute != null ) {
				//noinspection unchecked
				return (SqmPathSource<T>) concreteAttribute;
			}
		}
		return getModel();
	}

	@Override
	public @NonNull SqmBindableType<T> getExpressible() {
		return getResolvedModel().getExpressible();
	}

	@Override
	public @NonNull JavaType<T> getJavaTypeDescriptor() {
		return castNonNull( super.getJavaTypeDescriptor() );
	}

	@Override
	public @NonNull JavaType<T> getNodeJavaType() {
		return castNonNull( super.getNodeJavaType() );
	}

	@Override
	public SqmExpression<Class<? extends T>> type() {
		final SqmPathSource<T> referencedPathSource = getReferencedPathSource();
		final SqmPathSource<?> subPathSource =
				referencedPathSource.findSubPathSource( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME );
		if ( subPathSource == null ) {
			return new SqmLiteral<>(
					referencedPathSource.getBindableJavaType(),
					nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Class.class ),
					nodeBuilder()
			);
		}
		else {
			@SuppressWarnings("unchecked")
			final var discriminatorSource = (SqmPathSource<Class<? extends T>>) subPathSource;
			return resolvePath( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME, discriminatorSource );
		}
	}

	@Override
	public <Y> SqmPath<Y> get(String attributeName) {
		@SuppressWarnings("unchecked")
		final SqmPathSource<Y> subNavigable = (SqmPathSource<Y>) getResolvedModel().getSubPathSource( attributeName );
		return resolvePath( attributeName, subNavigable );
	}

	@Override
	public <Y> SqmPath<Y> get(String attributeName, boolean includeSubtypes) {
		@SuppressWarnings("unchecked")
		final SqmPathSource<Y> subPathSource = (SqmPathSource<Y>)
				getResolvedModel().getSubPathSource( attributeName, includeSubtypes );
		return resolvePath( attributeName, subPathSource );
	}

	protected <X> SqmPath<X> resolvePath(PersistentAttribute<?, X> attribute) {
		//noinspection unchecked
		return resolvePath( attribute.getName(), (SqmPathSource<X>) attribute );
	}

	protected <X> SqmPath<X> resolvePath(String attributeName, SqmPathSource<X> pathSource) {
		final SqmPathSource<?> intermediatePathSource =
				getResolvedModel().getIntermediatePathSource( pathSource );
		if ( reusablePaths == null ) {
			reusablePaths = new HashMap<>();
			final SqmPath<X> path = pathSource.createSqmPath( this, intermediatePathSource );
			reusablePaths.put( attributeName, path );
			return path;
		}
		else {
			//noinspection unchecked
			return (SqmPath<X>)
					reusablePaths.computeIfAbsent( attributeName,
							name -> pathSource.createSqmPath( this, intermediatePathSource ) );
		}
	}

	protected <S extends T> SqmTreatedPath<T, S> getTreatedPath(ManagedDomainType<S> treatTarget) {
		final NavigablePath treat = getNavigablePath().treatAs( treatTarget.getTypeName() );
		final SqmPath<?> lhs = castNonNull( getLhs() );
		final SqmPath<?> reusablePath = lhs.getReusablePath( treat.getLocalName() );
		//TODO: check this cast
		@SuppressWarnings("unchecked")
		final SqmTreatedPath<T, S> path = (SqmTreatedPath<T, S>) reusablePath;
		if ( path == null ) {
			final SqmTreatedPath<T, S> treatedPath;
			if ( treatTarget instanceof SqmEntityDomainType<S> entityDomainType ) {
				treatedPath = new SqmTreatedEntityValuedSimplePath<>( this, entityDomainType, nodeBuilder() );
			}
			else if ( treatTarget instanceof SqmEmbeddableDomainType<S> embeddableDomainType ) {
				treatedPath = new SqmTreatedEmbeddedValuedSimplePath<>( this, embeddableDomainType );
			}
			else {
				throw new AssertionFailure( "Unrecognized treat target type: " + treatTarget.getTypeName() );
			}
			lhs.registerReusablePath( treatedPath );
			return treatedPath;
		}
		else {
			return path;
		}
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
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		return getTreatedPath( treatTarget );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		return treatAs( treatTarget );
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

	private @Nullable NavigablePath getRealParentPath(NavigablePath realParent, NavigablePath parent) {
		@Nullable NavigablePath realParentPath;
		if ( parent == realParent ) {
			realParentPath = null;
		}
		else if ( realParent instanceof EntityIdentifierNavigablePath entityIdentifierNavigablePath ) {
			realParentPath = getRealParentPath( castNonNull( realParent.getRealParent() ), parent );
			if ( realParentPath != null ) {
				realParentPath = new EntityIdentifierNavigablePath(
						realParentPath,
						entityIdentifierNavigablePath.getIdentifierAttributeName()
				);
			}
		}
		else if ( realParent.getAlias() == null && realParent instanceof TreatedNavigablePath ) {
			// This might be an implicitly treated parent path, check with the non-treated parent
			realParentPath = getRealParentPath( castNonNull( realParent.getRealParent() ), parent );
			if ( realParentPath != null ) {
				realParentPath = realParentPath.treatAs( realParent.getLocalName().substring( 1 ) );
			}
		}
		else if ( CollectionPart.Nature.fromNameExact( realParent.getLocalName() ) != null ) {
			if ( parent == realParent.getRealParent() ) {
				realParentPath = null;
			}
			else {
				realParentPath = parent.append( realParent.getLocalName() );
			}
		}
		else {
			realParentPath = parent;
		}
		return realParentPath;
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

	// The equals/hashCode and isCompatible/cacheHashCode implementations are based on NavigablePath to match paths
	// "syntactically" for regular uses in expressions and predicates, which is good enough since the NavigablePath
	// contains all the important information. Deep equality for SqmFrom is determined through SqmFromClause

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AbstractSqmPath<?> that
			&& this.getClass() == that.getClass()
			&& Objects.equals( this.navigablePath, that.navigablePath );
	}

	@Override
	public int hashCode() {
		return navigablePath.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof AbstractSqmPath<?> that
			&& this.getClass() == that.getClass()
			&& Objects.equals( this.navigablePath, that.navigablePath );
	}

	@Override
	public int cacheHashCode() {
		return navigablePath.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ")";
	}
}
