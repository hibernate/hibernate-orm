/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static jakarta.persistence.metamodel.Bindable.BindableType.ENTITY_TYPE;
import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;

/**
 * Acts as the {@link EntityDomainType} for a "polymorphic query" grouping.
 *
 * @author Steve Ebersole
 */
public class SqmPolymorphicRootDescriptor<T> implements SqmEntityDomainType<T> {

	private static final Comparator<EntityDomainType<?>> ENTITY_DOMAIN_TYPE_NAME_COMPARATOR
			= comparing( EntityDomainType::getTypeName );

	private final Set<EntityDomainType<? extends T>> implementors;
	private final Map<String, SqmPersistentAttribute<? super T,?>> commonAttributes;

	private final JavaType<T> polymorphicJavaType;
	private final JpaMetamodel jpaMetamodel;

	public SqmPolymorphicRootDescriptor(
			JavaType<T> polymorphicJavaType,
			Set<EntityDomainType<? extends T>> implementors,
			JpaMetamodel jpaMetamodel) {
		this.polymorphicJavaType = polymorphicJavaType;
		this.jpaMetamodel = jpaMetamodel;
		this.implementors = new TreeSet<>( ENTITY_DOMAIN_TYPE_NAME_COMPARATOR );
		this.implementors.addAll( implementors );
		this.commonAttributes = unmodifiableMap( inferCommonAttributes( implementors ) );
	}

	@Override
	@Nonnull
	public JpaMetamodel getMetamodel() {
		return jpaMetamodel;
	}

	/**
	 * The attributes of a "polymorphic" root are the attributes which are
	 * common to all subtypes of the root type.
	 */
	private static <T> Map<String, SqmPersistentAttribute<? super T, ?>> inferCommonAttributes(Set<EntityDomainType<? extends T>> implementors) {
		final Map<String, SqmPersistentAttribute<? super T,?>> workMap = new HashMap<>();
		final ArrayList<EntityDomainType<?>> implementorsList = new ArrayList<>( implementors );
		final EntityDomainType<?> firstImplementor = implementorsList.get( 0 );
		if ( implementorsList.size() == 1 ) {
			firstImplementor.visitAttributes( attribute -> workMap.put( attribute.getName(), promote( attribute ) ) );
		}
		else {
			// we want to "expose" only the attributes that all the implementors expose
			// visit every attribute declared on the first implementor and check that it
			// is also declared by every other implementor
			final List<EntityDomainType<?>> subList = implementorsList.subList( 1, implementors.size() - 1 );
			firstImplementor.visitAttributes(
					attribute -> {
						if ( isACommonAttribute( subList, attribute ) ) {
							// they all had it, so put it in the workMap
							// todo (6.0) : ATM we use the attribute from the first implementor directly for
							//              each implementor - need to handle this in QuerySplitter somehow
							workMap.put( attribute.getName(), promote( attribute ) );
						}
					}
			);
		}
		return workMap;
	}

	/**
	 * Here we pretend that an attribute belonging to all known subtypes
	 * is an attribute of this type. The unchecked and unsound-looking
	 * type cast is actually perfectly correct.
	 */
	@SuppressWarnings("unchecked")
	private static <T> SqmPersistentAttribute<? super T, ?> promote(PersistentAttribute<?, ?> attribute) {
		return (SqmPersistentAttribute<? super T, ?>) attribute;
	}

	private static boolean isACommonAttribute(List<EntityDomainType<?>> subList, PersistentAttribute<?, ?> attribute) {
		// for each of its attributes, check whether the other implementors also expose it
		for ( EntityDomainType<?> navigable : subList ) {
			if ( navigable.findAttribute( attribute.getName() ) == null ) {
				// we found an implementor that does not expose that attribute,
				// so break-out to the next attribute
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean hasId() {
		return false;
	}

	public Set<EntityDomainType<? extends T>> getImplementors() {
		return implementors;
	}

	@Override
	@Nonnull
	public Class<T> getBindableJavaType() {
		return polymorphicJavaType.getJavaTypeClass();
	}

	@Override
	@Nonnull
	public String getName() {
		return polymorphicJavaType.getTypeName();
	}

	@Override
	@Nonnull
	public EntityGraph<T> createEntityGraph() {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public Map<String, EntityGraph<T>> getNamedEntityGraphs() {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public String getHibernateEntityName() {
		return getName();
	}

	@Override
	public String getTypeName() {
		return getName();
	}

	@Override
	public String getPathName() {
		return getName();
	}

	@Override
	public SqmDomainType<T> getPathType() {
		return this;
	}

	@Override
	@Nonnull
	public BindableType getBindableType() {
		return ENTITY_TYPE;
	}

	@Override
	@Nonnull
	public Class<T> getJavaType() {
		return polymorphicJavaType.getJavaTypeClass();
	}

	@Override
	@Nonnull
	public PersistenceType getPersistenceType() {
		return ENTITY;
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return polymorphicJavaType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute handling

	@Override
	@Nullable public SqmPersistentAttribute<? super T, ?> findAttribute(@Nonnull String name) {
		return commonAttributes.get( name );
	}

	@Override
	@Nullable public SqmPersistentAttribute<?, ?> findSubTypesAttribute(@Nonnull String name) {
		return commonAttributes.get( name );
	}

	@Override
	public void visitAttributes(@Nonnull Consumer<? super PersistentAttribute<? super T, ?>> action) {
		commonAttributes.values().forEach( action );
	}

	@Override
	public void visitDeclaredAttributes(@Nonnull Consumer<? super PersistentAttribute<T, ?>> action) {
	}

	@Override
	@Nonnull
	public SqmPersistentAttribute<? super T, ?> getAttribute(@Nonnull String name) {
		final var attribute = findAttribute( name );
		if ( attribute == null ) {
			// per-JPA
			throw new IllegalArgumentException();
		}
		return attribute;
	}

	@Override
	@Nonnull
	public SqmPersistentAttribute<T, ?> getDeclaredAttribute(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nullable public SqmSingularPersistentAttribute<? super T, ?> findSingularAttribute(@Nonnull String name) {
		return (SqmSingularPersistentAttribute<? super T, ?>) findAttribute( name );
	}

	@Override
	@Nullable public SqmPluralPersistentAttribute<? super T, ?, ?> findPluralAttribute(@Nonnull String name) {
		return (SqmPluralPersistentAttribute<? super T, ?, ?>) findAttribute( name );
	}

	@Override
	@Nullable public SqmPersistentAttribute<? super T, ?> findConcreteGenericAttribute(@Nonnull String name) {
		return null;
	}

	@Override
	@Nullable public SqmPersistentAttribute<T, ?> findDeclaredAttribute(@Nonnull String name) {
		return null;
	}

	@Override
	@Nullable public SqmSingularPersistentAttribute<T, ?> findDeclaredSingularAttribute(@Nonnull String name) {
		return null;
	}

	@Override
	@Nullable public SqmPluralPersistentAttribute<T, ?, ?> findDeclaredPluralAttribute(@Nonnull String name) {
		return null;
	}

	@Override
	@Nullable public SqmPersistentAttribute<T, ?> findDeclaredConcreteGenericAttribute(@Nonnull String name) {
		return null;
	}

	@Override
	@Nonnull
	public Set<Attribute<? super T, ?>> getAttributes() {
		return new HashSet<>( commonAttributes.values() );
	}

	@Override
	@Nonnull
	public Set<Attribute<T, ?>> getDeclaredAttributes() {
		return Collections.emptySet();
	}

	@Override
	@Nonnull
	public <Y> SingularAttribute<? super T, Y> getSingularAttribute(@Nonnull String name, @Nonnull Class<Y> type) {
		//noinspection unchecked
		return (SingularAttribute<? super T, Y>) getAttribute( name );
	}

	@Override
	@Nonnull
	public <Y> SingularAttribute<T, Y> getDeclaredSingularAttribute(@Nonnull String name, @Nonnull Class<Y> type) {
		//noinspection unchecked
		return (SingularAttribute<T, Y>) getDeclaredAttribute( name );
	}

	@Override
	@Nonnull
	public Set<SingularAttribute<? super T, ?>> getSingularAttributes() {
		final Set<SingularAttribute<? super T, ?>> singularAttributes = new HashSet<>();
		for ( PersistentAttribute<? super T, ?> attribute : commonAttributes.values() ) {
			if ( attribute instanceof SingularAttribute ) {
				singularAttributes.add( (SingularPersistentAttribute<? super T, ?>) attribute );
			}
		}
		return singularAttributes;
	}

	@Override
	@Nonnull
	public Set<SingularAttribute<T, ?>> getDeclaredSingularAttributes() {
		return Collections.emptySet();
	}

	@Override
	@Nonnull
	public <E> CollectionAttribute<? super T, E> getCollection(@Nonnull String name, @Nonnull Class<E> elementType) {
		//noinspection unchecked
		return (CollectionAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	@Nonnull
	public <E> CollectionAttribute<T, E> getDeclaredCollection(@Nonnull String name, @Nonnull Class<E> elementType) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public <E> SetAttribute<? super T, E> getSet(@Nonnull String name, @Nonnull Class<E> elementType) {
		//noinspection unchecked
		return (SetAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	@Nonnull
	public <E> SetAttribute<T, E> getDeclaredSet(@Nonnull String name, @Nonnull Class<E> elementType) {
		throw new IllegalArgumentException(  );
	}

	@Override
	@Nonnull
	public <E> ListAttribute<? super T, E> getList(@Nonnull String name, @Nonnull Class<E> elementType) {
		//noinspection unchecked
		return (ListAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	@Nonnull
	public <E> ListAttribute<T, E> getDeclaredList(@Nonnull String name, @Nonnull Class<E> elementType) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public <K, V> MapAttribute<? super T, K, V> getMap(@Nonnull String name, @Nonnull Class<K> keyType, @Nonnull Class<V> valueType) {
		//noinspection unchecked
		return (MapAttribute<? super T, K, V>) getAttribute( name );
	}

	@Override
	@Nonnull
	public <K, V> MapAttribute<T, K, V> getDeclaredMap(@Nonnull String name, @Nonnull Class<K> keyType, @Nonnull Class<V> valueType) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public Set<PluralAttribute<? super T, ?, ?>> getPluralAttributes() {
		final Set<PluralAttribute<? super T, ?, ?>> pluralAttributes = new HashSet<>();
		for ( PersistentAttribute<? super T, ?> attribute : commonAttributes.values() ) {
			if ( attribute instanceof PluralAttribute ) {
				pluralAttributes.add( (PluralPersistentAttribute<? super T, ?, ?>) attribute );
			}
		}
		return pluralAttributes;
	}

	@Override
	@Nonnull
	public Set<PluralAttribute<T, ?, ?>> getDeclaredPluralAttributes() {
		return Collections.emptySet();
	}

	@Override
	@Nonnull
	public SingularAttribute<? super T, ?> getSingularAttribute(@Nonnull String name) {
		return (SqmSingularPersistentAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	@Nonnull
	public SingularAttribute<T, ?> getDeclaredSingularAttribute(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public CollectionAttribute<? super T, ?> getCollection(@Nonnull String name) {
		//noinspection unchecked
		return (CollectionAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	@Nonnull
	public CollectionAttribute<T, ?> getDeclaredCollection(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public SetAttribute<? super T, ?> getSet(@Nonnull String name) {
		//noinspection unchecked
		return (SetAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	@Nonnull
	public SetAttribute<T, ?> getDeclaredSet(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public ListAttribute<? super T, ?> getList(@Nonnull String name) {
		//noinspection unchecked
		return (ListAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	@Nonnull
	public ListAttribute<T, ?> getDeclaredList(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nonnull
	public MapAttribute<? super T, ?, ?> getMap(@Nonnull String name) {
		//noinspection unchecked
		return (MapAttribute<? super T, ?, ?>) getAttribute( name );
	}

	@Override
	@Nonnull
	public MapAttribute<T, ?, ?> getDeclaredMap(@Nonnull String name) {
		throw new IllegalArgumentException();
	}

	@Override
	@Nullable public SqmPathSource<?> findSubPathSource(@Nonnull String name) {
		return (SqmPathSource<?>) findAttribute( name );
	}

	@Override
	public SqmPath<T> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedOperationException();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unsupported operations

	@Override
	@Nonnull
	public RepresentationMode getRepresentationMode() {
		return RepresentationMode.POJO;
	}

	@Override
	@Nullable public SqmPathSource<?> getIdentifierDescriptor() {
		return null;
	}

	@Override
	@Nonnull
	public <Y> SingularPersistentAttribute<? super T, Y> getId(@Nonnull Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public <Y> SingularPersistentAttribute<T, Y> getDeclaredId(@Nonnull Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public <Y> SingularPersistentAttribute<? super T, Y> getVersion(@Nonnull Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public <Y> SingularPersistentAttribute<T, Y> getDeclaredVersion(@Nonnull Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public Set<SingularAttribute<? super T, ?>> getIdClassAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public SimpleDomainType<?> getIdType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nullable public IdentifiableDomainType<? super T> getSupertype() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasIdClass() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nullable public SingularPersistentAttribute<? super T, ?> findIdAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void visitIdClassAttributes(@Nonnull Consumer<SingularPersistentAttribute<? super T, ?>> action) {
	}

	@Override
	@Nullable public SingularPersistentAttribute<? super T, ?> findVersionAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nullable public List<? extends SingularPersistentAttribute<? super T, ?>> findNaturalIdAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasSingleIdAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasVersionAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nullable
	public ManagedDomainType<? super T> getSuperType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public Collection<? extends EntityDomainType<? extends T>> getSubTypes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void addSubType(@Nonnull ManagedDomainType<? extends T> subType) {
		throw new UnsupportedOperationException(  );
	}
}
