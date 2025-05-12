/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

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
		this.implementors = new TreeSet<>( comparing(EntityDomainType::getTypeName) );
		this.implementors.addAll( implementors );
		this.commonAttributes = unmodifiableMap( inferCommonAttributes( implementors ) );
	}

	@Override
	public JpaMetamodel getMetamodel() {
		return jpaMetamodel;
	}

	/**
	 * The attributes of a "polymorphic" root are the attributes which are
	 * common to all subtypes of the root type.
	 */
	private Map<String, SqmPersistentAttribute<? super T, ?>> inferCommonAttributes(Set<EntityDomainType<? extends T>> implementors) {
		final Map<String, SqmPersistentAttribute<? super T,?>> workMap = new HashMap<>();
		final ArrayList<EntityDomainType<?>> implementorsList = new ArrayList<>(implementors);
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
	private SqmPersistentAttribute<? super T, ?> promote(PersistentAttribute<?, ?> attribute) {
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

	public Set<EntityDomainType<? extends T>> getImplementors() {
		return implementors;
	}

	@Override
	public Class<T> getBindableJavaType() {
		return polymorphicJavaType.getJavaTypeClass();
	}

	@Override
	public String getName() {
		return polymorphicJavaType.getTypeName();
	}

	@Override
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
	public BindableType getBindableType() {
		return ENTITY_TYPE;
	}

	@Override
	public Class<T> getJavaType() {
		return polymorphicJavaType.getJavaTypeClass();
	}

	@Override
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
	public SqmPersistentAttribute<? super T, ?> findAttribute(String name) {
		return commonAttributes.get( name );
	}

	@Override
	public SqmPersistentAttribute<?, ?> findSubTypesAttribute(String name) {
		return commonAttributes.get( name );
	}

	@Override
	public void visitAttributes(Consumer<? super PersistentAttribute<? super T, ?>> action) {
		commonAttributes.values().forEach( action );
	}

	@Override
	public void visitDeclaredAttributes(Consumer<? super PersistentAttribute<T, ?>> action) {
	}

	@Override
	public SqmPersistentAttribute<? super T, ?> getAttribute(String name) {
		final var attribute = findAttribute( name );
		if ( attribute == null ) {
			// per-JPA
			throw new IllegalArgumentException();
		}
		return attribute;
	}

	@Override
	public SqmPersistentAttribute<T, ?> getDeclaredAttribute(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public SqmSingularPersistentAttribute<? super T, ?> findSingularAttribute(String name) {
		return (SqmSingularPersistentAttribute<? super T, ?>) findAttribute( name );
	}

	@Override
	public SqmPluralPersistentAttribute<? super T, ?, ?> findPluralAttribute(String name) {
		return (SqmPluralPersistentAttribute<? super T, ?, ?>) findAttribute( name );
	}

	@Override
	public SqmPersistentAttribute<? super T, ?> findConcreteGenericAttribute(String name) {
		return null;
	}

	@Override
	public SqmPersistentAttribute<T, ?> findDeclaredAttribute(String name) {
		return null;
	}

	@Override
	public SqmSingularPersistentAttribute<T, ?> findDeclaredSingularAttribute(String name) {
		return null;
	}

	@Override
	public SqmPluralPersistentAttribute<T, ?, ?> findDeclaredPluralAttribute(String name) {
		return null;
	}

	@Override
	public SqmPersistentAttribute<T, ?> findDeclaredConcreteGenericAttribute(String name) {
		return null;
	}

	@Override
	public Set<Attribute<? super T, ?>> getAttributes() {
		return new HashSet<>( commonAttributes.values() );
	}

	@Override
	public Set<Attribute<T, ?>> getDeclaredAttributes() {
		return Collections.emptySet();
	}

	@Override
	public <Y> SingularAttribute<? super T, Y> getSingularAttribute(String name, Class<Y> type) {
		//noinspection unchecked
		return (SingularAttribute<? super T, Y>) getAttribute( name );
	}

	@Override
	public <Y> SingularAttribute<T, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
		//noinspection unchecked
		return (SingularAttribute<T, Y>) getDeclaredAttribute( name );
	}

	@Override
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
	public Set<SingularAttribute<T, ?>> getDeclaredSingularAttributes() {
		return Collections.emptySet();
	}

	@Override
	public <E> CollectionAttribute<? super T, E> getCollection(String name, Class<E> elementType) {
		//noinspection unchecked
		return (CollectionAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	public <E> CollectionAttribute<T, E> getDeclaredCollection(String name, Class<E> elementType) {
		throw new IllegalArgumentException();
	}

	@Override
	public <E> SetAttribute<? super T, E> getSet(String name, Class<E> elementType) {
		//noinspection unchecked
		return (SetAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	public <E> SetAttribute<T, E> getDeclaredSet(String name, Class<E> elementType) {
		throw new IllegalArgumentException(  );
	}

	@Override
	public <E> ListAttribute<? super T, E> getList(String name, Class<E> elementType) {
		//noinspection unchecked
		return (ListAttribute<? super T, E>) getAttribute( name );
	}

	@Override
	public <E> ListAttribute<T, E> getDeclaredList(String name, Class<E> elementType) {
		throw new IllegalArgumentException();
	}

	@Override
	public <K, V> MapAttribute<? super T, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		//noinspection unchecked
		return (MapAttribute<? super T, K, V>) getAttribute( name );
	}

	@Override
	public <K, V> MapAttribute<T, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
		throw new IllegalArgumentException();
	}

	@Override
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
	public Set<PluralAttribute<T, ?, ?>> getDeclaredPluralAttributes() {
		return Collections.emptySet();
	}

	@Override
	public SingularAttribute<? super T, ?> getSingularAttribute(String name) {
		return (SqmSingularPersistentAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	public SingularAttribute<T, ?> getDeclaredSingularAttribute(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public CollectionAttribute<? super T, ?> getCollection(String name) {
		//noinspection unchecked
		return (CollectionAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	public CollectionAttribute<T, ?> getDeclaredCollection(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public SetAttribute<? super T, ?> getSet(String name) {
		//noinspection unchecked
		return (SetAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	public SetAttribute<T, ?> getDeclaredSet(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public ListAttribute<? super T, ?> getList(String name) {
		//noinspection unchecked
		return (ListAttribute<? super T, ?>) getAttribute( name );
	}

	@Override
	public ListAttribute<T, ?> getDeclaredList(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public MapAttribute<? super T, ?, ?> getMap(String name) {
		//noinspection unchecked
		return (MapAttribute<? super T, ?, ?>) getAttribute( name );
	}

	@Override
	public MapAttribute<T, ?, ?> getDeclaredMap(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) findAttribute( name );
	}

	@Override
	public SqmPath<T> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedOperationException();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unsupported operations

	@Override
	public RepresentationMode getRepresentationMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public SqmPathSource<?> getIdentifierDescriptor() {
		return null;
	}

	@Override
	public <Y> SingularPersistentAttribute<? super T, Y> getId(Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <Y> SingularPersistentAttribute<T, Y> getDeclaredId(Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <Y> SingularPersistentAttribute<? super T, Y> getVersion(Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <Y> SingularPersistentAttribute<T, Y> getDeclaredVersion(Class<Y> type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set<SingularAttribute<? super T, ?>> getIdClassAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SimpleDomainType<?> getIdType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public IdentifiableDomainType<? super T> getSupertype() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasIdClass() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularPersistentAttribute<? super T, ?> findIdAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void visitIdClassAttributes(Consumer<SingularPersistentAttribute<? super T, ?>> action) {
	}

	@Override
	public SingularPersistentAttribute<? super T, ?> findVersionAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<? extends SingularPersistentAttribute<? super T, ?>> findNaturalIdAttributes() {
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
	public ManagedDomainType<? super T> getSuperType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Collection<? extends EntityDomainType<? extends T>> getSubTypes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void addSubType(ManagedDomainType<? extends T> subType) {
		throw new UnsupportedOperationException(  );
	}
}
