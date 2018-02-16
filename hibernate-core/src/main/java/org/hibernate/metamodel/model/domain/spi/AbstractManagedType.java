/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<J> implements InheritanceCapable<J> {

	private static final Logger log = Logger.getLogger( AbstractManagedType.class );

	// todo (6.0) : I think we can just drop the mutabilityPlan and comparator for managed types

	private final ManagedJavaDescriptor<J> javaTypeDescriptor;
	private final ManagedTypeRepresentationStrategy representationStrategy;

	private final TypeConfiguration typeConfiguration;

	private final InheritanceCapable<? super J> superTypeDescriptor;
	private final Set<InheritanceCapable<? extends J>> subclassTypes = ConcurrentHashMap.newKeySet();
	private final Set<String> subClassEntityNames = ConcurrentHashMap.newKeySet();


	// todo (6.0) : we need some kind of callback after all Navigables have been added to all containers
	//		use that callback to build these 2 lists - they are cached resolutions
	//		for performance rather that "recalculating" each time
	//
	//		see `#getNavigables` and `#getDeclaredNavigables` below.


	private List<NonIdPersistentAttribute> declaredAttributes;
	private List<NonIdPersistentAttribute> attributes;

	private List<StateArrayContributor> stateArrayContributors;

	// a cache to more easily find the PersistentAttribute by name
	private Map<String,NonIdPersistentAttribute> declaredAttributesByName;

	@SuppressWarnings("WeakerAccess")
	public AbstractManagedType(
			ManagedTypeMapping managedTypeMapping,
			InheritanceCapable<? super J> superTypeDescriptor,
			ManagedJavaDescriptor<J> javaTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.superTypeDescriptor = superTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;

		this.typeConfiguration = creationContext.getTypeConfiguration();

		this.representationStrategy = creationContext.getRepresentationStrategySelector()
				.resolveStrategy( managedTypeMapping, this, creationContext );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final int declaredAttributeCount = bootDescriptor.getDeclaredPersistentAttributes().size();

		declaredAttributes = CollectionHelper.arrayList( declaredAttributeCount );
		declaredAttributesByName = CollectionHelper.concurrentMap( declaredAttributeCount );
		// NOTE : we can't know the size of declared contributors up front
		stateArrayContributors = new ArrayList<>();

		if ( superTypeDescriptor != null ) {
			attributes = CollectionHelper.arrayList(
					superTypeDescriptor.getPersistentAttributes().size() + declaredAttributeCount
			);

			attributes.addAll( superTypeDescriptor.getPersistentAttributes() );
			stateArrayContributors.addAll( superTypeDescriptor.getStateArrayContributors() );
		}
		else {
			attributes = CollectionHelper.arrayList( declaredAttributeCount );
		}

		final List<PersistentAttributeMapping> sortedAttributeMappings = new ArrayList<>( bootDescriptor.getDeclaredPersistentAttributes() );
		sortedAttributeMappings.sort( Comparator.comparing( PersistentAttributeMapping::getName ) );

		for ( PersistentAttributeMapping attributeMapping : sortedAttributeMappings ) {
			final PersistentAttribute persistentAttribute = attributeMapping.makeRuntimeAttribute(
					this,
					bootDescriptor,
					SingularPersistentAttribute.Disposition.NORMAL,
					creationContext
			);

			if ( !NonIdPersistentAttribute.class.isInstance( persistentAttribute ) ) {
				throw new HibernateException(
						String.format(
								Locale.ROOT,
								"Boot-time attribute descriptor [%s] made non-NonIdPersistentAttribute, " +
										"while a NonIdPersistentAttribute was expected : %s",
								attributeMapping,
								persistentAttribute
						)
				);
			}

			attributes.add( (NonIdPersistentAttribute) persistentAttribute );
			declaredAttributes.add( (NonIdPersistentAttribute) persistentAttribute );
			declaredAttributesByName.put( persistentAttribute.getAttributeName(), (NonIdPersistentAttribute) persistentAttribute );

			final StateArrayContributor contributor = (StateArrayContributor) persistentAttribute;
			contributor.setStateArrayPosition( stateArrayContributors.size() );
			stateArrayContributors.add( contributor );
		}
	}


	public void addSubclassDescriptor(InheritanceCapable<? extends J> subclassType) {
		log.debugf(
				"Adding runtime descriptor [%s] as subclass for ManagedType [%s]",
				subclassType.getJavaTypeDescriptor().getTypeName(),
				this.getJavaTypeDescriptor().getTypeName()
		);

		subclassTypes.add( subclassType );
		addSubclassName( subclassType );
	}

	protected void addSubclassName(InheritanceCapable subclassType) {
		subClassEntityNames.add( subclassType.getNavigableName() );

		if ( superTypeDescriptor != null ) {
			if ( !AbstractManagedType.class.isInstance( subclassType ) ) {
				throw new HibernateException(
						"Expecting super type to be derived from AbstractManagedType : " + superTypeDescriptor
				);
			}

			( (AbstractManagedType) superTypeDescriptor ).addSubclassName( subclassType );
		}
	}

	@Override
	public Collection<InheritanceCapable<? extends J>> getSubclassTypes() {
		return subclassTypes;
	}

	@Override
	public boolean isSubclassTypeName(String name) {
		return subClassEntityNames.contains( name );
	}

	@Override
	public ManagedTypeRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public InheritanceCapable<? super J> getSuperclassType() {
		return superTypeDescriptor;
	}

	public ManagedJavaDescriptor<J> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public boolean canCompositeContainCollections() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return declaredAttributesByName.get( navigableName );
	}

	@Override
	public NonIdPersistentAttribute<? super J, ?> findPersistentAttribute(String name) {
		final NonIdPersistentAttribute<? super J, ?> declaredPersistentAttribute = findDeclaredPersistentAttribute( name );
		if ( declaredPersistentAttribute != null ) {
			return declaredPersistentAttribute;
		}

		if ( getSuperclassType() != null ) {
			final NonIdPersistentAttribute<? super J, ?> superPersistentAttribute = getSuperclassType().findPersistentAttribute( name );
			if ( superPersistentAttribute != null ) {
				return superPersistentAttribute;
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NonIdPersistentAttribute<? super J, ?> findDeclaredPersistentAttribute(String name) {
		if ( declaredAttributesByName == null ) {
			return null;
		}

		return declaredAttributesByName.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// get groups of attributes


	@Override
	@SuppressWarnings("unchecked")
	public List<NonIdPersistentAttribute> getPersistentAttributes() {
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<NonIdPersistentAttribute> getDeclaredPersistentAttributes() {
		return declaredAttributes;
	}

	@Override
	public void visitStateArrayNavigables(Consumer<StateArrayContributor<?>> consumer) {
		visitAttributes(
				attribute -> {
					if ( attribute != null ) {
						consumer.accept( (StateArrayContributor<?>) attribute );
					}
				}
		);
	}



	// todo (6.0) : make sure we are only iterating the attributes once to do all of these kinds of initialization

	Boolean hasMutableProperties;

	@Override
	public boolean hasMutableProperties() {
		if ( hasMutableProperties == null ) {
			hasMutableProperties = lookForMutableAttributes();
		}

		return hasMutableProperties;
	}

	private Boolean lookForMutableAttributes() {
		if ( superTypeDescriptor != null ) {
			if ( superTypeDescriptor.hasMutableProperties() ) {
				return true;
			}
		}

		for ( NonIdPersistentAttribute attribute : declaredAttributes ) {
			if ( attribute.isUpdatable() ) {
				return true;
			}
		}

		// sub-types?

		return false;

	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected <R extends PersistentAttribute> void collectDeclaredAttributes(Collection<R> collection, Class<R> restrictionType) {
		for ( PersistentAttribute<J, ?> declaredAttribute : declaredAttributes ) {
			if ( restrictionType.isInstance( declaredAttribute ) ) {
				collection.add( (R) declaredAttribute );
			}
		}
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected <R extends PersistentAttribute> void collectAttributes(Collection<R> collection, Class<R> restrictionType) {
		for ( PersistentAttribute<J, ?> attribute : attributes ) {
			if ( restrictionType == null || restrictionType.isInstance( attribute ) ) {
				collection.add( (R) attribute );
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.Attribute<? super J, ?>> getAttributes() {
		return new HashSet( this.attributes );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.Attribute<J,?>> getDeclaredAttributes() {
		return new HashSet( this.declaredAttributes );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.SingularAttribute<? super J,?>> getSingularAttributes() {
		final HashSet jpaAttributes = new HashSet();
		collectAttributes( jpaAttributes, SingularPersistentAttribute.class );
		return jpaAttributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.SingularAttribute<J,?>> getDeclaredSingularAttributes() {
		final HashSet jpaAttributes = new HashSet();
		collectDeclaredAttributes( jpaAttributes, SingularPersistentAttribute.class );
		return jpaAttributes;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.PluralAttribute<? super J, ?, ?>> getPluralAttributes() {
		final HashSet attributes = new HashSet<>();
		collectAttributes( attributes, PluralPersistentAttribute.class );
		return attributes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<javax.persistence.metamodel.PluralAttribute<J, ?, ?>> getDeclaredPluralAttributes() {
		final HashSet attributes = new HashSet<>();
		collectDeclaredAttributes( attributes, PluralPersistentAttribute.class );
		return attributes;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access to information about a single Attribute

	@Override
	@SuppressWarnings("unchecked")
	public Navigable findNavigable(String navigableName) {
		Navigable attribute = findDeclaredPersistentAttribute( navigableName );
		if ( attribute == null && getSuperclassType() != null ) {
			attribute = getSuperclassType().findNavigable( navigableName );
		}
		return attribute;
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		visitDeclaredNavigables( visitor );
		if ( getSuperclassType() != null ) {
			getSuperclassType().visitNavigables( visitor );
		}
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		for ( PersistentAttribute persistentAttribute : declaredAttributesByName.values() ) {
			persistentAttribute.visitNavigable( visitor );
		}
	}

	@Override
	public PersistentAttribute<? super J, ?> getAttribute(String name) {
		return getAttribute( name, null );
	}

	protected PersistentAttribute<? super J, ?> getAttribute(String name, Class resultType) {
		PersistentAttribute<? super J, ?> persistentAttribute = findDeclaredPersistentAttribute( name, resultType );
		if ( persistentAttribute == null && getSuperclassType() != null ) {
			persistentAttribute = getSuperclassType().findDeclaredPersistentAttribute( name, resultType );
		}

		if ( persistentAttribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named [" + name + "] relative to [" + this.asLoggableText() + "]" );
		}

		return persistentAttribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NonIdPersistentAttribute<? super J, ?> findDeclaredPersistentAttribute(String name, Class resultType) {
		final NonIdPersistentAttribute ormPersistentAttribute = declaredAttributesByName.get( name );
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

	@SuppressWarnings("WeakerAccess")
	protected void checkAttributeType(SingularPersistentAttribute ormAttribute, Class resultType) {
		checkType(  ormAttribute.getName(), ormAttribute.getJavaType(), resultType );
	}

	@SuppressWarnings("WeakerAccess")
	protected void checkAttributeType(PluralPersistentAttribute ormAttribute, Class resultType) {
		checkType(  ormAttribute.getName(), ormAttribute.getElementType().getJavaType(), resultType );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
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
	@SuppressWarnings("unchecked")
	public NonIdPersistentAttribute getDeclaredAttribute(String name) {
		return getDeclaredAttribute( name, null );
	}

	@SuppressWarnings("WeakerAccess")
	public NonIdPersistentAttribute getDeclaredAttribute(String name, Class javaType) {
		final NonIdPersistentAttribute persistentAttribute = findDeclaredPersistentAttribute( name, javaType );
		if ( persistentAttribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named [" + name + "] relative to [" + this.asLoggableText() + "]" );
		}
		return persistentAttribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute getSingularAttribute(String name) {
		return getSingularAttribute( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute getSingularAttribute(String name, Class type) {
		return (SingularPersistentAttribute) getAttribute( name, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public javax.persistence.metamodel.SingularAttribute getDeclaredSingularAttribute(String name) {
		return getDeclaredSingularAttribute( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute getDeclaredSingularAttribute(String name, Class type) {
		return (SingularPersistentAttribute) getDeclaredAttribute( name, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeCollection getCollection(String name) {
		return getCollection( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeCollection getCollection(String name, Class elementType) {
		return (PluralAttributeCollection) getAttribute( name, elementType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeCollection getDeclaredCollection(String name) {
		return getDeclaredCollection( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeCollection getDeclaredCollection(String name, Class elementType) {
		return (PluralAttributeCollection) getDeclaredAttribute( name, elementType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeList getList(String name) {
		return getList( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeList getList(String name, Class elementType) {
		return (PluralAttributeList) getAttribute( name, elementType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeList getDeclaredList(String name) {
		return getDeclaredList( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeList getDeclaredList(String name, Class elementType) {
		return (PluralAttributeList) getDeclaredAttribute( name, elementType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeMap getMap(String name) {
		return getMap( name, null, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeMap getMap(String name, Class keyType, Class valueType) {
		final PluralAttributeMap mapAttribute = (PluralAttributeMap) getAttribute( name, valueType );
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
	@SuppressWarnings("unchecked")
	public PluralAttributeMap getDeclaredMap(String name) {
		return getDeclaredMap( name, null, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeMap getDeclaredMap(String name, Class keyType, Class valueType) {
		final PluralAttributeMap mapAttribute = (PluralAttributeMap) getDeclaredAttribute( name, valueType );
		if ( mapAttribute == null ) {
			return null;
		}

		checkMapKeyType( name, keyType, mapAttribute.getKeyJavaType() );

		return mapAttribute;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeSet getSet(String name) {
		return getSet( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeSet getSet(String name, Class elementType) {
		return (PluralAttributeSet) getAttribute( name, elementType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeSet getDeclaredSet(String name) {
		return getDeclaredSet( name, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PluralAttributeSet getDeclaredSet(String name, Class elementType) {
		return (PluralAttributeSet) getDeclaredAttribute( name, elementType );
	}

	@Override
	public List<StateArrayContributor> getStateArrayContributors() {
		return stateArrayContributors;
	}
}
