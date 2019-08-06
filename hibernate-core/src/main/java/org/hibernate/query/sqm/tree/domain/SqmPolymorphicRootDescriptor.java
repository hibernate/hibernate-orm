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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Acts as the EntityValuedNavigable for a "polymorphic query" grouping
 *
 * @author Steve Ebersole
 */
public class SqmPolymorphicRootDescriptor<T> implements EntityDomainType<T> {
	private final Set<EntityDomainType<?>> implementors;
	private final Map<String, PersistentAttribute> commonAttributes;

	private final JavaTypeDescriptor<T> polymorphicJavaDescriptor;

	public SqmPolymorphicRootDescriptor(
			JavaTypeDescriptor<T> polymorphicJavaDescriptor,
			Set<EntityDomainType<?>> implementors) {
		this.polymorphicJavaDescriptor = polymorphicJavaDescriptor;

		this.implementors = implementors;

		final Map<String, PersistentAttribute> workMap = new HashMap<>();

		final ArrayList<EntityDomainType<?>> implementorsList = new ArrayList<>( implementors );

		final EntityDomainType<?> firstImplementor = implementorsList.get( 0 );

		// basically we want to "expose" only the attributes that all the implementors expose...
		// 		- visit all of the attributes defined on the first implementor and check it against
		// 		all of the others
		final List<EntityDomainType<?>> subList = implementorsList.subList( 1, implementors.size() - 1 );
		firstImplementor.visitAttributes(
				attribute -> {
					// for each of its attributes, check whether the other implementors also expose it
					for ( EntityDomainType navigable : subList ) {
						if ( navigable.findAttribute( attribute.getName() ) == null ) {
							// we found an implementor that does not expose that attribute,
							// so break-out to the next attribute
							break;
						}

						// if we get here - they all had it.  so put it in the workMap
						//
						// todo (6.0) : Atm We use the attribute from the first implementor directly for each implementor
						//		need to handle this in QuerySplitter somehow
						workMap.put( attribute.getName(), attribute );
					}

				}
		);
		this.commonAttributes = Collections.unmodifiableMap( workMap );
	}

	public Set<EntityDomainType<?>> getImplementors() {
		return new HashSet<>( implementors );
	}

	@Override
	public String getName() {
		return polymorphicJavaDescriptor.getJavaType().getName();
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
	public DomainType<?> getSqmPathType() {
		return this;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public Class<T> getBindableJavaType() {
		return polymorphicJavaDescriptor.getJavaType();
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
	public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
		return polymorphicJavaDescriptor;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute handling

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute findAttribute(String name) {
		return commonAttributes.get( name );
	}

	@Override
	public void visitAttributes(Consumer<PersistentAttribute<T, ?>> action) {
		commonAttributes.values().forEach( (Consumer) action );
	}

	@Override
	public void visitDeclaredAttributes(Consumer<PersistentAttribute<T, ?>> action) {
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
		//noinspection unchecked
		return (SingularPersistentAttribute<? super T, ?>) findAttribute( name );
	}

	@Override
	public PluralPersistentAttribute<? super T, ?, ?> findPluralAttribute(String name) {
		//noinspection unchecked
		return (PluralPersistentAttribute<? super T, ?, ?>) findAttribute( name );
	}

	@Override
	public PersistentAttribute<T, ?> findDeclaredAttribute(String name) {
		return null;
	}

	@Override
	public SingularPersistentAttribute<? super T, ?> findDeclaredSingularAttribute(String name) {
		return null;
	}

	@Override
	public PluralPersistentAttribute<? super T, ?, ?> findDeclaredPluralAttribute(String name) {
		return null;
	}

	@Override
	public Set<Attribute<? super T, ?>> getAttributes() {
		//noinspection unchecked
		return (Set<Attribute<? super T, ?>>) commonAttributes;
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
		//noinspection unchecked
		return (Set) commonAttributes.values().stream()
				.filter( attribute -> attribute instanceof SingularAttribute )
				.collect( Collectors.toSet() );
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
		//noinspection unchecked
		return (Set) commonAttributes.values().stream()
				.filter( attribute -> attribute instanceof PluralAttribute )
				.collect( Collectors.toSet() );
	}

	@Override
	public Set<PluralAttribute<T, ?, ?>> getDeclaredPluralAttributes() {
		return Collections.emptySet();
	}

	@Override
	public SingularAttribute<? super T, ?> getSingularAttribute(String name) {
		//noinspection unchecked
		return (SingularAttribute<? super T, ?>) getAttribute( name );
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
	public SqmPath<T> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		throw new UnsupportedOperationException();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unsupported operations

	@Override
	public RepresentationMode getRepresentationMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public SubGraphImplementor<T> makeSubGraph() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> ManagedDomainType<S> findSubType(String subTypeName) {
		// technically we could support this
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> ManagedDomainType<S> findSubType(Class<S> type) {
		// technically we could support this
		throw new UnsupportedOperationException(  );
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
	public SingularPersistentAttribute<T, ?> findIdAttribute() {
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
}
