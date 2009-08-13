/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.metamodel;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Bindable;

import org.hibernate.mapping.Property;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public abstract class ManagedTypeImpl<X> implements ManagedType<X>, Serializable {
	private final Class<X> javaClass;
	private final Map<String,Attribute<X, ?>> declaredAttributes;
	private final Map<String,SingularAttribute<X, ?>> declaredSingularAttributes;
	private final Map<String,PluralAttribute<X, ?, ?>> declaredCollections;

	

	ManagedTypeImpl(Class<X> clazz, Iterator<Property> properties, MetadataContext context) {
		this.javaClass = clazz;
		Map<String,Attribute<X, ?>> localDeclAttr = new HashMap<String,Attribute<X, ?>>();
		Map<String,SingularAttribute<X, ?>> localDeclSingAttr = new HashMap<String,SingularAttribute<X, ?>>();
		Map<String,PluralAttribute<X,?,?>> localDeclPlurAttr = new HashMap<String,PluralAttribute<X,?,?>>();

		while ( properties.hasNext() ) {
			Property property = properties.next();
			addProperty( property, context, localDeclAttr, localDeclSingAttr, localDeclPlurAttr );
		}
		declaredAttributes = Collections.unmodifiableMap( localDeclAttr );
		declaredSingularAttributes = Collections.unmodifiableMap( localDeclSingAttr );
		declaredCollections = Collections.unmodifiableMap( localDeclPlurAttr );
	}

	private <T> void addProperty(Property property,
								 MetadataContext context,
								 Map<String,Attribute<X, ?>> localDeclAttr,
								 Map<String,SingularAttribute<X, ?>> localDeclSingAttr,
								 Map<String,PluralAttribute<X,?,?>> localDeclPlurAttr) {
		final Attribute<X, ?> attribute = MetamodelFactory.getAttribute( this, property, context );
		localDeclAttr.put(attribute.getName(), attribute );
		final Bindable.BindableType bindableType = ( ( Bindable<T> ) attribute ).getBindableType();
		switch ( bindableType ) {
			case SINGULAR_ATTRIBUTE:
				localDeclSingAttr.put(attribute.getName(), (SingularAttribute<X, ?>) attribute );
				break;
			case PLURAL_ATTRIBUTE:
				localDeclPlurAttr.put(attribute.getName(), (PluralAttribute<X,?,?>) attribute );
				break;
			default:
				throw new AssertionFailure( "unknown bindable type: " + bindableType );
		}

	}

	public Set<Attribute<? super X, ?>> getAttributes() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<Attribute<X, ?>> getDeclaredAttributes() {
		return new HashSet<Attribute<X, ?>>(declaredAttributes.values());
	}

	public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
		final SingularAttribute<X, ?> attr = declaredSingularAttributes.get( name );
		checkTypeForSingleAttribute( "SingularAttribute ", attr, name, type );
		@SuppressWarnings( "unchecked")
		final SingularAttribute<X, Y> result = ( SingularAttribute<X, Y> ) attr;
		return result;
	}

	private <Y> void checkTypeForSingleAttribute(String error, SingularAttribute<?,?> attr, String name, Class<Y> type) {
		if (attr == null || ( type != null && !attr.getBindableJavaType().equals( type ) ) ) {
			throw new IllegalArgumentException(
					error + " named " + name
					+ (type != null ? " and of type " + type : "")
					+ " is not present");
		}
	}

	private <Y> void checkTypeForPluralAttributes(String error,
												  PluralAttribute<?,?,?> attr,
												  String name,
												  Class<Y> type,
												  PluralAttribute.CollectionType collectionType) {
		if (attr == null
				|| ( type != null && !attr.getBindableJavaType().equals( type ) )
				|| attr.getCollectionType() != collectionType ) {
			throw new IllegalArgumentException(
					error + " named " + name
					+ (type != null ? " and of element type " + type : "")
					+ " is not present");
		}
	}

	private <Y> void checkNotNull(String error, Attribute<?,?> attr, String name) {
		if (attr == null) {
			throw new IllegalArgumentException(
					error + " named " + name
					+ " is not present");
		}
	}

	public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
		return new HashSet<SingularAttribute<X, ?>>(declaredSingularAttributes.values());
	}

	public <E> CollectionAttribute<? super X, E> getCollection(String name, Class<E> elementType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <E> SetAttribute<? super X, E> getSet(String name, Class<E> elementType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <E> ListAttribute<? super X, E> getList(String name, Class<E> elementType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <E> CollectionAttribute<X, E> getDeclaredCollection(String name, Class<E> elementType) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		checkTypeForPluralAttributes( "CollectionAttribute ", attr, name, elementType, PluralAttribute.CollectionType.COLLECTION );
		@SuppressWarnings( "unchecked")
		final CollectionAttribute<X, E> result = ( CollectionAttribute<X, E> ) attr;
		return result;
	}

	public <E> SetAttribute<X, E> getDeclaredSet(String name, Class<E> elementType) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		checkTypeForPluralAttributes( "SetAttribute ", attr, name, elementType, PluralAttribute.CollectionType.SET );
		@SuppressWarnings( "unchecked")
		final SetAttribute<X, E> result = ( SetAttribute<X, E> ) attr;
		return result;
	}

	public <E> ListAttribute<X, E> getDeclaredList(String name, Class<E> elementType) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		checkTypeForPluralAttributes( "ListAttribute ", attr, name, elementType, PluralAttribute.CollectionType.LIST );
		@SuppressWarnings( "unchecked")
		final ListAttribute<X, E> result = ( ListAttribute<X, E> ) attr;
		return result;
	}

	public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		final String error = "MapAttribute ";
		checkTypeForPluralAttributes( error, attr, name, valueType, PluralAttribute.CollectionType.MAP );
		@SuppressWarnings( "unchecked")
		final MapAttribute<X, K, V> result = ( MapAttribute<X, K, V> ) attr;
		if ( result.getKeyJavaType() != keyType ) {
			throw new IllegalArgumentException(
					error + " named " + name + " does not support a key of type " + keyType
			);
		}
		return result;
	}

	public Set<PluralAttribute<? super X, ?, ?>> getCollections() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<PluralAttribute<X, ?, ?>> getDeclaredCollections() {
		return new HashSet<PluralAttribute<X,?,?>>(declaredCollections.values());
	}

	public Attribute<? super X, ?> getAttribute(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Attribute<X, ?> getDeclaredAttribute(String name) {
		final Attribute<X, ?> attr = declaredSingularAttributes.get( name );
		checkNotNull( "Attribute ", attr, name );
		return attr;
	}

	public SingularAttribute<? super X, ?> getSingularAttribute(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public SingularAttribute<X, ?> getDeclaredSingularAttribute(String name) {
		final SingularAttribute<X, ?> attr = declaredSingularAttributes.get( name );
		checkNotNull( "SingularAttribute ", attr, name );
		return attr;
	}

	public CollectionAttribute<? super X, ?> getCollection(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public SetAttribute<? super X, ?> getSet(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ListAttribute<? super X, ?> getList(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MapAttribute<? super X, ?, ?> getMap(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public CollectionAttribute<X, ?> getDeclaredCollection(String name) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		final String error = "CollectionAttribute ";
		checkNotNull( error, attr, name );
		if ( ! CollectionAttribute.class.isAssignableFrom( attr.getClass() ) ) {
			throw new IllegalArgumentException( name
					+ " is not a " + error + ": " + attr.getClass() );
		}
		@SuppressWarnings( "unchecked")
		final CollectionAttribute<X, ?> result = ( CollectionAttribute<X, ?> ) attr;
		return result;
	}

	public SetAttribute<X, ?> getDeclaredSet(String name) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		final String error = "SetAttribute ";
		checkNotNull( error, attr, name );
		if ( ! CollectionAttribute.class.isAssignableFrom( attr.getClass() ) ) {
			throw new IllegalArgumentException( name
					+ " is not a " + error + ": " + attr.getClass() );
		}
		@SuppressWarnings( "unchecked")
		final SetAttribute<X, ?> result = ( SetAttribute<X, ?> ) attr;
		return result;
	}

	public ListAttribute<X, ?> getDeclaredList(String name) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		final String error = "ListAttribute ";
		checkNotNull( error, attr, name );
		if ( ! CollectionAttribute.class.isAssignableFrom( attr.getClass() ) ) {
			throw new IllegalArgumentException( name
					+ " is not a " + error + ": " + attr.getClass() );
		}
		@SuppressWarnings( "unchecked")
		final ListAttribute<X, ?> result = ( ListAttribute<X, ?> ) attr;
		return result;
	}

	public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
		final PluralAttribute<X,?,?> attr = declaredCollections.get( name );
		final String error = "MapAttribute ";
		checkNotNull( error, attr, name );
		if ( ! MapAttribute.class.isAssignableFrom( attr.getClass() ) ) {
			throw new IllegalArgumentException( name
					+ " is not a " + error + ": " + attr.getClass() );
		}
		@SuppressWarnings( "unchecked")
		final MapAttribute<X,?,?> result = ( MapAttribute<X,?,?> ) attr;
		return result;
	}

	public abstract PersistenceType getPersistenceType();

	public Class<X> getJavaType() {
		return javaClass;
	}
}
