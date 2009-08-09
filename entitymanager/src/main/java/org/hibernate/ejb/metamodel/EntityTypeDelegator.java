package org.hibernate.ejb.metamodel;

import java.util.Set;
import java.io.Serializable;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

/**
 * Delegate to an other EntityType<X>
 * Helps break infinite loops when creating entity metamodel related to each other
 * 
 * @author Emmanuel Bernard
 */
public class EntityTypeDelegator<X> implements EntityType<X>, Serializable {
	private volatile EntityType<X> delegate;

	void setDelegate(EntityType<X> delegate) {
		this.delegate = delegate;
	}

	public String getName() {
		return delegate.getName();
	}

	public <Y> SingularAttribute<? super X, Y> getId(Class<Y> type) {
		return delegate.getId( type );
	}

	public <Y> SingularAttribute<? super X, Y> getVersion(Class<Y> type) {
		return delegate.getVersion( type );
	}

	public <Y> SingularAttribute<X, Y> getDeclaredId(Class<Y> type) {
		return delegate.getDeclaredId( type );
	}

	public <Y> SingularAttribute<X, Y> getDeclaredVersion(Class<Y> type) {
		return delegate.getDeclaredVersion( type );
	}

	public IdentifiableType<? super X> getSupertype() {
		return delegate.getSupertype();
	}

	public boolean hasSingleIdAttribute() {
		return delegate.hasSingleIdAttribute();
	}

	public boolean hasVersionAttribute() {
		return delegate.hasVersionAttribute();
	}

	public Set<SingularAttribute<? super X, ?>> getIdClassAttributes() {
		return delegate.getIdClassAttributes();
	}

	public Type<?> getIdType() {
		return delegate.getIdType();
	}

	public Set<Attribute<? super X, ?>> getAttributes() {
		return delegate.getAttributes();
	}

	public Set<Attribute<X, ?>> getDeclaredAttributes() {
		return delegate.getDeclaredAttributes();
	}

	public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
		return delegate.getSingularAttribute( name, type );
	}

	public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
		return delegate.getDeclaredSingularAttribute( name, type );
	}

	public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
		return delegate.getSingularAttributes();
	}

	public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
		return delegate.getDeclaredSingularAttributes();
	}

	public <E> CollectionAttribute<? super X, E> getCollection(String name, Class<E> elementType) {
		return delegate.getCollection( name, elementType );
	}

	public <E> SetAttribute<? super X, E> getSet(String name, Class<E> elementType) {
		return delegate.getSet( name, elementType );
	}

	public <E> ListAttribute<? super X, E> getList(String name, Class<E> elementType) {
		return delegate.getList( name, elementType );
	}

	public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		return delegate.getMap( name, keyType, valueType );
	}

	public <E> CollectionAttribute<X, E> getDeclaredCollection(String name, Class<E> elementType) {
		return delegate.getDeclaredCollection( name, elementType );
	}

	public <E> SetAttribute<X, E> getDeclaredSet(String name, Class<E> elementType) {
		return delegate.getDeclaredSet( name, elementType );
	}

	public <E> ListAttribute<X, E> getDeclaredList(String name, Class<E> elementType) {
		return delegate.getDeclaredList( name, elementType );
	}

	public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
		return delegate.getDeclaredMap( name, keyType, valueType );
	}

	public Set<PluralAttribute<? super X, ?, ?>> getCollections() {
		return delegate.getCollections();
	}

	public Set<PluralAttribute<X, ?, ?>> getDeclaredCollections() {
		return delegate.getDeclaredCollections();
	}

	public Attribute<? super X, ?> getAttribute(String name) {
		return delegate.getAttribute( name );
	}

	public Attribute<X, ?> getDeclaredAttribute(String name) {
		return delegate.getDeclaredAttribute( name );
	}

	public SingularAttribute<? super X, ?> getSingularAttribute(String name) {
		return delegate.getSingularAttribute( name );
	}

	public SingularAttribute<X, ?> getDeclaredSingularAttribute(String name) {
		return delegate.getDeclaredSingularAttribute( name );
	}

	public CollectionAttribute<? super X, ?> getCollection(String name) {
		return delegate.getCollection( name );
	}

	public SetAttribute<? super X, ?> getSet(String name) {
		return delegate.getSet( name );
	}

	public ListAttribute<? super X, ?> getList(String name) {
		return delegate.getList( name );
	}

	public MapAttribute<? super X, ?, ?> getMap(String name) {
		return delegate.getMap( name );
	}

	public CollectionAttribute<X, ?> getDeclaredCollection(String name) {
		return delegate.getDeclaredCollection( name );
	}

	public SetAttribute<X, ?> getDeclaredSet(String name) {
		return delegate.getDeclaredSet( name );
	}

	public ListAttribute<X, ?> getDeclaredList(String name) {
		return delegate.getDeclaredList( name );
	}

	public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
		return delegate.getDeclaredMap( name );
	}

	public PersistenceType getPersistenceType() {
		return delegate.getPersistenceType();
	}

	public Class<X> getJavaType() {
		return delegate.getJavaType();
	}

	public BindableType getBindableType() {
		return delegate.getBindableType();
	}

	public Class<X> getBindableJavaType() {
		return delegate.getBindableJavaType();
	}
}
