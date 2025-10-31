/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.tree.domain.SqmManagedDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmPluralPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.DynamicModelJavaType;

import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Functionality common to all implementations of {@link ManagedType}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<J>
		extends AbstractDomainType<J>
		implements SqmManagedDomainType<J>, AttributeContainer<J>, Serializable {

	private final String hibernateTypeName;
	private final @Nullable SqmManagedDomainType<? super J> supertype;
	private final RepresentationMode representationMode;
	private final JpaMetamodelImplementor metamodel;

	private final Map<String, SqmSingularPersistentAttribute<J, ?>> declaredSingularAttributes = new LinkedHashMap<>();
	private volatile Map<String, SqmPluralPersistentAttribute<J, ?, ?>> declaredPluralAttributes ;
	private volatile Map<String, SqmPersistentAttribute<J, ?>> declaredConcreteGenericAttributes;

	private final List<SqmManagedDomainType<? extends J>> subtypes = new ArrayList<>();

	protected AbstractManagedType(
			String hibernateTypeName,
			JavaType<J> javaType,
			ManagedDomainType<? super J> supertype,
			JpaMetamodelImplementor metamodel) {
		super( javaType );
		this.hibernateTypeName = hibernateTypeName;
		// TODO: fix callers and remove this typecast
		this.supertype = (SqmManagedDomainType<? super J>) supertype;
		this.metamodel = metamodel;
		if ( supertype != null ) {
			supertype.addSubType( this );
		}
		representationMode = representationMode( javaType );
		inFlightAccess = createInFlightAccess();
	}

	private static <J> RepresentationMode representationMode(JavaType<J> javaType) {
		return javaType instanceof DynamicModelJavaType
				? RepresentationMode.MAP
				: RepresentationMode.POJO;
	}

	protected InFlightAccess<J> createInFlightAccess() {
		return new InFlightAccessImpl();
	}

	public JpaMetamodelImplementor getMetamodel() {
		return metamodel;
	}

	@Override
	public Class<J> getJavaType() {
		return super.getJavaType();
	}

	@Override
	public @Nullable SqmManagedDomainType<? super J> getSuperType() {
		return supertype;
	}

	@Override
	public Collection<? extends SqmManagedDomainType<? extends J>> getSubTypes() {
		return subtypes;
	}

	@Override
	public void addSubType(ManagedDomainType<? extends J> subType){
		subtypes.add( (SqmManagedDomainType<? extends J>) subType );
	}

	@Override
	public RepresentationMode getRepresentationMode() {
		return representationMode;
	}

	@Override
	public void visitAttributes(Consumer<? super PersistentAttribute<? super J, ?>> action) {
		visitDeclaredAttributes( action );
		if ( getSuperType() != null ) {
			getSuperType().visitAttributes( action );
		}
	}

	@Override
	public void visitDeclaredAttributes(Consumer<? super PersistentAttribute<J, ?>> action) {
		declaredSingularAttributes.values().forEach( action );
		if ( declaredPluralAttributes != null ) {
			declaredPluralAttributes.values().forEach( action );
		}
	}

	@Override
	public Set<Attribute<? super J, ?>> getAttributes() {
		final Set<Attribute<? super J, ?>> attributes = new LinkedHashSet<>( getDeclaredAttributes() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<Attribute<J, ?>> getDeclaredAttributes() {
		final boolean isDeclaredSingularAttributesEmpty = isEmpty( declaredSingularAttributes );
		final boolean isDeclaredPluralAttributes = isEmpty( declaredPluralAttributes );
		if ( isDeclaredSingularAttributesEmpty && isDeclaredPluralAttributes ) {
			return emptySet();
		}
		else if ( !isDeclaredSingularAttributesEmpty ) {
			final Set<Attribute<J, ?>> attributes =
					new LinkedHashSet<>( declaredSingularAttributes.values() );
			if ( !isDeclaredPluralAttributes ) {
				attributes.addAll( declaredPluralAttributes.values() );
			}
			return attributes;
		}
		else {
			return new LinkedHashSet<>( declaredPluralAttributes.values() );
		}
	}

	@Override
	public SqmPersistentAttribute<? super J,?> getAttribute(String name) {
		final var attribute = findAttribute( name );
		checkNotNull( "Attribute", attribute, name );
		return attribute;
	}

	@Override
	public @Nullable SqmPersistentAttribute<? super J,?> findAttribute(String name) {
		final var attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else {
			return supertype != null ? supertype.findAttribute( name ) : null;
		}
	}

	@Override
	public final @Nullable SqmPersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name) {
		final var attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else {
			return supertype != null ? supertype.findAttributeInSuperTypes( name ) : null;
		}
	}

	@Override
	public @Nullable SqmPersistentAttribute<?, ?> findSubTypesAttribute(String name) {
		final var attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else {
			for ( var subtype : subtypes ) {
				final var subTypeAttribute = subtype.findSubTypesAttribute( name );
				if ( subTypeAttribute != null ) {
					return subTypeAttribute;
				}
			}
			return null;
		}
	}

	@Override
	public @Nullable SqmPersistentAttribute<J,?> findDeclaredAttribute(String name) {
		// try singular attribute
		final var attribute = declaredSingularAttributes.get( name );
		if ( attribute != null ) {
			return attribute;
		}
		// next plural
		else if ( declaredPluralAttributes != null ) {
			return declaredPluralAttributes.get( name );
		}
		else {
			return null;
		}
	}

	@Override
	public PersistentAttribute<J,?> getDeclaredAttribute(String name) {
		final var attribute = findDeclaredAttribute( name );
		checkNotNull( "Attribute", attribute, name );
		return attribute;
	}

	private void checkNotNull(String attributeType, Attribute<?,?> attribute, String name) {
		if ( attribute == null ) {
			throw new IllegalArgumentException(
					String.format(
							"Unable to locate %s with the given name [%s] on this ManagedType [%s]",
							attributeType,
							name,
							getTypeName()
					)
			);
		}
	}

	@Override
	public String getTypeName() {
		return hibernateTypeName;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Singular attributes

	@Override
	public Set<SingularAttribute<? super J, ?>> getSingularAttributes() {
		final Set<SingularAttribute<? super J, ?>> attributes =
				new HashSet<>( declaredSingularAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getSingularAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<SingularAttribute<J, ?>> getDeclaredSingularAttributes() {
		return new HashSet<>( declaredSingularAttributes.values() );
	}

	@Override
	public SingularPersistentAttribute<? super J, ?> getSingularAttribute(String name) {
		final var attribute = findSingularAttribute( name );
		checkNotNull( "SingularAttribute", attribute, name );
		return attribute;
	}

	@Override
	public @Nullable SqmSingularPersistentAttribute<? super J, ?> findSingularAttribute(String name) {
		final var attribute = findDeclaredSingularAttribute( name );
		return attribute == null && getSuperType() != null
				? getSuperType().findSingularAttribute( name )
				: attribute;
	}

	@Override
	public <Y> SqmSingularPersistentAttribute<? super J, Y> getSingularAttribute(String name, Class<Y> type) {
		return checkTypeForSingleAttribute( findSingularAttribute( name ), name, type );
	}

	@Override
	public SingularAttribute<J, ?> getDeclaredSingularAttribute(String name) {
		final var attribute = findDeclaredSingularAttribute( name );
		checkNotNull( "SingularAttribute", attribute, name );
		return attribute;
	}

	@Override
	public @Nullable SqmSingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name) {
		return declaredSingularAttributes.get( name );
	}

	@Override
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredSingularAttribute(String name, Class<Y> javaType) {
		return checkTypeForSingleAttribute( findDeclaredSingularAttribute( name ), name, javaType );
	}

	private <K,Y> SqmSingularPersistentAttribute<K,Y> checkTypeForSingleAttribute(
			SqmSingularPersistentAttribute<K,?> attribute,
			String name,
			Class<Y> javaType) {
		if ( attribute == null || !hasMatchingReturnType( attribute, javaType ) ) {
			throw new IllegalArgumentException(
					"SingularAttribute named " + name
							+ ( javaType != null ? " and of type " + javaType.getName() : "" )
							+ " is not present"
			);
		}
		else {
			@SuppressWarnings("unchecked")
			final SqmSingularPersistentAttribute<K, Y> narrowed =
					(SqmSingularPersistentAttribute<K, Y>) attribute;
			return narrowed;
		}
	}

	private <T, Y> boolean hasMatchingReturnType(SingularAttribute<T, ?> attribute, Class<Y> javaType) {
		return javaType == null
			|| attribute.getJavaType().equals( javaType )
			|| isPrimitiveVariant( attribute, javaType );
	}

	protected <Y> boolean isPrimitiveVariant(SingularAttribute<?,?> attribute, Class<Y> javaType) {
		if ( attribute != null ) {
			final Class<?> declaredType = attribute.getJavaType();
			if ( declaredType.isPrimitive() ) {
				return ( Boolean.class.equals( javaType ) && Boolean.TYPE.equals( declaredType ) )
					|| ( Character.class.equals( javaType ) && Character.TYPE.equals( declaredType ) )
					|| ( Byte.class.equals( javaType ) && Byte.TYPE.equals( declaredType ) )
					|| ( Short.class.equals( javaType ) && Short.TYPE.equals( declaredType ) )
					|| ( Integer.class.equals( javaType ) && Integer.TYPE.equals( declaredType ) )
					|| ( Long.class.equals( javaType ) && Long.TYPE.equals( declaredType ) )
					|| ( Float.class.equals( javaType ) && Float.TYPE.equals( declaredType ) )
					|| ( Double.class.equals( javaType ) && Double.TYPE.equals( declaredType ) );
			}

			if ( javaType.isPrimitive() ) {
				return ( Boolean.class.equals( declaredType ) && Boolean.TYPE.equals( javaType ) )
					|| ( Character.class.equals( declaredType ) && Character.TYPE.equals( javaType ) )
					|| ( Byte.class.equals( declaredType ) && Byte.TYPE.equals( javaType ) )
					|| ( Short.class.equals( declaredType ) && Short.TYPE.equals( javaType ) )
					|| ( Integer.class.equals( declaredType ) && Integer.TYPE.equals( javaType ) )
					|| ( Long.class.equals( declaredType ) && Long.TYPE.equals( javaType ) )
					|| ( Float.class.equals( declaredType ) && Float.TYPE.equals( javaType ) )
					|| ( Double.class.equals( declaredType ) && Double.TYPE.equals( javaType ) );
			}
		}

		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Plural attributes

	@Override
	public Set<PluralAttribute<? super J, ?, ?>> getPluralAttributes() {
		final Set<PluralAttribute<? super J, ?, ?>> attributes =
				declaredPluralAttributes == null
						? new HashSet<>()
						: new HashSet<>( declaredPluralAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getPluralAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<PluralAttribute<J, ?, ?>> getDeclaredPluralAttributes() {
		return declaredPluralAttributes == null
				? emptySet()
				: new HashSet<>( declaredPluralAttributes.values() );
	}

	@Override
	public @Nullable SqmPluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(String name) {
		var attribute = findDeclaredPluralAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else if ( getSuperType() != null ) {
			return getSuperType().findPluralAttribute( name );
		}
		else {
			return null;
		}
	}

	@Override
	public @Nullable SqmPluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name) {
		return declaredPluralAttributes == null ? null : declaredPluralAttributes.get( name );
	}

	private <E> void checkTypeForPluralAttributes(
			String attributeType,
			PluralAttribute<?,?,?> attribute,
			String name,
			Class<E> elementType,
			PluralAttribute.CollectionType collectionType) {
		if ( attribute == null
				|| elementType != null && !attribute.getBindableJavaType().equals( elementType )
				|| attribute.getCollectionType() != collectionType ) {
			throw new IllegalArgumentException(
					attributeType + " named " + name
							+ ( elementType != null ? " and of element type " + elementType : "" )
							+ " is not present"
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic attributes

	@Override
	public @Nullable SqmPersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name) {
		final var attribute = findDeclaredConcreteGenericAttribute( name );
		return attribute == null && getSuperType() != null
				? getSuperType().findDeclaredConcreteGenericAttribute( name )
				: attribute;
	}

	@Override
	public @Nullable SqmPersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name) {
		return declaredConcreteGenericAttributes == null ? null : declaredConcreteGenericAttributes.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bags

	@Override
	@SuppressWarnings("unchecked")
	public BagPersistentAttribute<? super J, ?> getCollection(String name) {
		var attribute = findPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findPluralAttribute( name );
		}
		basicCollectionCheck( attribute, name );
		return (BagPersistentAttribute<J, ?>) attribute;
	}

	private void basicCollectionCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "CollectionAttribute", attribute, name );
		if ( ! BagPersistentAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a CollectionAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings( "unchecked")
	public CollectionAttribute<J, ?> getDeclaredCollection(String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicCollectionCheck( attribute, name );
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> BagPersistentAttribute<? super J, E> getCollection(String name, Class<E> elementType) {
		final var attribute = findPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (BagPersistentAttribute<? super J, E>) attribute;
	}

	private <E> void checkCollectionElementType(PluralAttribute<?,?,?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "CollectionAttribute", attribute, name, elementType,
				PluralAttribute.CollectionType.COLLECTION );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttribute<J, E> getDeclaredCollection(String name, Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttribute<J, E>) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set attributes

	@Override
	@SuppressWarnings("unchecked")
	public SetPersistentAttribute<? super J, ?> getSet(String name) {
		final var attribute = findPluralAttribute( name );
		basicSetCheck( attribute, name );
		return (SetPersistentAttribute<? super J, ?>) attribute;
	}

	private void basicSetCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "SetAttribute", attribute, name );
		if ( ! SetPersistentAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a SetAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings( "unchecked")
	public SetPersistentAttribute<J, ?> getDeclaredSet(String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicSetCheck( attribute, name );
		return (SetPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttribute<? super J, E> getSet(String name, Class<E> elementType) {
		final var attribute = findPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return (SetAttribute<? super J, E>) attribute;
	}

	private <E> void checkSetElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "SetAttribute", attribute, name, elementType,
				PluralAttribute.CollectionType.SET );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttribute<J, E> getDeclaredSet(String name, Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return (SetAttribute<J, E>) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// List attributes

	@Override
	@SuppressWarnings("unchecked")
	public ListPersistentAttribute<? super J, ?> getList(String name) {
		final var attribute = findPluralAttribute( name );
		basicListCheck( attribute, name );
		return (ListPersistentAttribute<? super J, ?>) attribute;
	}

	private void basicListCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "ListAttribute", attribute, name );
		if ( ! ListPersistentAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a ListAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListPersistentAttribute<J, ?> getDeclaredList(String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicListCheck( attribute, name );
		return (ListPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttribute<? super J, E> getList(String name, Class<E> elementType) {
		final var attribute = findPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<? super J, E> ) attribute;
	}

	private <E> void checkListElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "ListAttribute", attribute, name, elementType, PluralAttribute.CollectionType.LIST );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttribute<J, E> getDeclaredList(String name, Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Map attributes

	@Override
	@SuppressWarnings("unchecked")
	public MapPersistentAttribute<? super J, ?, ?> getMap(String name) {
		final var attribute = findPluralAttribute( name );
		basicMapCheck( attribute, name );
		return (MapPersistentAttribute<? super J, ?, ?>) attribute;
	}

	private void basicMapCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "MapAttribute", attribute, name );
		if ( ! MapAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a MapAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public MapPersistentAttribute<J, ?, ?> getDeclaredMap(String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicMapCheck( attribute, name );
		return (MapPersistentAttribute<J, ?, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> MapAttribute<? super J, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		final var attribute = findPluralAttribute( name );
		checkMapValueType( attribute, name, valueType );
		final var mapAttribute = (MapAttribute<? super J, K, V>) attribute;
		checkMapKeyType( mapAttribute, name, keyType );
		return mapAttribute;
	}

	private <V> void checkMapValueType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<V> valueType) {
		checkTypeForPluralAttributes( "MapAttribute", attribute, name, valueType, PluralAttribute.CollectionType.MAP);
	}

	private <K,V> void checkMapKeyType(MapAttribute<? super J, K, V> mapAttribute, String name, Class<K> keyType) {
		if ( mapAttribute.getKeyJavaType() != keyType ) {
			throw new IllegalArgumentException( "MapAttribute named " + name + " does not support a key of type " + keyType );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> MapAttribute<J, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkMapValueType( attribute, name, valueType );
		final var mapAttribute = (MapAttribute<J, K, V>) attribute;
		checkMapKeyType( mapAttribute, name, keyType );
		return mapAttribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	@Serial
	protected Object writeReplace() throws ObjectStreamException {
		return new SerialForm( metamodel, getJavaType() );
	}

	private static class SerialForm implements Serializable {
		private final JpaMetamodel jpaMetamodel;
		private final Class<?> typeClass;

		public SerialForm(JpaMetamodel jpaMetamodel, Class<?> typeClass) {
			this.jpaMetamodel = jpaMetamodel;
			this.typeClass = typeClass;
		}

		@Serial
		private Object readResolve() {
			return jpaMetamodel.managedType( typeClass );
		}

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	private transient InFlightAccess<J> inFlightAccess;

	@Override
	public InFlightAccess<J> getInFlightAccess() {
		if ( inFlightAccess == null ) {
			throw new IllegalStateException( "Type has been locked" );
		}

		return inFlightAccess;
	}

	protected class InFlightAccessImpl implements InFlightAccess<J> {
		@Override
		public void addAttribute(PersistentAttribute<J,?> attribute) {
			if ( attribute instanceof SqmSingularPersistentAttribute<J, ?> singularAttribute ) {
				declaredSingularAttributes.put( attribute.getName(), singularAttribute );
			}
			else if ( attribute instanceof SqmPluralPersistentAttribute<J,?,?> pluralAttribute ) {
				if ( declaredPluralAttributes == null ) {
					declaredPluralAttributes = new HashMap<>();
				}
				declaredPluralAttributes.put( attribute.getName(), pluralAttribute );
			}
			else {
				throw new IllegalArgumentException(
						"Unable to classify attribute as singular or plural [" + attribute + "] for `" + this + '`'
				);
			}
		}

		@Override
		public void addConcreteGenericAttribute(PersistentAttribute<J, ?> attribute) {
			if ( declaredConcreteGenericAttributes == null ) {
				declaredConcreteGenericAttributes = new HashMap<>();
			}
			declaredConcreteGenericAttributes.put( attribute.getName(),
					(SqmPersistentAttribute<J, ?>) attribute );
		}

		@Override
		public void finishUp() {
			inFlightAccess = null;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + hibernateTypeName + "]";
	}
}
