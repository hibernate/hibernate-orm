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
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.MutabilityPlan;
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
	private final RepresentationStrategy representationStrategy;

	private final TypeConfiguration typeConfiguration;

	private InheritanceCapable<? super J>  superTypeDescriptor;
	private List<InheritanceCapable<? extends J>> subclassTypes;

	private final Set<String> subClassEntityNames = new HashSet<>();

	// todo (6.0) : I think we can just drop the mutabilityPlan and comparator for managed types

	private final MutabilityPlan<J> mutabilityPlan;
	private final Comparator<J> comparator;



	// todo (6.0) : we need some kind of callback after all Navigables have been added to all containers
	//		use that callback to build these 2 lists - they are cached resolutions
	//		for performance rather that "recalculating" each time
	//
	//		see `#getNavigables` and `#getDeclaredNavigables` below.


	private List<NonIdPersistentAttribute<J,?>> declaredAttributes;
	private List<NonIdPersistentAttribute<J,?>> attributes;

	private List<StateArrayContributor<?>> stateArrayContributors;

	// a cache to more easily find the PersistentAttribute by name
	private Map<String,NonIdPersistentAttribute> declaredAttributesByName;

	public AbstractManagedType(
			ManagedTypeMapping managedTypeMapping,
			ManagedJavaDescriptor<J> javaTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		this(
				managedTypeMapping,
				javaTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator(),
				creationContext
		);
	}

	@SuppressWarnings("WeakerAccess")
	public AbstractManagedType(
			ManagedTypeMapping managedTypeMapping,
			ManagedJavaDescriptor<J> javaTypeDescriptor,
			MutabilityPlan<J> mutabilityPlan,
			Comparator<J> comparator,
			RuntimeModelCreationContext creationContext) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;

		this.representationStrategy = creationContext.getRepresentationStrategySelector()
				.resolveRepresentationStrategy( managedTypeMapping, creationContext );
		this.typeConfiguration = creationContext.getTypeConfiguration();
	}

	@SuppressWarnings("unchecked")
	public void finishInitialization(
			InheritanceCapable superType,
			ManagedTypeMappingImplementor mappingDescriptor,
			RuntimeModelCreationContext creationContext) {
		injectSuperTypeDescriptor( superTypeDescriptor );

		final int declaredAttributeCount = mappingDescriptor.getDeclaredPersistentAttributes().size();

		declaredAttributes = CollectionHelper.arrayList( declaredAttributeCount );
		declaredAttributesByName = CollectionHelper.concurrentMap( declaredAttributeCount );
		// NOTE : we can't know the size of declared contributors up front
		stateArrayContributors = new ArrayList<>();

		if ( superType != null ) {
			attributes = CollectionHelper.arrayList(
					superType.getPersistentAttributes().size() + declaredAttributeCount
			);

			attributes.addAll( superType.getPersistentAttributes() );
			stateArrayContributors.addAll( superType.getStateArrayContributors() );
		}
		else {
			attributes = CollectionHelper.arrayList( declaredAttributeCount );
		}

		final List<PersistentAttributeMapping> sortedAttributeMappings = new ArrayList<>( mappingDescriptor.getDeclaredPersistentAttributes() );
		sortedAttributeMappings.sort( Comparator.comparing( PersistentAttributeMapping::getName ) );

		for ( PersistentAttributeMapping attributeMapping : sortedAttributeMappings ) {
			final PersistentAttribute persistentAttribute = attributeMapping.makeRuntimeAttribute(
					this,
					mappingDescriptor,
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

	@Override
	public void injectSuperTypeDescriptor(InheritanceCapable<? super J> superTypeDescriptor) {
		log.debugf(
				"Injecting super-type descriptor [%s] for ManagedTypeImplementor [%s]; was [%s]",
				superTypeDescriptor,
				this,
				this.superTypeDescriptor
		);
		this.superTypeDescriptor = superTypeDescriptor;

		superTypeDescriptor.addSubclassType( this );
	}

	@Override
	public void addSubclassType(InheritanceCapable<? extends J> subclassType) {
		subclassTypes.add( subclassType );
		subClassEntityNames.add( subclassType.getNavigableName() );
	}


	@Override
	public List<InheritanceCapable<? extends J>> getSubclassTypes() {
		return subclassTypes;
	}

	@Override
	public boolean isSubclassEntityName(String entityName) {
		return subClassEntityNames.contains( entityName );
	}

	@Override
	public RepresentationStrategy getRepresentationStrategy() {
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
	public void visitStateArrayNavigables(Consumer<StateArrayContributor<?>> consumer) {
		visitAttributes(
				attribute -> {
					if ( attribute != null ) {
						consumer.accept( (StateArrayContributor<?>) attribute );
					}
				}
		);
	}

	@Override
	public boolean hasMutableProperties() {
		throw new NotYetImplementedFor6Exception(  );
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
	public List<StateArrayContributor<?>> getStateArrayContributors() {
		return stateArrayContributors;
	}
}
