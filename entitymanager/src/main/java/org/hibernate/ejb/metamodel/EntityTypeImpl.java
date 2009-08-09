package org.hibernate.ejb.metamodel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.Serializable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 */
public class EntityTypeImpl<X> extends ManagedTypeImpl<X> implements EntityType<X>, Serializable {

	private final SingularAttribute<X, ?> id;
	private final SingularAttribute<X, ?> version;
	private final String className;
	private final boolean hasIdentifierProperty;
	private final boolean isVersioned;
	private final Set<SingularAttribute<? super X,?>> idClassAttributes;
	private final IdentifiableType<? super X> supertype;

	public EntityTypeImpl(Class<X> clazz, PersistentClass persistentClass, MetadataContext context) {
		super(clazz, (Iterator<Property>) persistentClass.getPropertyIterator(), context );
		this.className = persistentClass.getClassName();
		this.hasIdentifierProperty = persistentClass.hasIdentifierProperty();
		this.isVersioned = persistentClass.isVersioned();
		id = buildIdAttribute( persistentClass );
		version = buildVersionAttribute( persistentClass );
		final Set<SingularAttribute<? super X, ?>> attributes = buildIdClassAttributes( persistentClass, context );
		this.idClassAttributes = attributes != null ? Collections.unmodifiableSet( attributes ) : null;

		PersistentClass superPersistentClass = persistentClass.getSuperclass();
		if ( superPersistentClass == null ) {
			supertype = null;
		}
		else {
			final Class<?> superclass = superPersistentClass.getMappedClass();
			final EntityTypeDelegator<X> entityTypeDelegator = new EntityTypeDelegator<X>();
			context.addDelegator( entityTypeDelegator, superclass );
			supertype = entityTypeDelegator;
		}
	}

	private <A> SingularAttribute<X, A> buildIdAttribute(PersistentClass persistentClass) {
		final Property identifierProperty = persistentClass.getIdentifierProperty();
		@SuppressWarnings( "unchecked" )
		Class<A> idClass = identifierProperty.getType().getReturnedClass();
		final Type<A> attrType = new BasicTypeImpl<A>( idClass,
														identifierProperty.isComposite() ?
																PersistenceType.EMBEDDABLE :
																PersistenceType.BASIC);
		return SingularAttributeImpl.create(this, attrType )
										.property(identifierProperty)
										//.member( null ) //TODO member
										.id()
										.build();
	}

	private <A> SingularAttribute<X, A> buildVersionAttribute(PersistentClass persistentClass) {
		if ( persistentClass.isVersioned() ) {
			@SuppressWarnings( "unchecked" )
			Class<A> versionClass = persistentClass.getVersion().getType().getReturnedClass();
			Property property = persistentClass.getVersion();
			final Type<A> attrType = new BasicTypeImpl<A>( versionClass, PersistenceType.BASIC);
			return SingularAttributeImpl.create(this, attrType )
										.property(property)
										//.member( null ) //TODO member
										.version()
										.build();
		}
		else {
			return null;
		}
	}

	private Set<SingularAttribute<? super X, ?>> buildIdClassAttributes(PersistentClass persistentClass, MetadataContext context) {
		if ( hasSingleIdAttribute() ) {
			return null;
		}
		@SuppressWarnings( "unchecked")
		Iterator<Property> properties = persistentClass.getIdentifierMapper().getPropertyIterator();
		Set<SingularAttribute<? super X, ?>> attributes = new HashSet<SingularAttribute<? super X, ?>>();
		while ( properties.hasNext() ) {
			attributes.add(
					(SingularAttribute<? super X, ?>) MetamodelFactory.getAttribute( this, properties.next(), context )
			);
		}
		return attributes;
	}

	public String getName() {
		return className;
	}

	public <Y> SingularAttribute<? super X, Y> getId(Class<Y> type) {
		//TODO check that type and id.getJavaType() are related
		@SuppressWarnings( "unchecked")
		final SingularAttribute<? super X, Y> result = ( SingularAttribute<? super X, Y> ) id;
		return result;
	}

	public <Y> SingularAttribute<? super X, Y> getVersion(Class<Y> type) {
		//TODO check that type and version.getJavaType() are related
		@SuppressWarnings( "unchecked")
		final SingularAttribute<? super X, Y> result = ( SingularAttribute<? super X, Y> ) version;
		return result;
	}

	public <Y> SingularAttribute<X, Y> getDeclaredId(Class<Y> type) {
		//TODO check that type and id.getJavaType() are related
		@SuppressWarnings("unchecked")
		final SingularAttribute<X, Y> result = ( SingularAttribute<X, Y> ) id;
		return result;
	}

	public <Y> SingularAttribute<X, Y> getDeclaredVersion(Class<Y> type) {
		//TODO check that type and version.getJavaType() are related
		@SuppressWarnings("unchecked")
		final SingularAttribute<X, Y> result = ( SingularAttribute<X, Y> ) version;
		return result;
	}

	public IdentifiableType<? super X> getSupertype() {
		return supertype;
	}

	public boolean hasSingleIdAttribute() {
		return hasIdentifierProperty;
	}

	public boolean hasVersionAttribute() {
		return isVersioned;
	}

	public Set<SingularAttribute<? super X, ?>> getIdClassAttributes() {
		if ( hasSingleIdAttribute() ) {
			throw new IllegalArgumentException( "This class does not use @IdClass: " + getName() );
		}
		return idClassAttributes;
	}

	public Type<?> getIdType() {
		return id.getType();
	}

	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	public Class<X> getBindableJavaType() {
		return getJavaType();
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}
}
