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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

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
import org.hibernate.query.sqm.tree.spi.domain.SqmManagedDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmPersistentAttribute;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralPersistentAttribute;
import org.hibernate.query.sqm.tree.spi.domain.SqmSingularPersistentAttribute;
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

	@Nonnull
	@Override
	public JpaMetamodelImplementor getMetamodel() {
		return metamodel;
	}

	@Override
	@Nonnull
	public Class<J> getJavaType() {
		return super.getJavaType();
	}

	@Override
	@Nullable
	public SqmManagedDomainType<? super J> getSuperType() {
		return supertype;
	}

	@Override
	@Nonnull
	public Collection<? extends SqmManagedDomainType<? extends J>> getSubTypes() {
		return subtypes;
	}

	@Override
	public void addSubType(@Nonnull ManagedDomainType<? extends J> subType){
		subtypes.add( (SqmManagedDomainType<? extends J>) subType );
	}

	@Override
	@Nonnull
	public RepresentationMode getRepresentationMode() {
		return representationMode;
	}

	@Override
	public void visitAttributes(@Nonnull Consumer<? super PersistentAttribute<? super J, ?>> action) {
		visitDeclaredAttributes( action );
		final var superType = getSuperType();
		if ( superType != null ) {
			superType.visitAttributes( action );
		}
	}

	@Override
	public void visitDeclaredAttributes(@Nonnull Consumer<? super PersistentAttribute<J, ?>> action) {
		declaredSingularAttributes.values().forEach( action );
		if ( declaredPluralAttributes != null ) {
			declaredPluralAttributes.values().forEach( action );
		}
	}

	@Override
	@Nonnull
	public Set<Attribute<? super J, ?>> getAttributes() {
		final Set<Attribute<? super J, ?>> attributes = new LinkedHashSet<>( getDeclaredAttributes() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getAttributes() );
		}
		return attributes;
	}

	@Override
	@Nonnull
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
	@Nonnull
	public SqmPersistentAttribute<? super J,?> getAttribute(@Nonnull String name) {
		final var attribute = findAttribute( name );
		checkNotNull( "Attribute", attribute, name );
		return attribute;
	}

	@Override
	@Nullable
	public SqmPersistentAttribute<? super J,?> findAttribute(@Nonnull String name) {
		final var attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else {
			return supertype != null ? supertype.findAttribute( name ) : null;
		}
	}

	@Override
	@Nullable
	public SqmPersistentAttribute<?, ?> findSubTypesAttribute(@Nonnull String name) {
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
	@Nullable
	public SqmPersistentAttribute<J,?> findDeclaredAttribute(@Nonnull String name) {
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
	@Nonnull
	public PersistentAttribute<J,?> getDeclaredAttribute(@Nonnull String name) {
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
	@Nonnull
	public Set<SingularAttribute<? super J, ?>> getSingularAttributes() {
		final Set<SingularAttribute<? super J, ?>> attributes =
				new HashSet<>( declaredSingularAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getSingularAttributes() );
		}
		return attributes;
	}

	@Override
	@Nonnull
	public Set<SingularAttribute<J, ?>> getDeclaredSingularAttributes() {
		return new HashSet<>( declaredSingularAttributes.values() );
	}

	@Override
	@Nonnull
	public SingularPersistentAttribute<? super J, ?> getSingularAttribute(@Nonnull String name) {
		final var attribute = findSingularAttribute( name );
		checkNotNull( "SingularAttribute", attribute, name );
		return attribute;
	}

	@Override
	@Nullable
	public SqmSingularPersistentAttribute<? super J, ?> findSingularAttribute(@Nonnull String name) {
		final var attribute = findDeclaredSingularAttribute( name );
		return attribute == null && getSuperType() != null
				? getSuperType().findSingularAttribute( name )
				: attribute;
	}

	@Override
	@Nonnull
	public <Y> SqmSingularPersistentAttribute<? super J, Y> getSingularAttribute(@Nonnull String name, @Nonnull Class<Y> type) {
		return checkTypeForSingleAttribute( findSingularAttribute( name ), name, type );
	}

	@Override
	@Nonnull
	public SingularAttribute<J, ?> getDeclaredSingularAttribute(@Nonnull String name) {
		final var attribute = findDeclaredSingularAttribute( name );
		checkNotNull( "SingularAttribute", attribute, name );
		return attribute;
	}

	@Override
	@Nullable
	public SqmSingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(@Nonnull String name) {
		return declaredSingularAttributes.get( name );
	}

	@Override
	@Nonnull
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredSingularAttribute(@Nonnull String name, @Nonnull Class<Y> javaType) {
		return checkTypeForSingleAttribute( findDeclaredSingularAttribute( name ), name, javaType );
	}

	private <K,Y> SqmSingularPersistentAttribute<K,Y> checkTypeForSingleAttribute(
			SqmSingularPersistentAttribute<K,?> attribute,
			String name,
			Class<Y> javaType) {
		if ( attribute == null || !hasMatchingReturnType( attribute, javaType ) ) {
			throw new IllegalArgumentException(
					"No singular attribute named '" + name
					+ ( javaType != null ? "' and of type '" + javaType.getName() : "" )
					+ "' in type '" + hibernateTypeName + "'"
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
	@Nonnull
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
	@Nonnull
	public Set<PluralAttribute<J, ?, ?>> getDeclaredPluralAttributes() {
		return declaredPluralAttributes == null
				? emptySet()
				: new HashSet<>( declaredPluralAttributes.values() );
	}

	@Override
	@Nullable
	public SqmPluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(@Nonnull String name) {
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
	@Nullable
	public SqmPluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(@Nonnull String name) {
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
					"No plural attribute named '" + name
					+ ( elementType != null ? "' and of element type '" + elementType.getName() : "" )
					+ "' in type '" + hibernateTypeName + "'"
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic attributes

	@Override
	@Nullable
	public SqmPersistentAttribute<? super J, ?> findConcreteGenericAttribute(@Nonnull String name) {
		final var attribute = findDeclaredConcreteGenericAttribute( name );
		return attribute == null && getSuperType() != null
				? getSuperType().findDeclaredConcreteGenericAttribute( name )
				: attribute;
	}

	@Override
	@Nullable
	public SqmPersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(@Nonnull String name) {
		return declaredConcreteGenericAttributes == null ? null : declaredConcreteGenericAttributes.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bags

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public BagPersistentAttribute<? super J, ?> getCollection(@Nonnull String name) {
		var attribute = findPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findPluralAttribute( name );
		}
		basicCollectionCheck( attribute, name );
		assert attribute != null;
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
	@Nonnull
	public CollectionAttribute<J, ?> getDeclaredCollection(@Nonnull String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicCollectionCheck( attribute, name );
		assert attribute != null;
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <E> BagPersistentAttribute<? super J, E> getCollection(@Nonnull String name, @Nonnull Class<E> elementType) {
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
	@Nonnull
	public <E> CollectionAttribute<J, E> getDeclaredCollection(@Nonnull String name, @Nonnull Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttribute<J, E>) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set attributes

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public SetPersistentAttribute<? super J, ?> getSet(@Nonnull String name) {
		final var attribute = findPluralAttribute( name );
		basicSetCheck( attribute, name );
		assert attribute != null;
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
	@Nonnull
	public SetPersistentAttribute<J, ?> getDeclaredSet(@Nonnull String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicSetCheck( attribute, name );
		assert attribute != null;
		return (SetPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <E> SetAttribute<? super J, E> getSet(@Nonnull String name, @Nonnull Class<E> elementType) {
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
	@Nonnull
	public <E> SetAttribute<J, E> getDeclaredSet(@Nonnull String name, @Nonnull Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return (SetAttribute<J, E>) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// List attributes

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public ListPersistentAttribute<? super J, ?> getList(@Nonnull String name) {
		final var attribute = findPluralAttribute( name );
		basicListCheck( attribute, name );
		assert attribute != null;
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
	@Nonnull
	public ListPersistentAttribute<J, ?> getDeclaredList(@Nonnull String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicListCheck( attribute, name );
		assert attribute != null;
		return (ListPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <E> ListAttribute<? super J, E> getList(@Nonnull String name, @Nonnull Class<E> elementType) {
		final var attribute = findPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<? super J, E> ) attribute;
	}

	private <E> void checkListElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "ListAttribute", attribute, name, elementType, PluralAttribute.CollectionType.LIST );
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <E> ListAttribute<J, E> getDeclaredList(@Nonnull String name, @Nonnull Class<E> elementType) {
		final var attribute = findDeclaredPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Map attributes

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public MapPersistentAttribute<? super J, ?, ?> getMap(@Nonnull String name) {
		final var attribute = findPluralAttribute( name );
		basicMapCheck( attribute, name );
		assert attribute != null;
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
	@Nonnull
	public MapPersistentAttribute<J, ?, ?> getDeclaredMap(@Nonnull String name) {
		final var attribute = findDeclaredPluralAttribute( name );
		basicMapCheck( attribute, name );
		assert attribute != null;
		return (MapPersistentAttribute<J, ?, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nonnull
	public <K, V> MapAttribute<? super J, K, V> getMap(
			@Nonnull String name,
			@Nonnull Class<K> keyType,
			@Nonnull Class<V> valueType) {
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
	@Nonnull
	public <K, V> MapAttribute<J, K, V> getDeclaredMap(
			@Nonnull String name,
			@Nonnull Class<K> keyType,
			@Nonnull Class<V> valueType) {
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
