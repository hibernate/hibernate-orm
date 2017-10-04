/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.internal.NavigableSpliterator;
import org.hibernate.metamodel.model.domain.internal.PersistentAttributeSpliterator;
import org.hibernate.metamodel.model.domain.internal.StateArrayContributorSpliterator;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedType<T> implements InheritanceCapable<T> {

	private static final Logger log = Logger.getLogger( AbstractManagedType.class );

	// todo (6.0) : I think we can just drop the mutabilityPlan and comparator for managed types

	private final ManagedJavaDescriptor<T> javaTypeDescriptor;
	private final RepresentationStrategy representationStrategy;

	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	private final TypeConfiguration typeConfiguration;

	private InheritanceCapable<? super T>  superTypeDescriptor;
	private List<InheritanceCapable<? extends T>> subclassTypes;

	private Map<String,PersistentAttribute> declaredAttributesByName;

	public AbstractManagedType(
			ManagedTypeMapping managedTypeMapping,
			ManagedJavaDescriptor<T> javaTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		this(
				managedTypeMapping,
				javaTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator(),
				creationContext
		);
	}

	public AbstractManagedType(
			ManagedTypeMapping managedTypeMapping,
			ManagedJavaDescriptor<T> javaTypeDescriptor,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			RuntimeModelCreationContext creationContext) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;

		this.representationStrategy = creationContext.getRepresentationStrategySelector()
				.resolveRepresentationStrategy( managedTypeMapping, creationContext );
		this.typeConfiguration = creationContext.getTypeConfiguration();
	}


	@Override
	public void injectSuperTypeDescriptor(InheritanceCapable<? super T> superTypeDescriptor) {
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
	public void addSubclassType(InheritanceCapable<? extends T> subclassType) {
		subclassTypes.add( subclassType );
	}

	@Override
	public List<InheritanceCapable<? extends T>> getSubclassTypes() {
		return subclassTypes;
	}

	@Override
	public RepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public InheritanceCapable<? super T> getSuperclassType() {
		return superTypeDescriptor;
	}

	public ManagedJavaDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public boolean canCompositeContainCollections() {
		return true;
	}


	protected void addAttribute(PersistentAttribute persistentAttribute) {
		if ( declaredAttributesByName == null ) {
			// NOTE : TreeMap so that we sort based on attribute name
			declaredAttributesByName = new TreeMap<>();
		}
		declaredAttributesByName.put( persistentAttribute.getAttributeName(), persistentAttribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return declaredAttributesByName.get( navigableName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getNavigables() {
		final ArrayList<Navigable> navigables = new ArrayList<>();
		navigables.addAll( declaredAttributesByName.values() );
		if ( getSuperclassType() != null ) {
			navigables.addAll( getSuperclassType().getNavigables() );
		}
		return navigables;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getDeclaredNavigables() {
		return new ArrayList<>( declaredAttributesByName.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Spliterator navigableSource() {
		return new NavigableSpliterator( this, true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Spliterator declaredNavigableSource() {
		return new NavigableSpliterator( this, true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Spliterator persistentAttributeSource() {
		return new PersistentAttributeSpliterator( this, true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Spliterator declaredPersistentAttributeSource() {
		return declaredAttributesByName.values().spliterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Spliterator stateArrayContributorSource() {
		return new StateArrayContributorSpliterator( this );
	}

	@Override
	public PersistentAttribute<? super T, ?> findAttribute(String name) {
		final PersistentAttribute<? super T, ?> declaredPersistentAttribute = findDeclaredAttribute( name );
		if ( declaredPersistentAttribute != null ) {
			return declaredPersistentAttribute;
		}

		if ( getSuperclassType() != null ) {
			final PersistentAttribute<? super T, ?> superPersistentAttribute = getSuperclassType().findAttribute( name );
			if ( superPersistentAttribute != null ) {
				return superPersistentAttribute;
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentAttribute<? super T, ?> findDeclaredAttribute(String name) {
		if ( declaredAttributesByName == null ) {
			return null;
		}

		return declaredAttributesByName.get( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// get groups of attributes


	@Override
	public void visitStateArrayNavigables(Consumer<StateArrayElementContributor<?>> consumer) {
		visitAttributes(
				attribute -> {
					if ( attribute instanceof StateArrayElementContributor ) {
						consumer.accept( (StateArrayElementContributor<?>) attribute );
					}
				}
		);
	}

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

		if ( getSuperclassType() != null  ) {
			getSuperclassType().collectAttributes( collector, restrictionType );
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
		final HashSet<javax.persistence.metamodel.Attribute<T, ?>> attributes = new HashSet<>();
		collectDeclaredAttributes( attributes::add );
		return attributes;
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
	public PersistentAttribute<? super T, ?> getAttribute(String name) {
		return getAttribute( name, null );
	}

	protected PersistentAttribute<? super T, ?> getAttribute(String name, Class resultType) {
		PersistentAttribute<? super T, ?> persistentAttribute = findDeclaredAttribute( name, resultType );
		if ( persistentAttribute == null && getSuperclassType() != null ) {
			persistentAttribute = getSuperclassType().findDeclaredAttribute( name, resultType );
		}

		if ( persistentAttribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named [" + name + "] relative to [" + this.asLoggableText() + "]" );
		}

		return persistentAttribute;
	}

	@Override
	public PersistentAttribute<? super T, ?> findDeclaredAttribute(String name, Class resultType) {
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
	public PluralAttributeCollection getCollection(String name) {
		return getCollection( name, null );
	}

	@Override
	public PluralAttributeCollection getCollection(String name, Class elementType) {
		return (PluralAttributeCollection) getAttribute( name, elementType );
	}

	@Override
	public PluralAttributeCollection getDeclaredCollection(String name) {
		return getDeclaredCollection( name, null );
	}

	@Override
	public PluralAttributeCollection getDeclaredCollection(String name, Class elementType) {
		return (PluralAttributeCollection) getDeclaredAttribute( name, elementType );
	}

	@Override
	public PluralAttributeList getList(String name) {
		return getList( name, null );
	}

	@Override
	public PluralAttributeList getList(String name, Class elementType) {
		return (PluralAttributeList) getAttribute( name, elementType );
	}

	@Override
	public PluralAttributeList getDeclaredList(String name) {
		return getDeclaredList( name, null );
	}

	@Override
	public PluralAttributeList getDeclaredList(String name, Class elementType) {
		return (PluralAttributeList) getDeclaredAttribute( name, elementType );
	}

	@Override
	public PluralAttributeMap getMap(String name) {
		return getMap( name, null, null );
	}

	@Override
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
	public PluralAttributeMap getDeclaredMap(String name) {
		return getDeclaredMap( name, null, null );
	}

	@Override
	public PluralAttributeMap getDeclaredMap(String name, Class keyType, Class valueType) {
		final PluralAttributeMap mapAttribute = (PluralAttributeMap) getDeclaredAttribute( name, valueType );
		if ( mapAttribute == null ) {
			return null;
		}

		checkMapKeyType( name, keyType, mapAttribute.getKeyJavaType() );

		return mapAttribute;
	}

	@Override
	public PluralAttributeSet getSet(String name) {
		return getSet( name, null );
	}

	@Override
	public PluralAttributeSet getSet(String name, Class elementType) {
		return (PluralAttributeSet) getAttribute( name, elementType );
	}

	@Override
	public PluralAttributeSet getDeclaredSet(String name) {
		return getDeclaredSet( name, null );
	}

	@Override
	public PluralAttributeSet getDeclaredSet(String name, Class elementType) {
		return (PluralAttributeSet) getDeclaredAttribute( name, elementType );
	}
}
