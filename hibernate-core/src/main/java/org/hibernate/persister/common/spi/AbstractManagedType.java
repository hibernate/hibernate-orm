/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<T> implements ManagedTypeImplementor<T>, TypeConfigurationAware {
	private static final Logger log = Logger.getLogger( AbstractManagedType.class );

	private final JavaTypeDescriptor<T> javaTypeDescriptor;
	private final MutabilityPlan mutabilityPlan;
	private final Comparator comparator;

	private ManagedTypeImplementor  superTypeDescriptor;

	private Map<String,PersistentAttribute> declaredAttributesByName;

	private TypeConfiguration typeConfiguration;

	public AbstractManagedType(JavaTypeDescriptor<T> javaTypeDescriptor) {
		this(
				javaTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator()
		);
	}

	public AbstractManagedType(
			JavaTypeDescriptor<T> javaTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;
	}

	protected void injectSuperTypeDescriptor(ManagedTypeImplementor superTypeDescriptor) {
		log.debugf(
				"Injecting super-type descriptor [%s] for ManagedTypeImplementor [%s]; was [%s]",
				superTypeDescriptor,
				this,
				this.superTypeDescriptor
		);
		this.superTypeDescriptor = superTypeDescriptor;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;

	}

	@Override
	public ManagedTypeImplementor getSuperType() {
		return superTypeDescriptor;
	}

	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public boolean canCompositeContainCollections() {
		return true;
	}


	protected void addAttribute(PersistentAttribute persistentAttribute) {
		if ( declaredAttributesByName == null ) {
			declaredAttributesByName = new HashMap<>();
		}
		declaredAttributesByName.put( persistentAttribute.getAttributeName(), persistentAttribute );
	}

	@Override
	public PersistentAttribute findAttribute(String name) {
		final PersistentAttribute declaredPersistentAttribute = findDeclaredAttribute( name );
		if ( declaredPersistentAttribute != null ) {
			return declaredPersistentAttribute;
		}

		if ( getSuperType() != null ) {
			final PersistentAttribute superPersistentAttribute = getSuperType().findAttribute( name );
			if ( superPersistentAttribute != null ) {
				return superPersistentAttribute;
			}
		}

		return null;
	}

	@Override
	public PersistentAttribute findDeclaredAttribute(String name) {
		if ( declaredAttributesByName == null ) {
			return null;
		}

		return declaredAttributesByName.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// get groups of attributes

	public void collectDeclaredAttributes(Consumer<javax.persistence.metamodel.Attribute> collector) {
		collectDeclaredAttributes( collector, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void collectDeclaredAttributes(Consumer collector, Class restrictionType) {
		if ( declaredAttributesByName != null ) {
			Stream stream = declaredAttributesByName.values().stream();
			if ( restrictionType != null ) {
				stream = stream.filter( restrictionType::isInstance );
			}
			stream.forEach( ormAttribute -> collector.accept( ormAttribute ) );
		}
	}

	private void collectAttributes(Consumer<javax.persistence.metamodel.Attribute> collector) {
		collectAttributes( collector, null );
	}

	@Override
	public void collectAttributes(Consumer collector, Class restrictionType) {
		collectDeclaredAttributes( collector, restrictionType );

		if ( getSuperType() != null  ) {
			getSuperType().collectAttributes( collector, restrictionType );
		}
	}

	@Override
	public Set<javax.persistence.metamodel.Attribute<? super T, ?>> getAttributes() {
		final HashSet<javax.persistence.metamodel.Attribute<? super T, ?>> attributes = new HashSet<>();
		collectAttributes( attributes::add );
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.Attribute<T,?>> getDeclaredAttributes() {
		final HashSet<javax.persistence.metamodel.Attribute> attributes = new HashSet<>();
		collectDeclaredAttributes( attributes::add );
		return attributes.stream().collect( Collectors.toSet() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.SingularAttribute<? super T,?>> getSingularAttributes() {
		final HashSet attributes = new HashSet();
		collectAttributes( attributes::add, javax.persistence.metamodel.SingularAttribute.class );
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.SingularAttribute<T,?>> getDeclaredSingularAttributes() {
		final HashSet attributes = new HashSet<>();
		collectDeclaredAttributes( attributes::add, javax.persistence.metamodel.SingularAttribute.class );
		return attributes;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.PluralAttribute<? super T, ?, ?>> getPluralAttributes() {
		final HashSet attributes = new HashSet<>();
		collectAttributes( attributes::add, PluralPersistentAttribute.class );
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.PluralAttribute<T, ?, ?>> getDeclaredPluralAttributes() {
		final HashSet attributes = new HashSet<>();
		collectDeclaredAttributes( attributes::add, PluralPersistentAttribute.class );
		return attributes;
	}

	@Override
	public Map<String, PersistentAttribute> getAttributesByName() {
		final Map<String, PersistentAttribute> attributeMap = new HashMap<>();
		collectAttributes( attributeMap );
		return attributeMap;
	}

	protected void collectAttributes(Map<String, PersistentAttribute> attributeMap) {
		attributeMap.putAll( getDeclaredAttributesByName() );
		if ( superTypeDescriptor != null && superTypeDescriptor instanceof AbstractManagedType ) {
			( (AbstractManagedType) superTypeDescriptor ).collectAttributes( attributeMap );
		}
	}

	@Override
	public Map<String, PersistentAttribute> getDeclaredAttributesByName() {
		return declaredAttributesByName == null ? Collections.emptyMap() : declaredAttributesByName;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access to information about a single Attribute

	@Override
	public Navigable findNavigable(String navigableName) {
		Navigable attribute = findDeclaredAttribute( navigableName );
		if ( attribute == null && getSuperType() != null ) {
			attribute = getSuperType().findNavigable( navigableName );
		}
		return attribute;
	}

	@Override
	public PersistentAttribute getAttribute(String name) {
		return getAttribute( name, null );
	}

	protected PersistentAttribute getAttribute(String name, Class resultType) {
		PersistentAttribute persistentAttribute = findDeclaredAttribute( name, resultType );
		if ( persistentAttribute == null && getSuperType() != null ) {
			persistentAttribute = getSuperType().findDeclaredAttribute( name, resultType );
		}

		if ( persistentAttribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named [" + name + "] relative to [" + this.asLoggableText() + "]" );
		}

		return persistentAttribute;
	}

	@Override
	public PersistentAttribute findDeclaredAttribute(String name, Class resultType) {
		final PersistentAttribute ormPersistentAttribute = declaredAttributesByName.get( name );
		if ( ormPersistentAttribute == null ) {
			return null;
		}

		if ( ormPersistentAttribute instanceof SingularPersistentAttribute ) {
			checkAttributeType( (SingularPersistentAttribute) ormPersistentAttribute, resultType );
		}
		else {
			checkAttributeType( (PluralPersistentAttribute) ormPersistentAttribute, resultType );
		}

		return ormPersistentAttribute;
	}

	protected void checkAttributeType(SingularPersistentAttribute ormAttribute, Class resultType) {
		checkType(  ormAttribute.getName(), ormAttribute.getJavaType(), resultType );
	}

	protected void checkAttributeType(PluralPersistentAttribute ormAttribute, Class resultType) {
		checkType(  ormAttribute.getName(), ormAttribute.getElementType().getJavaType(), resultType );
	}

	@SuppressWarnings("unchecked")
	protected void checkType(String name, Class attributeType, Class resultType) {
		if ( resultType != null && attributeType != null ) {
			if ( !resultType.isAssignableFrom( attributeType ) ) {
				throw new IllegalArgumentException(
						"Found attribute for given name [" + name +
								"], but its type [" + attributeType +
								"] is not assignable to the requested type [" + resultType + "]"
				);
			}
		}
	}

	@Override
	public PersistentAttribute getDeclaredAttribute(String name) {
		return getDeclaredAttribute( name, null );
	}

	public PersistentAttribute getDeclaredAttribute(String name, Class javaType) {
		final PersistentAttribute persistentAttribute = findDeclaredAttribute( name, javaType );
		if ( persistentAttribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named [" + name + "] relative to [" + this.asLoggableText() + "]" );
		}
		return persistentAttribute;
	}

	@Override
	public SingularPersistentAttribute getSingularAttribute(String name) {
		return getSingularAttribute( name, null );
	}

	@Override
	public SingularPersistentAttribute getSingularAttribute(String name, Class type) {
		return (SingularPersistentAttribute) getAttribute( name, type );
	}

	@Override
	public javax.persistence.metamodel.SingularAttribute getDeclaredSingularAttribute(String name) {
		return getDeclaredSingularAttribute( name, null );
	}

	@Override
	public SingularPersistentAttribute getDeclaredSingularAttribute(String name, Class type) {
		return (SingularPersistentAttribute) getDeclaredAttribute( name, type );
	}

	@Override
	public CollectionAttribute getCollection(String name) {
		return getCollection( name, null );
	}

	@Override
	public CollectionAttribute getCollection(String name, Class elementType) {
		return (CollectionAttribute) getAttribute( name, elementType );
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name) {
		return getDeclaredCollection( name, null );
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name, Class elementType) {
		return (CollectionAttribute) getDeclaredAttribute( name, elementType );
	}

	@Override
	public ListAttribute getList(String name) {
		return getList( name, null );
	}

	@Override
	public ListAttribute getList(String name, Class elementType) {
		return (ListAttribute) getAttribute( name, elementType );
	}

	@Override
	public ListAttribute getDeclaredList(String name) {
		return getDeclaredList( name, null );
	}

	@Override
	public ListAttribute getDeclaredList(String name, Class elementType) {
		return (ListAttribute) getDeclaredAttribute( name, elementType );
	}

	@Override
	public MapAttribute getMap(String name) {
		return getMap( name, null, null );
	}

	@Override
	public MapAttribute getMap(String name, Class keyType, Class valueType) {
		final MapAttribute mapAttribute = (MapAttribute) getAttribute( name, valueType );
		if ( mapAttribute == null ) {
			return null;
		}

		checkMapKeyType( name, mapAttribute.getKeyJavaType(), keyType  );

		return mapAttribute;
	}

	@SuppressWarnings("unchecked")
	private void checkMapKeyType(String name, Class attributeType, Class resultType) {
		checkType( name + ".key", attributeType, resultType );
	}

	@Override
	public MapAttribute getDeclaredMap(String name) {
		return getDeclaredMap( name, null, null );
	}

	@Override
	public MapAttribute getDeclaredMap(String name, Class keyType, Class valueType) {
		final MapAttribute mapAttribute = (MapAttribute) getDeclaredAttribute( name, valueType );
		if ( mapAttribute == null ) {
			return null;
		}

		checkMapKeyType( name, keyType, mapAttribute.getKeyJavaType() );

		return mapAttribute;
	}

	@Override
	public SetAttribute getSet(String name) {
		return getSet( name, null );
	}

	@Override
	public SetAttribute getSet(String name, Class elementType) {
		return (SetAttribute) getAttribute( name, elementType );
	}

	@Override
	public SetAttribute getDeclaredSet(String name) {
		return getDeclaredSet( name, null );
	}

	@Override
	public SetAttribute getDeclaredSet(String name, Class elementType) {
		return (SetAttribute) getDeclaredAttribute( name, elementType );
	}
}
