/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.metamodel.*;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.*;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableMap;

/**
 * Acts as the {@link EntityDomainType} for a "polymorphic query" grouping.
 *
 * @author Steve Ebersole
 */
public class SqmPolymorphicRootDescriptor<T> implements EntityDomainType<T> {
	private final Set<EntityDomainType<? extends T>> implementors;
	private final Map<String, PersistentAttribute<? super T,?>> commonAttributes;

	private final JavaType<T> polymorphicJavaType;

	public SqmPolymorphicRootDescriptor(
			JavaType<T> polymorphicJavaType,
			Set<EntityDomainType<? extends T>> implementors) {
		this.polymorphicJavaType = polymorphicJavaType;
		TreeSet<EntityDomainType<? extends T>> treeSet = new TreeSet<>( Comparator.comparing(EntityDomainType::getTypeName) );
		treeSet.addAll( implementors );
		this.implementors = treeSet;
		this.commonAttributes = unmodifiableMap( inferCommonAttributes( implementors ) );
	}

	/**
	 * The attributes of a "polymorphic" root are the attributes which are
	 * common to all subtypes of the root type.
	 */
	private Map<String, PersistentAttribute<? super T, ?>> inferCommonAttributes(Set<EntityDomainType<? extends T>> implementors) {
		final Map<String, PersistentAttribute<? super T,?>> workMap = new HashMap<>();
		final ArrayList<EntityDomainType<?>> implementorsList = new ArrayList<>(implementors);
		final EntityDomainType<?> firstImplementor = implementorsList.get( 0 );
		if ( implementorsList.size() == 1 ) {
			firstImplementor.visitAttributes(
					attribute -> workMap.put( attribute.getName(), promote( attribute ) )
			);
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
	private PersistentAttribute<? super T, ?> promote(PersistentAttribute<?, ?> attribute) {
		return (PersistentAttribute<? super T, ?>) attribute;
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
	public DomainType<T> getSqmPathType() {
		return this;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public Class<T> getBindableJavaType() {
		return polymorphicJavaType.getJavaTypeClass();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public Class<T> getJavaType() {
		return getBindableJavaType();
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return polymorphicJavaType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute handling

	@Override
	public PersistentAttribute<? super T, ?> findAttribute(String name) {
		return commonAttributes.get( name );
	}

	@Override
	public PersistentAttribute<?, ?> findSubTypesAttribute(String name) {
		return commonAttributes.get( name );
	}

	@Override
	public PersistentAttribute<? super T, ?> findAttributeInSuperTypes(String name) {
		// there are effectively no super-types
		return null;
	}

	@Override
	public void visitAttributes(Consumer<? super PersistentAttribute<? super T, ?>> action) {
		commonAttributes.values().forEach( action );
	}

	@Override
	public void visitDeclaredAttributes(Consumer<? super PersistentAttribute<T, ?>> action) {
	}

	@Override
	public PersistentAttribute<? super T, ?> getAttribute(String name) {
		final PersistentAttribute<? super T, ?> attribute = findAttribute( name );
		if ( attribute == null ) {
			// per-JPA
			throw new IllegalArgumentException();
		}
		return attribute;
	}

	@Override
	public PersistentAttribute<T, ?> getDeclaredAttribute(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public SingularPersistentAttribute<? super T, ?> findSingularAttribute(String name) {
		return (SingularPersistentAttribute<? super T, ?>) findAttribute( name );
	}

	@Override
	public PluralPersistentAttribute<? super T, ?, ?> findPluralAttribute(String name) {
		return (PluralPersistentAttribute<? super T, ?, ?>) findAttribute( name );
	}

	@Override
	public PersistentAttribute<? super T, ?> findConcreteGenericAttribute(String name) {
		return null;
	}

	@Override
	public PersistentAttribute<T, ?> findDeclaredAttribute(String name) {
		return null;
	}

	@Override
	public SingularPersistentAttribute<T, ?> findDeclaredSingularAttribute(String name) {
		return null;
	}

	@Override
	public PluralPersistentAttribute<T, ?, ?> findDeclaredPluralAttribute(String name) {
		return null;
	}

	@Override
	public PersistentAttribute<T, ?> findDeclaredConcreteGenericAttribute(String name) {
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
		return (SingularPersistentAttribute<? super T, ?>) getAttribute( name );
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
