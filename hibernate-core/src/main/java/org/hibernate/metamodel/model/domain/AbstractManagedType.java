/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.internal.AttributeContainer;
import org.hibernate.metamodel.model.domain.internal.DomainModelHelper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Commonality for Hibernate's implementations of the JPA {@link ManagedType}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<J>
		extends AbstractDomainType<J>
		implements ManagedDomainType<J>, AttributeContainer<J>, Serializable {
	private final String hibernateTypeName;
	private final ManagedDomainType<? super J> superType;
	private final RepresentationMode representationMode;

	private final Map<String, SingularPersistentAttribute<J, ?>> declaredSingularAttributes = new HashMap<>();
	private final Map<String, PluralPersistentAttribute<J, ?, ?>> declaredPluralAttributes = new HashMap<>();

	protected AbstractManagedType(
			String hibernateTypeName,
			JavaTypeDescriptor<J> javaTypeDescriptor,
			ManagedDomainType<? super J> superType,
			JpaMetamodel jpaMetamodel) {
		super( javaTypeDescriptor, jpaMetamodel );
		this.hibernateTypeName = hibernateTypeName;
		this.superType = superType;

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
	public RepresentationMode getRepresentationMode() {
		return representationMode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitAttributes(Consumer<PersistentAttribute<? super J, ?>> action) {
		visitDeclaredAttributes( (Consumer) action );
		if ( getSuperType() != null ) {
			getSuperType().visitAttributes( (Consumer) action );
		}
	}

	@Override
	public void visitDeclaredAttributes(Consumer<PersistentAttribute<J, ?>> action) {
		declaredSingularAttributes.values().forEach( action );
		declaredPluralAttributes.values().forEach( action );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Attribute<? super J, ?>> getAttributes() {
		final HashSet attributes = new HashSet<>( getDeclaredAttributes() );

		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getAttributes() );
		}

		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Attribute<J, ?>> getDeclaredAttributes() {
		if ( declaredSingularAttributes.isEmpty() && declaredPluralAttributes.isEmpty() ) {
			return Collections.emptySet();
		}

		final HashSet attributes = new HashSet<>( declaredSingularAttributes.values() );
		attributes.addAll( declaredPluralAttributes.values() );
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute<? super J,?> getAttribute(String name) {
		final PersistentAttribute attribute = findAttribute( name );
		checkNotNull( "Attribute ", attribute, name );
		return attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute<? super J,?> findAttribute(String name) {
		// first look at declared attributes
		PersistentAttribute attribute = findDeclaredAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( getSuperType() != null ) {
			attribute = getSuperType().findAttribute( name );
			//noinspection RedundantIfStatement
			if ( attribute != null ) {
				return attribute;
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute<J,?> findDeclaredAttribute(String name) {
		// try singular attribute
		PersistentAttribute attribute = declaredSingularAttributes.get( name );
		if ( attribute != null ) {
			return attribute;
		}

		// next plural
		attribute = declaredPluralAttributes.get( name );
		//noinspection RedundantIfStatement
		if ( attribute != null ) {
			return attribute;
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute<J,?> getDeclaredAttribute(String name) {
		PersistentAttribute attr = findDeclaredAttribute( name );
		checkNotNull( "Attribute ", attr, name );
		return attr;
	}

	private void checkNotNull(String attributeType, Attribute<?,?> attribute, String name) {
		if ( attribute == null ) {
			throw new IllegalArgumentException(
					String.format(
							"Unable to locate %s with the the given name [%s] on this ManagedType [%s]",
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
	@SuppressWarnings({ "unchecked" })
	public Set<SingularAttribute<? super J, ?>> getSingularAttributes() {
		HashSet attributes = new HashSet<>( declaredSingularAttributes.values() );
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
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute<? super J, ?> getSingularAttribute(String name) {
		SingularPersistentAttribute attribute = findSingularAttribute( name );
		checkNotNull( "SingularAttribute ", attribute, name );
		return attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute<? super J, ?> findSingularAttribute(String name) {
		SingularPersistentAttribute attribute = findDeclaredSingularAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findSingularAttribute( name );
		}
		return attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularPersistentAttribute<? super J, Y> getSingularAttribute(String name, Class<Y> type) {
		SingularAttribute attribute = findSingularAttribute( name );
		checkTypeForSingleAttribute( attribute, name, type );
		return (SingularPersistentAttribute) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularAttribute<J, ?> getDeclaredSingularAttribute(String name) {
		final SingularAttribute attr = findDeclaredSingularAttribute( name );
		checkNotNull( "SingularAttribute ", attr, name );
		return attr;
	}

	@Override
	public SingularPersistentAttribute<? super J, ?> findDeclaredSingularAttribute(String name) {
		return declaredSingularAttributes.get( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularPersistentAttribute<J, Y> getDeclaredSingularAttribute(String name, Class<Y> javaType) {
		final SingularAttribute attr = findDeclaredSingularAttribute( name );
		checkTypeForSingleAttribute( attr, name, javaType );
		return (SingularPersistentAttribute) attr;
	}

	private <Y> void checkTypeForSingleAttribute(
			SingularAttribute<?,?> attribute,
			String name,
			Class<Y> javaType) {
		if ( attribute == null || ( javaType != null && !attribute.getBindableJavaType().equals( javaType ) ) ) {
			if ( isPrimitiveVariant( attribute, javaType ) ) {
				return;
			}
			throw new IllegalArgumentException(
					"SingularAttribute named " + name
							+ ( javaType != null ? " and of type " + javaType.getName() : "" )
							+ " is not present"
			);
		}
	}

	@SuppressWarnings({"SimplifiableIfStatement", "WeakerAccess"})
	protected <Y> boolean isPrimitiveVariant(SingularAttribute<?,?> attribute, Class<Y> javaType) {
		if ( attribute == null ) {
			return false;
		}
		Class declaredType = attribute.getBindableJavaType();

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
	@SuppressWarnings("unchecked")
	public Set<PluralAttribute<? super J, ?, ?>> getPluralAttributes() {
		final Set attributes = getDeclaredPluralAttributes();

		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getPluralAttributes() );
		}

		return attributes;
	}

	@Override
	public Set<PluralAttribute<J, ?, ?>> getDeclaredPluralAttributes() {
		return new HashSet<>( declaredPluralAttributes.values() );
	}

	@Override
	@SuppressWarnings({"unchecked", "RedundantIfStatement"})
	public PluralPersistentAttribute findPluralAttribute(String name) {
		PluralPersistentAttribute<? super J, ?, ?> attribute = findDeclaredPluralAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( getSuperType() != null ) {
			attribute = getSuperType().findDeclaredPluralAttribute( name );
			if ( attribute != null ) {
				return attribute;
			}
		}

		return null;
	}

	@Override
	public PluralPersistentAttribute<? super J, ?, ?> findDeclaredPluralAttribute(String name) {
		return declaredPluralAttributes.get( name );
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
	// Bags

	@Override
	@SuppressWarnings("unchecked")
	public BagPersistentAttribute<? super J, ?> getCollection(String name) {
		PluralPersistentAttribute attribute = findPluralAttribute( name );

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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		basicCollectionCheck( attribute, name );
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <E> BagPersistentAttribute<? super J, E> getCollection(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (BagPersistentAttribute) attribute;
	}

	private <E> void checkCollectionElementType(PluralAttribute<?,?,?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "CollectionAttribute", attribute, name, elementType, PluralAttribute.CollectionType.COLLECTION );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttribute<J, E> getDeclaredCollection(String name, Class<E> elementType) {
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttribute) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set attributes

	@Override
	@SuppressWarnings({ "unchecked" })
	public SetPersistentAttribute<? super J, ?> getSet(String name) {
		final PluralAttribute attribute = findPluralAttribute( name );
		basicSetCheck( attribute, name );
		return (SetPersistentAttribute) attribute;
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		basicSetCheck( attribute, name );
		return (SetPersistentAttribute) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		checkSetElementType( attribute, name, elementType );
		return ( SetAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// List attributes

	@Override
	@SuppressWarnings({ "unchecked" })
	public ListPersistentAttribute<? super J, ?> getList(String name) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		basicListCheck( attribute, name );
		return (ListPersistentAttribute) attribute;
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		basicListCheck( attribute, name );
		return (ListPersistentAttribute) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<J, E> ) attribute;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Map attributes

	@Override
	@SuppressWarnings({ "unchecked" })
	public MapPersistentAttribute<? super J, ?, ?> getMap(String name) {
		PluralAttribute<? super J, ?, ?> attribute = findPluralAttribute( name );
		basicMapCheck( attribute, name );
		return (MapPersistentAttribute) attribute;
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		basicMapCheck( attribute, name );
		return (MapPersistentAttribute) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
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
		final PluralAttribute attribute = findDeclaredPluralAttribute( name );
		checkMapValueType( attribute, name, valueType );
		final MapAttribute<J, K, V> mapAttribute = ( MapAttribute<J, K, V> ) attribute;
		checkMapKeyType( mapAttribute, name, keyType );
		return mapAttribute;
	}


	@SuppressWarnings("unchecked")
	@Override
	public SubGraphImplementor<J> makeSubGraph() {
		return new SubGraphImpl( this, true, jpaMetamodel() );
	}

	@Override
	public <S extends J> ManagedDomainType<S> findSubType(String subTypeName) {
		return DomainModelHelper.resolveSubType( this, subTypeName, jpaMetamodel() );
	}

	@Override
	public <S extends J> ManagedDomainType<S> findSubType(Class<S> subType) {
		return DomainModelHelper.resolveSubType( this, subType, jpaMetamodel() );
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
		@SuppressWarnings("unchecked")
		public void addAttribute(PersistentAttribute<J,?> attribute) {
			if ( attribute instanceof SingularPersistentAttribute ) {
				declaredSingularAttributes.put( attribute.getName(), (SingularPersistentAttribute<J,?>) attribute );
			}
			else if ( attribute instanceof PluralPersistentAttribute ) {
				declaredPluralAttributes.put(attribute.getName(), (PluralPersistentAttribute<J,?,?>) attribute );
			}

			throw new IllegalArgumentException(
					"Unable to classify attribute as singular or plural [" + attribute + "] for `" + this + '`'
			);
		}

		@Override
		public void finishUp() {
			inFlightAccess = null;
		}
	}
}
