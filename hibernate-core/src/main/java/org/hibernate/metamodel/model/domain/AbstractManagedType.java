/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.io.ObjectStreamException;
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

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.internal.AttributeContainer;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Functionality common to all implementations of {@link ManagedType}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<J>
		extends AbstractDomainType<J>
		implements ManagedDomainType<J>, AttributeContainer<J>, Serializable {
	private final String hibernateTypeName;
	private final ManagedDomainType<? super J> superType;
	private final RepresentationMode representationMode;
	private final JpaMetamodelImplementor metamodel;

	private final Map<String, SingularPersistentAttribute<J, ?>> declaredSingularAttributes = new LinkedHashMap<>();
	private volatile Map<String, PluralPersistentAttribute<J, ?, ?>> declaredPluralAttributes ;
	private volatile Map<String, PersistentAttribute<J, ?>> declaredConcreteGenericAttributes;

	private final List<ManagedDomainType<? extends J>> subTypes = new ArrayList<>();

	protected AbstractManagedType(
			String hibernateTypeName,
			JavaType<J> javaType,
			ManagedDomainType<? super J> superType,
			JpaMetamodelImplementor metamodel) {
		super( javaType );
		this.hibernateTypeName = hibernateTypeName;
		this.superType = superType;
		this.metamodel = metamodel;
		if ( superType != null ) {
			superType.addSubType( this );
		}

		// todo (6.0) : need to handle RepresentationMode#MAP as well
		this.representationMode = RepresentationMode.POJO;

		this.inFlightAccess = createInFlightAccess();
	}

	protected InFlightAccess<J> createInFlightAccess() {
		return new InFlightAccessImpl();
	}

	@Override
	public ManagedDomainType<? super J> getSuperType() {
		return superType;
	}

	@Override
	public Collection<? extends ManagedDomainType<? extends J>> getSubTypes() {
		return subTypes;
	}

	@Override
	public void addSubType(ManagedDomainType<? extends J> subType){
		subTypes.add( subType );
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
		final HashSet<Attribute<? super J, ?>> attributes = new LinkedHashSet<>( getDeclaredAttributes() );
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
			final HashSet<Attribute<J, ?>> attributes =
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
	public PersistentAttribute<? super J,?> getAttribute(String name) {
		final PersistentAttribute<? super J,?> attribute = findAttribute( name );
		checkNotNull( "Attribute", attribute, name );
		return attribute;
	}

	@Override
	public PersistentAttribute<? super J,?> findAttribute(String name) {
		// first look at declared attributes
		final PersistentAttribute<J,?> attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( getSuperType() != null ) {
			return getSuperType().findAttributeInSuperTypes( name );
		}

		return null;
	}

	@Override
	public PersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name) {
		final PersistentAttribute<J, ?> local = findDeclaredAttribute( name );
		if ( local != null ) {
			return local;
		}

		if ( superType != null ) {
			return superType.findAttributeInSuperTypes( name );
		}

		return null;
	}

	@Override
	public PersistentAttribute<?, ?> findSubTypesAttribute(String name) {
		// first look at declared attributes
		final PersistentAttribute<J,?> attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		for ( ManagedDomainType<? extends J> subType : subTypes ) {
			final PersistentAttribute<?,?> subTypeAttribute = subType.findSubTypesAttribute( name );
			if ( subTypeAttribute != null ) {
				return subTypeAttribute;
			}
		}

		return null;
	}

	@Override
	public PersistentAttribute<J,?> findDeclaredAttribute(String name) {
		// try singular attribute
		final PersistentAttribute<J,?> attribute = declaredSingularAttributes.get( name );
		if ( attribute != null ) {
			return attribute;
		}

		// next plural
		if ( declaredPluralAttributes != null ) {
			return declaredPluralAttributes.get( name );
		}

		return null;
	}

	@Override
	public PersistentAttribute<J,?> getDeclaredAttribute(String name) {
		PersistentAttribute<J,?> attr = findDeclaredAttribute( name );
		checkNotNull( "Attribute", attr, name );
		return attr;
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
		HashSet<SingularAttribute<? super J, ?>> attributes = new HashSet<>( declaredSingularAttributes.values() );
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
		SingularPersistentAttribute<? super J, ?> attribute = findSingularAttribute( name );
		checkNotNull( "SingularAttribute", attribute, name );
		return attribute;
	}

	@Override
	public SingularPersistentAttribute<? super J, ?> findSingularAttribute(String name) {
		SingularPersistentAttribute<? super J, ?> attribute = findDeclaredSingularAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findSingularAttribute( name );
		}
		return attribute;
	}

	@Override
	public <Y> SingularPersistentAttribute<? super J, Y> getSingularAttribute(String name, Class<Y> type) {
		return checkTypeForSingleAttribute( findSingularAttribute( name ), name, type );
	}

	@Override
	public SingularAttribute<J, ?> getDeclaredSingularAttribute(String name) {
		final SingularAttribute<J, ?> attr = findDeclaredSingularAttribute( name );
		checkNotNull( "SingularAttribute", attr, name );
		return attr;
	}

	@Override
	public SingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name) {
		return declaredSingularAttributes.get( name );
	}

	@Override
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredSingularAttribute(String name, Class<Y> javaType) {
		return checkTypeForSingleAttribute( findDeclaredSingularAttribute( name ), name, javaType );
	}

	private <K,Y> SingularPersistentAttribute<K,Y> checkTypeForSingleAttribute(
			SingularPersistentAttribute<K,?> attribute,
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
			SingularPersistentAttribute<K, Y> narrowed = (SingularPersistentAttribute<K, Y>) attribute;
			return narrowed;
		}
	}

	private <T, Y> boolean hasMatchingReturnType(SingularAttribute<T, ?> attribute, Class<Y> javaType) {
		return javaType == null
			|| attribute.getBindableJavaType().equals( javaType )
			|| isPrimitiveVariant( attribute, javaType );
	}

	protected <Y> boolean isPrimitiveVariant(SingularAttribute<?,?> attribute, Class<Y> javaType) {
		if ( attribute == null ) {
			return false;
		}
		Class<?> declaredType = attribute.getBindableJavaType();

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

		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Plural attributes

	@Override
	public Set<PluralAttribute<? super J, ?, ?>> getPluralAttributes() {
		Set<PluralAttribute<? super J, ?, ?>> attributes = declaredPluralAttributes == null
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
	public PluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(String name) {
		PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
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
	public PluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name) {
		return declaredPluralAttributes == null ? null : declaredPluralAttributes.get( name );
	}

	private <E> void checkTypeForPluralAttributes(
			String attributeType,
			PluralAttribute<?,?,?> attribute,
			String name,
			Class<E> elementType,
			PluralAttribute.CollectionType collectionType) {
		if ( attribute == null
				|| ( elementType != null && !attribute.getBindableJavaType().equals( elementType ) )
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
	public PersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name) {
		PersistentAttribute<? super J, ?> attribute = findDeclaredConcreteGenericAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findDeclaredConcreteGenericAttribute( name );
		}
		return attribute;
	}

	@Override
	public PersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name) {
		return declaredConcreteGenericAttributes == null ? null : declaredConcreteGenericAttributes.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bags

	@Override
	@SuppressWarnings("unchecked")
	public BagPersistentAttribute<? super J, ?> getCollection(String name) {
		PluralPersistentAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );

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
		final PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		basicCollectionCheck( attribute, name );
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> BagPersistentAttribute<? super J, E> getCollection(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (BagPersistentAttribute<? super J, E>) attribute;
	}

	private <E> void checkCollectionElementType(PluralAttribute<?,?,?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "CollectionAttribute", attribute, name, elementType, PluralAttribute.CollectionType.COLLECTION );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttribute<J, E> getDeclaredCollection(String name, Class<E> elementType) {
		final PluralAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttribute<J, E>) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set attributes

	@Override
	@SuppressWarnings("unchecked")
	public SetPersistentAttribute<? super J, ?> getSet(String name) {
		final PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
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
		final PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		basicSetCheck( attribute, name );
		return (SetPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttribute<? super J, E> getSet(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return ( SetAttribute<? super J, E> ) attribute;
	}

	private <E> void checkSetElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "SetAttribute", attribute, name, elementType, PluralAttribute.CollectionType.SET );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttribute<J, E> getDeclaredSet(String name, Class<E> elementType) {
		final PluralAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return ( SetAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// List attributes

	@Override
	@SuppressWarnings("unchecked")
	public ListPersistentAttribute<? super J, ?> getList(String name) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
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
		final PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		basicListCheck( attribute, name );
		return (ListPersistentAttribute<J, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttribute<? super J, E> getList(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<? super J, E> ) attribute;
	}

	private <E> void checkListElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "ListAttribute", attribute, name, elementType, PluralAttribute.CollectionType.LIST );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttribute<J, E> getDeclaredList(String name, Class<E> elementType) {
		final PluralAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Map attributes

	@Override
	@SuppressWarnings("unchecked")
	public MapPersistentAttribute<? super J, ?, ?> getMap(String name) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
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
		final PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		basicMapCheck( attribute, name );
		return (MapPersistentAttribute<J, ?, ?>) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> MapAttribute<? super J, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		checkMapValueType( attribute, name, valueType );
		final MapAttribute<? super J, K, V> mapAttribute = ( MapAttribute<? super J, K, V> ) attribute;
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
		final PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		checkMapValueType( attribute, name, valueType );
		final MapAttribute<J, K, V> mapAttribute = ( MapAttribute<J, K, V> ) attribute;
		checkMapKeyType( mapAttribute, name, keyType );
		return mapAttribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

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
			if ( attribute instanceof SingularPersistentAttribute ) {
				declaredSingularAttributes.put( attribute.getName(), (SingularPersistentAttribute<J, ?>) attribute );
			}
			else if ( attribute instanceof PluralPersistentAttribute ) {
				if ( AbstractManagedType.this.declaredPluralAttributes == null ) {
					AbstractManagedType.this.declaredPluralAttributes = new HashMap<>();
				}
				AbstractManagedType.this.declaredPluralAttributes.put( attribute.getName(), (PluralPersistentAttribute<J,?,?>) attribute );
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
			declaredConcreteGenericAttributes.put( attribute.getName(), attribute );
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
