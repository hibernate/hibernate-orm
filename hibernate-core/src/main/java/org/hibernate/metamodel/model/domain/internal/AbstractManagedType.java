/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.AttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.DomainModelHelper;
import org.hibernate.metamodel.model.domain.spi.ListAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.MapAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.SetAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.SingularAttributeImplementor;

/**
 * Defines commonality for the JPA {@link ManagedType} hierarchy of interfaces.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<J>
		extends AbstractType<J>
		implements ManagedTypeImplementor<J>, Serializable {

	private final SessionFactoryImplementor sessionFactory;

	private final ManagedTypeImplementor<? super J> superType;

	private final Map<String,AttributeImplementor<J, ?>> declaredAttributes = new HashMap<>();
	private final Map<String, SingularAttributeImplementor<J, ?>> declaredSingularAttributes = new HashMap<>();
	private final Map<String, PluralAttributeImplementor<J, ?, ?>> declaredPluralAttributes = new HashMap<>();

	private transient InFlightAccess<J> inFlightAccess;

	protected AbstractManagedType(
			Class<J> javaType,
			String typeName,
			ManagedTypeImplementor<? super J> superType,
			SessionFactoryImplementor sessionFactory) {
		super( javaType, typeName );
		this.superType = superType;
		this.sessionFactory = sessionFactory;

		this.inFlightAccess = createInFlightAccess();
	}

	protected InFlightAccess<J> createInFlightAccess() {
		return new InFlightAccessImpl();
	}

	@Override
	public String getName() {
		return getTypeName();
	}

	@Override
	public ManagedTypeImplementor<? super J> getSuperType() {
		return superType;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Override
	public InFlightAccess<J> getInFlightAccess() {
		if ( inFlightAccess == null ) {
			throw new IllegalStateException( "Type has been locked" );
		}

		return inFlightAccess;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Set<Attribute<? super J, ?>> getAttributes() {
		HashSet attributes = new HashSet<Attribute<J, ?>>( declaredAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<Attribute<J, ?>> getDeclaredAttributes() {
		return new HashSet<>( declaredAttributes.values() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public AttributeImplementor<? super J, ?> getAttribute(String name) {
		AttributeImplementor<? super J, ?> attribute = declaredAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getAttribute( name );
		}
		checkNotNull( "Attribute ", attribute, name );
		return attribute;
	}

	@Override
	public AttributeImplementor<J, ?> getDeclaredAttribute(String name) {
		AttributeImplementor<J, ?> attr = declaredAttributes.get( name );
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
	@SuppressWarnings({ "unchecked" })
	public Set<SingularAttribute<? super J, ?>> getSingularAttributes() {
		HashSet attributes = new HashSet<SingularAttribute<J, ?>>( declaredSingularAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getSingularAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<SingularAttribute<J, ?>> getDeclaredSingularAttributes() {
		return new HashSet<SingularAttribute<J, ?>>( declaredSingularAttributes.values() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public SingularAttribute<? super J, ?> getSingularAttribute(String name) {
		SingularAttribute<? super J, ?> attribute = declaredSingularAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getSingularAttribute( name );
		}
		checkNotNull( "SingularAttribute ", attribute, name );
		return attribute;
	}

	@Override
	public SingularAttribute<J, ?> getDeclaredSingularAttribute(String name) {
		final SingularAttribute<J, ?> attr = declaredSingularAttributes.get( name );
		checkNotNull( "SingularAttribute ", attr, name );
		return attr;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> SingularAttributeImplementor<? super J, Y> getSingularAttribute(String name, Class<Y> type) {
		SingularAttribute<? super J, ?> attribute = declaredSingularAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getSingularAttribute( name );
		}
		checkTypeForSingleAttribute( "SingularAttribute ", attribute, name, type );
		return (SingularAttributeImplementor) attribute;
	}

	@Override
	@SuppressWarnings( "unchecked")
	public <Y> SingularAttributeImplementor<J, Y> getDeclaredSingularAttribute(String name, Class<Y> javaType) {
		final SingularAttribute<J, ?> attr = declaredSingularAttributes.get( name );
		checkTypeForSingleAttribute( "SingularAttribute ", attr, name, javaType );
		return (SingularAttributeImplementor) attr;
	}

	private <Y> void checkTypeForSingleAttribute(
			String attributeType,
			SingularAttribute<?,?> attribute,
			String name,
			Class<Y> javaType) {
		if ( attribute == null || ( javaType != null && !attribute.getBindableJavaType().equals( javaType ) ) ) {
			if ( isPrimitiveVariant( attribute, javaType ) ) {
				return;
			}
			throw new IllegalArgumentException(
					attributeType + " named " + name
					+ ( javaType != null ? " and of type " + javaType.getName() : "" )
					+ " is not present"
			);
		}
	}

	@SuppressWarnings({ "SimplifiableIfStatement" })
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

	@Override
	@SuppressWarnings({ "unchecked" })
	public Set<PluralAttribute<? super J, ?, ?>> getPluralAttributes() {
		HashSet attributes = new HashSet<PluralAttribute<? super J, ?, ?>>( declaredPluralAttributes.values() );
		if ( getSuperType() != null ) {
			attributes.addAll( getSuperType().getPluralAttributes() );
		}
		return attributes;
	}

	@Override
	public Set<PluralAttribute<J, ?, ?>> getDeclaredPluralAttributes() {
		return new HashSet<PluralAttribute<J,?,?>>( declaredPluralAttributes.values() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public CollectionAttribute<? super J, ?> getCollection(String name) {
		PluralAttributeImplementor attribute = getPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		basicCollectionCheck( attribute, name );
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeImplementor<? super J, ?, ?> getPluralAttribute(String name) {
		return declaredPluralAttributes.get( name );
	}

	private void basicCollectionCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "CollectionAttribute", attribute, name );
		if ( ! CollectionAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a CollectionAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings( "unchecked")
	public CollectionAttribute<J, ?> getDeclaredCollection(String name) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		basicCollectionCheck( attribute, name );
		return ( CollectionAttribute<J, ?> ) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public SetAttributeImplementor<? super J, ?> getSet(String name) {
		PluralAttribute<? super J, ?, ?> attribute = getPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		basicSetCheck( attribute, name );
		return (SetAttributeImplementor) attribute;
	}

	private void basicSetCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "SetAttribute", attribute, name );
		if ( ! SetAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a SetAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings( "unchecked")
	public SetAttributeImplementor<J, ?> getDeclaredSet(String name) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		basicSetCheck( attribute, name );
		return (SetAttributeImplementor) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ListAttributeImplementor<? super J, ?> getList(String name) {
		PluralAttribute<? super J, ?, ?> attribute = getPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		basicListCheck( attribute, name );
		return (ListAttributeImplementor) attribute;
	}

	private void basicListCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "ListAttribute", attribute, name );
		if ( ! ListAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a ListAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListAttributeImplementor<J, ?> getDeclaredList(String name) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		basicListCheck( attribute, name );
		return (ListAttributeImplementor) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public MapAttributeImplementor<? super J, ?, ?> getMap(String name) {
		PluralAttribute<? super J, ?, ?> attribute = getPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		basicMapCheck( attribute, name );
		return (MapAttributeImplementor) attribute;
	}

	private void basicMapCheck(PluralAttribute<? super J, ?, ?> attribute, String name) {
		checkNotNull( "MapAttribute", attribute, name );
		if ( ! MapAttribute.class.isAssignableFrom( attribute.getClass() ) ) {
			throw new IllegalArgumentException( name + " is not a MapAttribute: " + attribute.getClass() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public MapAttributeImplementor<J, ?, ?> getDeclaredMap(String name) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		basicMapCheck( attribute, name );
		return (MapAttributeImplementor) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <E> CollectionAttributeImplementor<? super J, E> getCollection(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = declaredPluralAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttributeImplementor) attribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttribute<J, E> getDeclaredCollection(String name, Class<E> elementType) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		checkCollectionElementType( attribute, name, elementType );
		return (CollectionAttribute) attribute;
	}

	private <E> void checkCollectionElementType(PluralAttribute<?,?,?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "CollectionAttribute", attribute, name, elementType, PluralAttribute.CollectionType.COLLECTION );
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

	@Override
	@SuppressWarnings({ "unchecked" })
	public <E> SetAttribute<? super J, E> getSet(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = declaredPluralAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		checkSetElementType( attribute, name, elementType );
		return ( SetAttribute<? super J, E> ) attribute;
	}

	private <E> void checkSetElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "SetAttribute", attribute, name, elementType, PluralAttribute.CollectionType.SET );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttribute<J, E> getDeclaredSet(String name, Class<E> elementType) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		checkSetElementType( attribute, name, elementType );
		return ( SetAttribute<J, E> ) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <E> ListAttribute<? super J, E> getList(String name, Class<E> elementType) {
		PluralAttribute<? super J, ?, ?> attribute = declaredPluralAttributes.get( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<? super J, E> ) attribute;
	}

	private <E> void checkListElementType(PluralAttribute<? super J, ?, ?> attribute, String name, Class<E> elementType) {
		checkTypeForPluralAttributes( "ListAttribute", attribute, name, elementType, PluralAttribute.CollectionType.LIST );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttribute<J, E> getDeclaredList(String name, Class<E> elementType) {
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		checkListElementType( attribute, name, elementType );
		return ( ListAttribute<J, E> ) attribute;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <K, V> MapAttribute<? super J, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		PluralAttribute<? super J, ?, ?> attribute = getPluralAttribute( name );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().getPluralAttribute( name );
		}
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
		final PluralAttribute<J,?,?> attribute = declaredPluralAttributes.get( name );
		checkMapValueType( attribute, name, valueType );
		final MapAttribute<J, K, V> mapAttribute = ( MapAttribute<J, K, V> ) attribute;
		checkMapKeyType( mapAttribute, name, keyType );
		return mapAttribute;
	}


	@Override
	public SubGraphImplementor<J> makeSubGraph() {
		return new SubGraphImpl<>( this, true, sessionFactory );
	}

	protected class InFlightAccessImpl implements InFlightAccess<J> {
		@Override
		@SuppressWarnings("unchecked")
		public void addAttribute(AttributeImplementor<J, ?> attribute) {
			// put it in the collective group of attributes
			declaredAttributes.put( attribute.getName(), attribute );

			// additionally classify as singular or plural attribute
			final Bindable.BindableType bindableType = ( ( Bindable ) attribute ).getBindableType();
			switch ( bindableType ) {
				case SINGULAR_ATTRIBUTE : {
					declaredSingularAttributes.put( attribute.getName(), (SingularAttributeImplementor<J,?>) attribute );
					break;
				}
				case PLURAL_ATTRIBUTE : {
					declaredPluralAttributes.put(attribute.getName(), (PluralAttributeImplementor<J,?,?>) attribute );
					break;
				}
				default : {
					throw new AssertionFailure( "unknown bindable type: " + bindableType );
				}
			}
		}

		@Override
		public void finishUp() {
			inFlightAccess = null;
		}
	}

	@Override
	public <S extends J> ManagedTypeImplementor<S> findSubType(String subTypeName) {
		return DomainModelHelper.resolveSubType( this, subTypeName, sessionFactory() );
	}

	@Override
	public <S extends J> ManagedTypeImplementor<S> findSubType(Class<S> subType) {
		return DomainModelHelper.resolveSubType( this, subType, sessionFactory() );
	}
}
