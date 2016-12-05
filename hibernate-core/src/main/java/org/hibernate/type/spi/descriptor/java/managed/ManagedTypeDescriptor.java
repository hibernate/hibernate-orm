/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.java.managed.attribute.AttributeBuilderPluralStandardImpl;
import org.hibernate.type.spi.descriptor.java.managed.attribute.AttributeBuilderSingularStandardImpl;

import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class ManagedTypeDescriptor
		implements JavaTypeDescriptorManagedImplementor, InitializationAccess, AttributeDeclarer {
	private static final Logger log = Logger.getLogger( ManagedTypeDescriptor.class );

	private final String typeName;

	private Class javaType;
	private ManagedTypeDescriptor superType;

	private Map<String,AttributeBuilder> declaredAttributeBuilders = new HashMap<>();

	private boolean initialized;
	private Map<String,Attribute> declaredAttributes;

	public ManagedTypeDescriptor(String typeName) {
		this( typeName, null, null );
	}

	public ManagedTypeDescriptor(String typeName, Class javaType, ManagedTypeDescriptor superType) {
		this.typeName = typeName;
		this.javaType = javaType;
		this.superType = superType;
	}

	public ManagedTypeDescriptor(String typeName, Class javaType) {
		this( typeName, javaType, null );
	}

	protected Map<String, AttributeBuilder> declaredAttributeBuilders() {
		return declaredAttributeBuilders;
	}

	@Override
	public Class getJavaTypeClass() {
		return javaType;
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return null;
	}

	@Override
	public ManagedTypeDescriptor getSupertype() {
		return superType;
	}

	@Override
	public Set<Attribute> getDeclaredAttributes() {
		errorIfUninitialized();
		return CollectionHelper.asSet( declaredAttributes.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Attribute> getAttributes() {
		final Set<Attribute> attributes = getDeclaredAttributes();
		if ( getSupertype() != null ) {
			attributes.addAll( getSupertype().getAttributes() );
		}
		return attributes;
	}

	@Override
	public Attribute getDeclaredAttribute(String name) {
		errorIfUninitialized();
		Attribute attr = declaredAttributes.get( name );
		checkNotNull( attr, name, Attribute.class );
		return attr;
	}

	@Override
	public Attribute getAttribute(String name) {
		Attribute attribute = declaredAttributes.get( name );
		if ( attribute == null && getSupertype() != null ) {
			attribute = getSupertype().getAttribute( name );
		}
		checkNotNull( attribute, name, Attribute.class );
		return attribute;
	}

	private void checkNotNull(Attribute attribute, String attributeName, Class<? extends Attribute> expectedType) {
		if ( attribute == null ) {
			throw notFound( attributeName, expectedType );
		}
	}

	private IllegalArgumentException notFound(String attributeName, Class<? extends Attribute> expectedType) {
		assert Attribute.class.equals( expectedType )
				|| SingularAttribute.class.equals( expectedType )
				|| PluralAttribute.class.equals( expectedType )
				|| ListAttribute.class.equals( expectedType )
				|| SetAttribute.class.equals( expectedType )
				|| MapAttribute.class.equals( expectedType )
				|| CollectionAttribute.class.equals( expectedType );

		return new IllegalArgumentException(
				String.format(
						"Unable to locate %s with the the given name [%s] on this ManagedType [%s]",
						expectedType.getSimpleName(),
						attributeName,
						getTypeName()
				)
		);
	}


	@Override
	public Set<SingularAttribute> getSingularAttributes() {
		final HashSet<SingularAttribute> singularAttributes = new HashSet<>();
		filterAndCollectAttributes( singularAttributes, SingularAttribute.class, true );
		return singularAttributes;
	}

	private <T extends Attribute> void filterAndCollectAttributes(
			HashSet<T> collectionTarget,
			Class<T> specificAttributeClass,
			boolean includeSuper) {
		errorIfUninitialized();
		declaredAttributes.values()
				.stream()
				.filter( specificAttributeClass::isInstance )
				.map( specificAttributeClass::cast )
				.forEach( collectionTarget::add );

		if ( getSupertype() != null ) {
			getSupertype().filterAndCollectAttributes( collectionTarget, specificAttributeClass, includeSuper );
		}
	}

	@Override
	public Set<SingularAttribute> getDeclaredSingularAttributes() {
		final HashSet<SingularAttribute> singularAttributes = new HashSet<>();
		filterAndCollectAttributes( singularAttributes, SingularAttribute.class, false );
		return singularAttributes;
	}

	@Override
	public SingularAttribute getSingularAttribute(String name) {
		return locateAttribute( name, SingularAttribute.class, true );
	}

	@SuppressWarnings("unchecked")
	public <T extends Attribute> T locateAttribute(String name, Class<T> expectedType, boolean checkSupertype) {
		errorIfUninitialized();

		Attribute attribute = declaredAttributes.get( name );
		if ( attribute != null ) {
			checkType( attribute, expectedType );
			return (T) attribute;
		}

		if ( checkSupertype && getSupertype() != null ) {
			attribute = getSupertype().getSingularAttribute( name );
			if ( attribute != null ) {
				checkType( attribute, expectedType );
				return (T) attribute;
			}
		}

		throw notFound( name, SingularAttribute.class );
	}

	private void checkType(Attribute attribute, Class<? extends Attribute> expectedType) {
		assert Attribute.class.equals( expectedType )
				|| SingularAttribute.class.equals( expectedType )
				|| PluralAttribute.class.equals( expectedType )
				|| ListAttribute.class.equals( expectedType )
				|| SetAttribute.class.equals( expectedType )
				|| MapAttribute.class.equals( expectedType )
				|| CollectionAttribute.class.equals( expectedType );

		if ( !expectedType.isInstance( attribute ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Attribute with the given name [%s] was found on the ManagedType [%s], but it was not a %s : %s",
							attribute.getName(),
							this,
							expectedType.getSimpleName(),
							attribute
					)
			);
		}
	}

	@Override
	public SingularAttribute getDeclaredSingularAttribute(String name) {
		return locateAttribute( name, SingularAttribute.class, false );
	}

	@Override
	public SingularAttribute getSingularAttribute(String name, Class javaType) {
		final SingularAttribute attribute = locateAttribute( name, SingularAttribute.class, true );
		checkTypeForSingularAttribute( attribute, javaType );
		return attribute;
	}

	@Override
	public SingularAttribute getDeclaredSingularAttribute(String name, Class javaType) {
		final SingularAttribute attribute = locateAttribute( name, SingularAttribute.class, false );
		checkTypeForSingularAttribute( attribute, javaType );
		return attribute;
	}

	private void checkTypeForSingularAttribute(SingularAttribute attribute, Class javaType) {
		// NOTE : we use assert for the first since really this should already be checked
		// by callers,.  However, this is the first and primary validation of `javaType`
		// so we use an explicit if-check
		assert attribute != null;
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Passed Java type cannot be null" );
		}

		if ( !attribute.getBindableJavaType().equals( javaType ) ) {
			if ( isPrimitiveVariant( attribute, javaType ) ) {
				return;
			}
			throw new IllegalArgumentException(
					"SingularAttribute named " + attribute.getName()
							+ " and of type " + javaType.getName() + " is not present"
			);
		}
	}

	@SuppressWarnings({ "SimplifiableIfStatement" })
	protected boolean isPrimitiveVariant(SingularAttribute attribute, Class javaType) {
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
	public Set<PluralAttribute> getPluralAttributes() {
		HashSet<PluralAttribute> attributes = new HashSet<>();
		filterAndCollectAttributes( attributes, PluralAttribute.class, true );
		return attributes;
	}

	@Override
	public Set<PluralAttribute> getDeclaredPluralAttributes() {
		HashSet<PluralAttribute> attributes = new HashSet<>();
		filterAndCollectAttributes( attributes, PluralAttribute.class, true );
		return attributes;
	}

	@Override
	public CollectionAttribute getCollection(String name) {
		return locateAttribute( name, CollectionAttribute.class, true );
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name) {
		return locateAttribute( name, CollectionAttribute.class, false );
	}

	@Override
	public SetAttribute getSet(String name) {
		return locateAttribute( name, SetAttribute.class, true );
	}

	@Override
	public SetAttribute getDeclaredSet(String name) {
		return locateAttribute( name, SetAttribute.class, false );
	}

	@Override
	public ListAttribute getList(String name) {
		return locateAttribute( name, ListAttribute.class, true );
	}

	@Override
	public ListAttribute getDeclaredList(String name) {
		return locateAttribute( name, ListAttribute.class, false );
	}

	@Override
	public MapAttribute getMap(String name) {
		return locateAttribute( name, MapAttribute.class, true );
	}

	@Override
	public MapAttribute getDeclaredMap(String name) {
		return locateAttribute( name, MapAttribute.class, false );
	}

	@Override
	public CollectionAttribute getCollection(String name, Class elementType) {
		final CollectionAttribute collectionAttribute = locateAttribute( name, CollectionAttribute.class, true );
		checkRequestedElementType( collectionAttribute, elementType );
		return collectionAttribute;
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name, Class elementType) {
		final CollectionAttribute collectionAttribute = locateAttribute( name, CollectionAttribute.class, false );
		checkRequestedElementType( collectionAttribute, elementType );
		return collectionAttribute;
	}

	private void checkRequestedElementType(PluralAttribute attribute, Class elementType) {
		assert attribute != null;
		if ( elementType == null ) {
			throw new IllegalArgumentException( "Explicit Java type for collection element cannot be nul" );
		}

		if ( !attribute.getBindableJavaType().equals( elementType ) ) {
			throw new IllegalArgumentException(
					"PluralAttribute named " + attribute.getName() +
							" and of element type " + elementType + " is not present"
			);
		}
	}

	@Override
	public SetAttribute getSet(String name, Class elementType) {
		final SetAttribute setAttribute = locateAttribute( name, SetAttribute.class, true );
		checkRequestedElementType( setAttribute, elementType );
		return setAttribute;
	}

	@Override
	public SetAttribute getDeclaredSet(String name, Class elementType) {
		final SetAttribute setAttribute = locateAttribute( name, SetAttribute.class, false );
		checkRequestedElementType( setAttribute, elementType );
		return setAttribute;
	}

	@Override
	public ListAttribute getList(String name, Class elementType) {
		final ListAttribute listAttribute = locateAttribute( name, ListAttribute.class, true );
		checkRequestedElementType( listAttribute, elementType );
		return listAttribute;
	}

	@Override
	public ListAttribute getDeclaredList(String name, Class elementType) {
		final ListAttribute listAttribute = locateAttribute( name, ListAttribute.class, false );
		checkRequestedElementType( listAttribute, elementType );
		return listAttribute;
	}

	@Override
	public MapAttribute getMap(String name, Class keyType, Class valueType) {
		final MapAttribute mapAttribute = locateAttribute( name, MapAttribute.class, true );
		checkRequestedMapKeyType( mapAttribute, keyType );
		checkRequestedElementType( mapAttribute, valueType );
		return mapAttribute;
	}

	private void checkRequestedMapKeyType(MapAttribute mapAttribute, Class keyType) {
		if ( mapAttribute.getKeyJavaType().equals( keyType ) ) {
			throw new IllegalArgumentException( "MapAttribute named " + mapAttribute.getName() + " does not support a key of type " + keyType );
		}
	}

	@Override
	public MapAttribute getDeclaredMap(String name, Class keyType, Class valueType) {
		final MapAttribute mapAttribute = locateAttribute( name, MapAttribute.class, false );
		checkRequestedMapKeyType( mapAttribute, keyType );
		checkRequestedElementType( mapAttribute, valueType );
		return mapAttribute;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#getMutabilityPlan; the mapping type " +
						"ought to implement this in terms of any @Immutable defined 'up' the hierarchy"
		);
	}

	@Override
	public Comparator getComparator() {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#getComparator; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public int extractHashCode(Object value) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#extractHashCode; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public boolean areEqual(Object one, Object another) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#areEqual; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public String extractLoggableRepresentation(Object value) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#extractLoggableRepresentation; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public String toString(Object value) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#toString; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public Object fromString(String string) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#fromString; the mapping type " +
						"ought to implement this"
		);
	}

	@Override
	public Object unwrap(Object value, Class type, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#unwrap; the mapping type " +
						"ought to handle any needed unwrapping"
		);
	}

	@Override
	public Object wrap(Object value, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Unexpected call to ManagedTypeDescriptor#wrap; the mapping type " +
						"ought to handle any needed wrapping"
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void complete() {
		errorIfInitialized();

		declaredAttributes = new HashMap<>();
		for ( Map.Entry<String, AttributeBuilder> entry : declaredAttributeBuilders.entrySet() ) {
			declaredAttributes.put(
					entry.getKey(),
					entry.getValue().build()
			);
		}
	}

	@Override
	public InitializationAccess getInitializationAccess() {
		errorIfInitialized();
		return this;
	}

	protected void errorIfUninitialized() {
		if ( !initialized ) {
			throw new IllegalStateException(
					"ManagedType [" + toString() + "] descriptor not yet fully initialized"
			);
		}
	}

	protected void errorIfInitialized() {
		if ( initialized ) {
			throw new IllegalStateException(
					"ManagedType [" + toString() + "] descriptor was already initialized; illegal access to InitializationAccess"
			);
		}
	}


	@Override
	public void setJavaType(Class javaType) {
		errorIfInitialized();
		log.debugf( "ManagedTypeDescriptor#setJavaType(%s) called; previous value : %s", javaType, this.javaType );
		this.javaType = javaType;
	}

	@Override
	public void setSuperType(JavaTypeDescriptorManagedImplementor superType) {
		errorIfInitialized();
		log.debugf( "ManagedTypeDescriptor#setSuperType(%s) called; previous value : %s", superType, this.superType );
		this.superType = (ManagedTypeDescriptor) superType;
	}

	@Override
	public AttributeBuilderSingular getSingularAttributeBuilder(String name) {
		AttributeBuilder builder = declaredAttributeBuilders.get( name );
		if ( builder != null ) {
			if ( !AttributeBuilderSingular.class.isInstance( builder ) ) {
				throw new HibernateException(
						"Request for SingularAttribute builder named [" + name +
								"] resolved to non-singular attribute builder : " +
								builder
				);
			}
		}
		else {
			builder = new AttributeBuilderSingularStandardImpl( this, name );
			declaredAttributeBuilders.put( name, builder );
		}

		return (AttributeBuilderSingular) builder;
	}

	@Override
	public AttributeBuilderPlural getPluralAttributeBuilder(String name) {
		AttributeBuilder builder = declaredAttributeBuilders.get( name );
		if ( builder != null ) {
			if ( !AttributeBuilderPlural.class.isInstance( builder ) ) {
				throw new HibernateException(
						"Request for AttributeBuilderPlural builder named [" + name +
								"] resolved to non-plural attribute builder : " +
								builder
				);
			}
		}
		else {
			builder = new AttributeBuilderPluralStandardImpl( this,name );
			declaredAttributeBuilders.put( name, builder );
		}

		return (AttributeBuilderPlural) builder;
	}

	@Override
	public void attributeBuilt(Attribute attribute) {
		errorIfInitialized();
		log.debugf( "ManagedTypeDescriptor[%s] attribute [%s] built : %s", getTypeName(), attribute.getName(), attribute );
	}
}
