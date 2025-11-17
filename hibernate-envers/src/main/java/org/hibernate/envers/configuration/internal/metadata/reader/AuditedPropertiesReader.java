/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.CollectionAuditTable;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.internal.ModifiedColumnNameResolver;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import static org.hibernate.envers.configuration.internal.ModelsHelper.dynamicFieldDetails;
import static org.hibernate.envers.configuration.internal.ModelsHelper.getMember;
import static org.hibernate.envers.internal.tools.Tools.newHashMap;
import static org.hibernate.envers.internal.tools.Tools.newHashSet;

/**
 * Reads persistent properties form a {@link PersistentPropertiesSource} and adds the ones that are audited to a
 * {@link AuditedPropertiesHolder}, filling all the auditing data.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Erik-Berndt Scheper
 * @author Hern&aacut;n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 * @author Chris Cranford
 */
public class AuditedPropertiesReader {

	private static final AuditJoinTableData DEFAULT_AUDIT_JOIN_TABLE = new AuditJoinTableData();

	public static final String NO_PREFIX = "";

	private final PersistentPropertiesSource persistentPropertiesSource;
	private final AuditedPropertiesHolder auditedPropertiesHolder;
	private final EnversMetadataBuildingContext metadataBuildingContext;
	private final String propertyNamePrefix;

	private final Map<String, String> propertyAccessedPersistentProperties;
	private final Set<String> fieldAccessedPersistentProperties;
	// Mapping class field to corresponding <properties> element.
	private final Map<String, String> propertiesGroupMapping;

	private final Set<MemberDetails> overriddenAuditedProperties;
	private final Set<MemberDetails> overriddenNotAuditedProperties;
	private final Map<MemberDetails, AuditJoinTable> overriddenAuditedPropertiesJoinTables;

	private final Set<ClassDetails> overriddenAuditedClasses;
	private final Set<ClassDetails> overriddenNotAuditedClasses;

	public AuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder) {
		this ( metadataBuildingContext, persistentPropertiesSource, auditedPropertiesHolder, NO_PREFIX );
	}

	public AuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			String propertyNamePrefix) {
		this.persistentPropertiesSource = persistentPropertiesSource;
		this.auditedPropertiesHolder = auditedPropertiesHolder;
		this.metadataBuildingContext = metadataBuildingContext;
		this.propertyNamePrefix = propertyNamePrefix;

		propertyAccessedPersistentProperties = newHashMap();
		fieldAccessedPersistentProperties = newHashSet();
		propertiesGroupMapping = newHashMap();

		overriddenAuditedProperties = newHashSet();
		overriddenNotAuditedProperties = newHashSet();
		overriddenAuditedPropertiesJoinTables = newHashMap();

		overriddenAuditedClasses = newHashSet();
		overriddenNotAuditedClasses = newHashSet();
	}

	public void read() {
		read(null);
	}

	public void read(Audited allClassAudited) {
		// First reading the access types for the persistent properties.
		readPersistentPropertiesAccess();

		if ( persistentPropertiesSource.isDynamicComponent() ) {
			addPropertiesFromDynamicComponent( persistentPropertiesSource );
		}
		else {
			// Retrieve classes and properties that are explicitly marked for auditing process by any superclass
			// of currently mapped entity or itself.
			final ClassDetails classDetails = persistentPropertiesSource.getClassDetails();
			if ( persistentPropertiesSource.hasCompositeUserType() ) {
				for ( String propertyName : fieldAccessedPersistentProperties ) {
					final Property property = persistentPropertiesSource.getProperty( propertyName );
					final Value propertyValue = property.getValue();
					if ( propertyValue instanceof Component ) {
						this.addFromComponentProperty( property, "field", (Component) propertyValue, allClassAudited );
					}
					else {
						this.addFromNotComponentProperty( property, "field", allClassAudited );
					}
				}
				for ( String propertyName : propertyAccessedPersistentProperties.keySet() ) {
					final Property property = persistentPropertiesSource.getProperty( propertyName );
					final Value propertyValue = property.getValue();
					if ( propertyValue instanceof Component ) {
						this.addFromComponentProperty( property, "property", (Component) propertyValue, allClassAudited );
					}
					else {
						this.addFromNotComponentProperty( property, "property", allClassAudited );
					}
				}
			}
			else {
				readAuditOverrides( classDetails );
				// Adding all properties from the given class.
				addPropertiesFromClass( classDetails );
			}
		}
	}

	/**
	 * Recursively constructs sets of audited and not audited properties and classes which behavior has been overridden
	 * using {@link AuditOverride} annotation.
	 *
	 * @param classDetails Class that is being processed. Currently mapped entity shall be passed during first invocation.
	 */
	private void readAuditOverrides(ClassDetails classDetails) {
		/* TODO: Code to remove with @Audited.auditParents - start. */
		final Audited allClassAudited = classDetails.getDirectAnnotationUsage( Audited.class );
		if ( allClassAudited != null && allClassAudited.auditParents().length > 0 ) {
			for ( Class c : allClassAudited.auditParents() ) {
				final ClassDetails parentClass = metadataBuildingContext.getClassDetailsRegistry()
						.resolveClassDetails( c.getName() );
				checkSuperclass( classDetails, parentClass );
				if ( !overriddenNotAuditedClasses.contains( parentClass ) ) {
					// If the class has not been marked as not audited by the subclass.
					overriddenAuditedClasses.add( parentClass );
				}
			}
		}
		/* TODO: Code to remove with @Audited.auditParents - finish. */
		final List<AuditOverride> auditOverrides = computeAuditOverrides( classDetails );
		for ( AuditOverride auditOverride : auditOverrides ) {
			if ( auditOverride.forClass() != void.class ) {
				final ClassDetails overrideClass = metadataBuildingContext.getClassDetailsRegistry()
						.resolveClassDetails( auditOverride.forClass().getName() );
				checkSuperclass( classDetails, overrideClass );
				final String propertyName = auditOverride.name();
				if ( !StringTools.isEmpty( propertyName ) ) {
					// Override @Audited annotation on property level.
					final MemberDetails property = getProperty( overrideClass, propertyName );
					if ( auditOverride.isAudited() ) {
						if ( !overriddenNotAuditedProperties.contains( property ) ) {
							// If the property has not been marked as not audited by the subclass.
							overriddenAuditedProperties.add( property );
							overriddenAuditedPropertiesJoinTables.put( property, auditOverride.auditJoinTable() );
						}
					}
					else {
						if ( !overriddenAuditedProperties.contains( property ) ) {
							// If the property has not been marked as audited by the subclass.
							overriddenNotAuditedProperties.add( property );
						}
					}
				}
				else {
					// Override @Audited annotation on class level.
					if ( auditOverride.isAudited() ) {
						if ( !overriddenNotAuditedClasses.contains( overrideClass ) ) {
							// If the class has not been marked as not audited by the subclass.
							overriddenAuditedClasses.add( overrideClass );
						}
					}
					else {
						if ( !overriddenAuditedClasses.contains( overrideClass ) ) {
							// If the class has not been marked as audited by the subclass.
							overriddenNotAuditedClasses.add( overrideClass );
						}
					}
				}
			}
		}
		final ClassDetails superclass = classDetails.getSuperClass();
		if ( !classDetails.isInterface() && !Object.class.getName().equals( superclass.getName() ) ) {
			readAuditOverrides( superclass );
		}
	}

	/**
	 * @param classDetails Source class.
	 *
	 * @return List of @AuditOverride annotations applied at class level.
	 */
	private List<AuditOverride> computeAuditOverrides(ClassDetails classDetails) {
		final AuditOverrides auditOverrides = classDetails.getDirectAnnotationUsage( AuditOverrides.class );
		final AuditOverride auditOverride = classDetails.getDirectAnnotationUsage( AuditOverride.class );
		if ( auditOverrides == null && auditOverride != null ) {
			return Arrays.asList( auditOverride );
		}
		else if ( auditOverrides != null && auditOverride == null ) {
			return Arrays.asList( auditOverrides.value() );
		}
		else if ( auditOverrides != null && auditOverride != null ) {
			throw new EnversMappingException(
					"@AuditOverrides annotation should encapsulate all @AuditOverride declarations. " +
							"Please revise Envers annotations applied to class " + classDetails.getName() + "."
			);
		}
		return Collections.emptyList();
	}

	/**
	 * Checks whether one class is assignable from another. If not {@link EnversMappingException} is thrown.
	 *
	 * @param child Subclass.
	 * @param parent Superclass.
	 */
	private void checkSuperclass(ClassDetails child, ClassDetails parent) {
		if ( !child.isImplementor( parent.toJavaClass() ) ) {
			throw new EnversMappingException(
					"Class " + parent.getName() + " is not assignable from " + child.getName() + ". " +
							"Please revise Envers annotations applied to " + child.getName() + " type."
			);
		}
	}

	/**
	 * Checks whether class contains property with a given name. If not {@link EnversMappingException} is thrown.
	 *
	 * @param classDetails Class.
	 * @param propertyName Property name.
	 *
	 * @return Property object.
	 */
	private MemberDetails getProperty(ClassDetails classDetails, String propertyName) {
		final MemberDetails member = getMember( classDetails, propertyName );
		if ( member == null ) {
			throw new EnversMappingException(
					"Property '" + propertyName + "' not found in class " + classDetails.getName() + ". " +
							"Please revise Envers annotations applied to class " + persistentPropertiesSource.getClassDetails() + "."
			);
		}
		return member;
	}

	private void readPersistentPropertiesAccess() {
		final Iterator<Property> propertyIter = persistentPropertiesSource.getPropertyIterator();
		while ( propertyIter.hasNext() ) {
			final Property property = propertyIter.next();
			addPersistentProperty( property );
			// See HHH-6636
			if ( "embedded".equals( property.getPropertyAccessorName() ) && !NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( property.getName() ) ) {
				createPropertiesGroupMapping( property );
			}
		}
	}

	private void addPersistentProperty(Property property) {
		if ( "field".equals( property.getPropertyAccessorName() ) ) {
			fieldAccessedPersistentProperties.add( property.getName() );
		}
		else {
			propertyAccessedPersistentProperties.put( property.getName(), property.getPropertyAccessorName() );
		}
	}

	private void createPropertiesGroupMapping(Property property) {
		final Component component = (Component) property.getValue();
		for ( Property componentProperty : component.getProperties() ) {
			propertiesGroupMapping.put( componentProperty.getName(), property.getName() );
		}
	}

	/**
	 * @param classDetails Class which properties are currently being added.
	 *
	 * @return {@link Audited} annotation of specified class. If processed type hasn't been explicitly marked, method
	 *         checks whether given class exists in {@link AuditedPropertiesReader#overriddenAuditedClasses} collection.
	 *         In case of success, {@link Audited} configuration of currently mapped entity is returned, otherwise
	 *         {@code null}. If processed type exists in {@link AuditedPropertiesReader#overriddenNotAuditedClasses}
	 *         collection, the result is also {@code null}.
	 */
	private Audited computeAuditConfiguration(ClassDetails classDetails) {
		Audited allClassAudited = classDetails.getDirectAnnotationUsage( Audited.class );
		// If processed class is not explicitly marked with @Audited annotation, check whether auditing is
		// forced by any of its child entities configuration (@AuditedOverride.forClass).
		if ( allClassAudited == null && overriddenAuditedClasses.contains( classDetails ) ) {
			// Declared audited parent copies @Audited.modStore and @Audited.targetAuditMode configuration from
			// currently mapped entity.
			allClassAudited = persistentPropertiesSource.getClassDetails().getDirectAnnotationUsage( Audited.class );
			if ( allClassAudited == null ) {
				// If parent class declares @Audited on the field/property level.
				allClassAudited = DEFAULT_AUDITED;
			}
		}
		else if ( allClassAudited != null && overriddenNotAuditedClasses.contains( classDetails ) ) {
			return null;
		}
		return allClassAudited;
	}

	private void addPropertiesFromDynamicComponent(PersistentPropertiesSource propertiesSource) {
		Audited audited = computeAuditConfiguration( propertiesSource.getClassDetails() );
		if ( !fieldAccessedPersistentProperties.isEmpty() ) {
			throw new EnversMappingException(
					"Audited dynamic component cannot have properties with access=\"field\" for properties: " +
							fieldAccessedPersistentProperties +
							". \n Change properties access=\"property\", to make it work)"
			);
		}
		for ( Map.Entry<String, String> entry : propertyAccessedPersistentProperties.entrySet() ) {
			String property = entry.getKey();
			String accessType = entry.getValue();
			if ( !auditedPropertiesHolder.contains( property ) ) {
				final Value propertyValue = persistentPropertiesSource.getProperty( property ).getValue();
				final ModelsContext modelsContext = metadataBuildingContext.getModelsContext();
				final FieldDetails fieldDetails = dynamicFieldDetails( propertiesSource, property, modelsContext );
				if ( propertyValue instanceof Component ) {
					this.addFromComponentProperty(
							fieldDetails,
							accessType,
							(Component) propertyValue,
							audited
					);
				}
				else {
					this.addFromNotComponentProperty( fieldDetails, accessType, audited );
				}
			}
		}
	}

	/**
	 * Recursively adds all audited properties of entity class and its superclasses.
	 *
	 * @param classDetails Currently processed class.
	 */
	private void addPropertiesFromClass(ClassDetails classDetails) {
		final Audited allClassAudited = computeAuditConfiguration( classDetails );

		//look in the class
		classDetails.forEachField( (i, field) -> addFromProperty(
				field,
				it -> "field",
				fieldAccessedPersistentProperties,
				allClassAudited
		) );
		classDetails.forEachMethod( (i, method) -> addFromProperty(
				method,
				propertyAccessedPersistentProperties::get,
				propertyAccessedPersistentProperties.keySet(),
				allClassAudited
		) );

		if ( isClassHierarchyTraversalNeeded( allClassAudited ) ) {
			final ClassDetails superclass = classDetails.getSuperClass();
			if ( !classDetails.isInterface() && !"java.lang.Object".equals( superclass.getName() ) ) {
				addPropertiesFromClass( superclass );
			}
		}
	}

	protected boolean isClassHierarchyTraversalNeeded(Audited allClassAudited) {
		return allClassAudited != null || !auditedPropertiesHolder.isEmpty();
	}

	private void addFromProperty(
			MemberDetails memberDetails,
			Function<String, String> accessTypeProvider,
			Set<String> persistentProperties,
			Audited allClassAudited) {
		if ( !memberDetails.isPersistable() || memberDetails.hasDirectAnnotationUsage( Transient.class ) ) {
			return;
		}

		final String attributeName = memberDetails.resolveAttributeName();
		final String accessType = accessTypeProvider.apply( attributeName );

		// If this is not a persistent property, with the same access type as currently checked,
		// it's not audited as well.
		// If the property was already defined by the subclass, is ignored by superclasses
		if ( persistentProperties.contains( attributeName )
				&& !auditedPropertiesHolder.contains( attributeName ) ) {
			final Value propertyValue = persistentPropertiesSource.getProperty( attributeName ).getValue();
			if ( propertyValue instanceof Component ) {
				this.addFromComponentProperty( memberDetails, accessType, (Component) propertyValue, allClassAudited );
			}
			else {
				this.addFromNotComponentProperty( memberDetails, accessType, allClassAudited );
			}
		}
		else if ( propertiesGroupMapping.containsKey( attributeName ) ) {
			// Retrieve embedded component name based on class field.
			final String embeddedName = propertiesGroupMapping.get( attributeName );
			if ( !auditedPropertiesHolder.contains( embeddedName ) ) {
				// Manage properties mapped within <properties> tag.
				final Value propertyValue = persistentPropertiesSource.getProperty( embeddedName ).getValue();
				this.addFromPropertiesGroup(
						embeddedName,
						memberDetails,
						accessType,
						(Component) propertyValue,
						allClassAudited
				);
			}
		}
	}

	private void addFromPropertiesGroup(
			String embeddedName,
			MemberDetails memberDetails,
			String accessType,
			Component propertyValue,
			Audited allClassAudited) {
		final ComponentAuditingData componentData = new ComponentAuditingData();
		final boolean isAudited = fillPropertyData( memberDetails, componentData, accessType, allClassAudited );
		if ( isAudited ) {
			// EntityPersister.getPropertyNames() returns name of embedded component instead of class field.
			componentData.setName( embeddedName );
			// Marking component properties as placed directly in class (not inside another component).
			componentData.setBeanName( null );

			final PersistentPropertiesSource componentPropertiesSource = PersistentPropertiesSource.forComponent(
					metadataBuildingContext,
					propertyValue
			);
			final AuditedPropertiesReader audPropReader = new AuditedPropertiesReader(
					metadataBuildingContext,
					componentPropertiesSource,
					componentData,
					propertyNamePrefix + MappingTools.createComponentPrefix( embeddedName )
			);
			audPropReader.read();

			auditedPropertiesHolder.addPropertyAuditingData( embeddedName, componentData );
		}
	}

	private void addFromComponentProperty(
			MemberDetails memberDetails,
			String accessType,
			Component propertyValue,
			Audited allClassAudited) {
		final ComponentAuditingData componentData = new ComponentAuditingData();
		final boolean isAudited = fillPropertyData( memberDetails, componentData, accessType, allClassAudited );

		final PersistentPropertiesSource componentPropertiesSource;
		if ( propertyValue.isDynamic() ) {
			final ClassDetails mapClassDetails = metadataBuildingContext.getClassDetailsRegistry()
					.getClassDetails( Map.class.getName() );
			componentPropertiesSource = PersistentPropertiesSource.forComponent( propertyValue, mapClassDetails, true );
		}
		else {
			componentPropertiesSource = PersistentPropertiesSource.forComponent( metadataBuildingContext, propertyValue );
		}

		final ComponentAuditedPropertiesReader audPropReader = new ComponentAuditedPropertiesReader(
				metadataBuildingContext,
				componentPropertiesSource,
				componentData,
				propertyNamePrefix + MappingTools.createComponentPrefix( memberDetails.resolveAttributeName() )
		);
		audPropReader.read( allClassAudited );

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( memberDetails.resolveAttributeName(), componentData );
		}
	}

	private void addFromNotComponentProperty(MemberDetails memberDetails, String accessType, Audited allClassAudited) {
		final PropertyAuditingData propertyData = new PropertyAuditingData();
		final boolean isAudited = fillPropertyData( memberDetails, propertyData, accessType, allClassAudited );

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( memberDetails.resolveAttributeName(), propertyData );
		}
	}


	/**
	 * Checks if a property is audited and if yes, fills all of its data.
	 *
	 * @param memberDetails Property to check.
	 * @param propertyData Property data, on which to set this property's modification store.
	 * @param accessType Access type for the property.
	 *
	 * @return False if this property is not audited.
	 */
	private boolean fillPropertyData(
			MemberDetails memberDetails,
			PropertyAuditingData propertyData,
			String accessType,
			Audited allClassAudited) {

		// check if a property is declared as not audited to exclude it
		// useful if a class is audited but some properties should be excluded
		final NotAudited unVer = memberDetails.getDirectAnnotationUsage( NotAudited.class );
		if ( ( unVer != null
				&& !overriddenAuditedProperties.contains( memberDetails ) )
				|| overriddenNotAuditedProperties.contains( memberDetails ) ) {
			return false;
		}
		else {
			// if the optimistic locking field has to be unversioned and the current property
			// is the optimistic locking field, don't audit it
			if ( metadataBuildingContext.getConfiguration().isDoNotAuditOptimisticLockingField() ) {
				final Version jpaVer = memberDetails.getDirectAnnotationUsage( Version.class );
				if ( jpaVer != null ) {
					return false;
				}
			}
		}

		final String propertyName = propertyNamePrefix + memberDetails.resolveAttributeName();
		final String modifiedFlagsSuffix = metadataBuildingContext.getConfiguration().getModifiedFlagsSuffix();
		if ( !this.checkAudited( memberDetails, propertyData,propertyName, allClassAudited, modifiedFlagsSuffix ) ) {
			return false;
		}

		validateLobMappingSupport( memberDetails );

		propertyData.setName( propertyName );
		propertyData.setBeanName( memberDetails.resolveAttributeName() );
		propertyData.setAccessType( accessType );

		addPropertyJoinTables( memberDetails, propertyData );
		addPropertyCollectionAuditTable( memberDetails, propertyData );
		addPropertyAuditingOverrides( memberDetails, propertyData );
		if ( !processPropertyAuditingOverrides( memberDetails, propertyData ) ) {
			// not audited due to AuditOverride annotation
			return false;
		}
		addPropertyMapKey( memberDetails, propertyData );
		setPropertyAuditMappedBy( memberDetails, propertyData );
		setPropertyRelationMappedBy( memberDetails, propertyData );

		return true;
	}

	private void addFromComponentProperty(
			Property property,
			String accessType,
			Component propertyValue,
			Audited allClassAudited) {
		final ComponentAuditingData componentData = new ComponentAuditingData();
		final boolean isAudited = fillPropertyData( property, componentData, accessType, allClassAudited );

		final PersistentPropertiesSource componentPropertiesSource;
		if ( propertyValue.isDynamic() ) {
			final ClassDetails mapClassDetails = metadataBuildingContext.getClassDetailsRegistry()
					.getClassDetails( Map.class.getName() );
			componentPropertiesSource = PersistentPropertiesSource.forComponent( propertyValue, mapClassDetails, true );
		}
		else {
			componentPropertiesSource = PersistentPropertiesSource.forComponent( metadataBuildingContext, propertyValue );
		}

		final ComponentAuditedPropertiesReader audPropReader = new ComponentAuditedPropertiesReader(
				metadataBuildingContext,
				componentPropertiesSource,
				componentData,
				propertyNamePrefix + MappingTools.createComponentPrefix( property.getName() )
		);
		audPropReader.read( allClassAudited );

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( property.getName(), componentData );
		}
	}

	private void addFromNotComponentProperty(Property property, String accessType, Audited allClassAudited) {
		final PropertyAuditingData propertyData = new PropertyAuditingData();
		final boolean isAudited = fillPropertyData( property, propertyData, accessType, allClassAudited );

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( property.getName(), propertyData );
		}
	}


	/**
	 * Checks if a property is audited and if yes, fills all of its data.
	 *
	 * @param property Property to check.
	 * @param propertyData Property data, on which to set this property's modification store.
	 * @param accessType Access type for the property.
	 *
	 * @return False if this property is not audited.
	 */
	private boolean fillPropertyData(
			Property property,
			PropertyAuditingData propertyData,
			String accessType,
			Audited allClassAudited) {

		final String propertyName = propertyNamePrefix + property.getName();
		final String modifiedFlagsSuffix = metadataBuildingContext.getConfiguration().getModifiedFlagsSuffix();
		if ( !this.checkAudited( property, propertyData,propertyName, allClassAudited, modifiedFlagsSuffix ) ) {
			return false;
		}

		propertyData.setName( propertyName );
		propertyData.setBeanName( property.getName() );
		propertyData.setAccessType( accessType );

		propertyData.setJoinTable( DEFAULT_AUDIT_JOIN_TABLE );
		if ( !processPropertyAuditingOverrides( property, propertyData ) ) {
			// not audited due to AuditOverride annotation
			return false;
		}

		return true;
	}

	private void validateLobMappingSupport(MemberDetails memberDetails) {
		// HHH-9834 - Sanity check
		try {
			if ( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ) {
				if ( memberDetails.hasDirectAnnotationUsage( Lob.class ) ) {
					if ( !memberDetails.getType().isImplementor( Map.class ) ) {
						throw new EnversMappingException(
								"@ElementCollection combined with @Lob is only supported for Map collection types."
						);
					}
				}
			}
		}
		catch ( EnversMappingException e ) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Invalid mapping in [%s] for property [%s]",
							memberDetails.getDeclaringType().getName(),
							memberDetails.resolveAttributeName()
					),
					e
			);
		}
	}

	protected boolean checkAudited(
			MemberDetails memberDetails,
			PropertyAuditingData propertyData, String propertyName,
			Audited allClassAudited, String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		Audited aud = ( memberDetails.hasDirectAnnotationUsage( Audited.class ) )
				? memberDetails.getDirectAnnotationUsage( Audited.class )
				: allClassAudited;
		if ( aud == null
				&& overriddenAuditedProperties.contains( memberDetails )
				&& !overriddenNotAuditedProperties.contains( memberDetails ) ) {
			// Assigning @Audited defaults. If anyone needs to customize those values in the future,
			// appropriate fields shall be added to @AuditOverride annotation.
			aud = DEFAULT_AUDITED;
		}
		if ( aud != null ) {
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setRelationTargetNotFoundAction( getRelationNotFoundAction( memberDetails, allClassAudited ) );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			propertyData.setModifiedFlagName( ModifiedColumnNameResolver.getName( propertyName, modifiedFlagSuffix ) );
			if ( !StringTools.isEmpty( aud.modifiedColumnName() ) ) {
				propertyData.setExplicitModifiedFlagName( aud.modifiedColumnName() );
			}
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean checkAudited(
			Property property,
			PropertyAuditingData propertyData, String propertyName,
			Audited allClassAudited, String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		if ( allClassAudited != null ) {
			propertyData.setRelationTargetAuditMode( allClassAudited.targetAuditMode() );
			propertyData.setRelationTargetNotFoundAction(
					allClassAudited == null ?
							RelationTargetNotFoundAction.DEFAULT :
							allClassAudited.targetNotFoundAction()
			);
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( allClassAudited ) );
			propertyData.setModifiedFlagName( ModifiedColumnNameResolver.getName( propertyName, modifiedFlagSuffix ) );
			if ( !StringTools.isEmpty( allClassAudited.modifiedColumnName() ) ) {
				propertyData.setExplicitModifiedFlagName( allClassAudited.modifiedColumnName() );
			}
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean checkUsingModifiedFlag(Audited aud) {
		// HHH-10468
		if ( metadataBuildingContext.getConfiguration().hasSettingForUseModifiedFlag() ) {
			// HHH-10468
			// Modify behavior so that if the global setting has been set by user properties, then
			// the audit behavior should be a disjunction between the global setting and the field
			// annotation.  This allows the annotation to take precedence when the global value is
			// false and for the global setting to take precedence when true.
			return metadataBuildingContext.getConfiguration().isModifiedFlagsEnabled() || aud.withModifiedFlag();
		}
		// no global setting enabled, use the annotation's value only.
		return aud.withModifiedFlag();
	}

	private void setPropertyRelationMappedBy(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		final OneToMany oneToMany = memberDetails.getDirectAnnotationUsage( OneToMany.class );
		if ( oneToMany != null && StringHelper.isNotEmpty( oneToMany.mappedBy() ) ) {
			propertyData.setRelationMappedBy( oneToMany.mappedBy() );
		}
	}

	private void setPropertyAuditMappedBy(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		final AuditMappedBy auditMappedBy = memberDetails.getDirectAnnotationUsage( AuditMappedBy.class );
		if ( auditMappedBy != null ) {
			propertyData.setAuditMappedBy( auditMappedBy.mappedBy() );
			if ( StringHelper.isNotEmpty( auditMappedBy.positionMappedBy() ) ) {
				propertyData.setPositionMappedBy( auditMappedBy.positionMappedBy() );
			}
		}
	}

	private void addPropertyMapKey(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		final MapKey mapKey = memberDetails.getDirectAnnotationUsage( MapKey.class );
		if ( mapKey != null ) {
			propertyData.setMapKey( mapKey.name() );
		}
		else {
			final MapKeyEnumerated mapKeyEnumerated = memberDetails.getDirectAnnotationUsage( MapKeyEnumerated.class );
			if ( mapKeyEnumerated != null ) {
				propertyData.setMapKeyEnumType( mapKeyEnumerated.value() );
			}
		}
	}

	private void addPropertyJoinTables(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		// The AuditJoinTable annotation source will follow the following priority rules
		//		1. Use the override if one is specified
		//		2. Use the site annotation if one is specified
		//		3. Use the default if neither are specified
		//
		// The prime directive for (1) is so that when users in a subclass use @AuditOverride(s)
		// the join-table specified there should have a higher priority in the event the
		// super-class defines an equivalent @AuditJoinTable at the site/property level.

		final AuditJoinTable overrideJoinTable = overriddenAuditedPropertiesJoinTables.get( memberDetails );
		if ( overrideJoinTable != null ) {
			propertyData.setJoinTable( new AuditJoinTableData( overrideJoinTable ) );
		}
		else {
			final AuditJoinTable propertyJoinTable = memberDetails.getDirectAnnotationUsage( AuditJoinTable.class );
			if ( propertyJoinTable != null ) {
				propertyData.setJoinTable( new AuditJoinTableData( propertyJoinTable ) );
			}
			else {
				propertyData.setJoinTable( DEFAULT_AUDIT_JOIN_TABLE );
			}
		}
	}

	private void addPropertyCollectionAuditTable(MemberDetails memberDetails, PropertyAuditingData propertyAuditingData) {
		final CollectionAuditTable collectionAuditTableAnn = memberDetails.getDirectAnnotationUsage( CollectionAuditTable.class );
		if ( collectionAuditTableAnn != null ) {
			propertyAuditingData.setCollectionAuditTable( collectionAuditTableAnn );
		}
	}

	/**
	 * Add the {@link AuditOverride} annotations.
	 *
	 * @param memberDetails the property being processed
	 * @param propertyData the Envers auditing data for this property
	 */
	private void addPropertyAuditingOverrides(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		final AuditOverride annotationOverride = memberDetails.getDirectAnnotationUsage( AuditOverride.class );
		if ( annotationOverride != null ) {
			propertyData.addAuditingOverride( annotationOverride );
		}
		final AuditOverrides annotationOverrides = memberDetails.getDirectAnnotationUsage( AuditOverrides.class );
		if ( annotationOverrides != null ) {
			propertyData.addAuditingOverrides( annotationOverrides );
		}
	}

	/**
	 * Process the {@link AuditOverride} annotations for this property.
	 *
	 * @param memberDetails the property for which the {@link AuditOverride}
	 * annotations are being processed
	 * @param propertyData the Envers auditing data for this property
	 *
	 * @return {@code false} if isAudited() of the override annotation was set to
	 */
	private boolean processPropertyAuditingOverrides(MemberDetails memberDetails, PropertyAuditingData propertyData) {
		// Only components register audit overrides, classes will have no entries.
		for ( AuditOverrideData override : auditedPropertiesHolder.getAuditingOverrides() ) {
			if ( memberDetails.resolveAttributeName().equals( override.getName() ) ) {
				// the override applies to this property
				if ( !override.isAudited() ) {
					return false;
				}
				else {
					if ( override.getAuditJoinTableData() != null ) {
						propertyData.setJoinTable( override.getAuditJoinTableData() );
					}
				}
			}
		}
		return true;
	}

	private boolean processPropertyAuditingOverrides(Property property, PropertyAuditingData propertyData) {
		// Only components register audit overrides, classes will have no entries.
		for ( AuditOverrideData override : auditedPropertiesHolder.getAuditingOverrides() ) {
			if ( property.getName().equals( override.getName() ) ) {
				// the override applies to this property
				if ( !override.isAudited() ) {
					return false;
				}
				else {
					if ( override.getAuditJoinTableData() != null ) {
						propertyData.setJoinTable( override.getAuditJoinTableData() );
					}
				}
			}
		}
		return true;
	}

	protected boolean isOverriddenNotAudited(MemberDetails memberDetails) {
		return overriddenNotAuditedProperties.contains( memberDetails );
	}

	protected boolean isOverriddenNotAudited(ClassDetails classDetails) {
		return overriddenNotAuditedClasses.contains( classDetails );
	}

	protected boolean isOverriddenAudited(MemberDetails memberDetails) {
		return overriddenAuditedProperties.contains( memberDetails );
	}

	protected boolean isOverriddenAudited(ClassDetails classDetails) {
		return overriddenAuditedClasses.contains( classDetails );
	}

	private RelationTargetNotFoundAction getRelationNotFoundAction(MemberDetails memberDetails, Audited classAudited) {
		final Audited propertyAudited = memberDetails.getDirectAnnotationUsage( Audited.class );

		// class isn't annotated, check property
		if ( classAudited == null ) {
			if ( propertyAudited == null ) {
				// both class and property are not annotated, use default behavior
				return RelationTargetNotFoundAction.DEFAULT;
			}
			// Property is annotated use its value
			return propertyAudited.targetNotFoundAction();
		}

		// if class is annotated, take its value by default
		RelationTargetNotFoundAction action = classAudited.targetNotFoundAction();
		if ( propertyAudited != null ) {
			// both places have audited, use the property value only if it is not DEFAULT
			if ( !propertyAudited.targetNotFoundAction().equals( RelationTargetNotFoundAction.DEFAULT ) ) {
				action = propertyAudited.targetNotFoundAction();
			}
		}

		return action;
	}

	private static final Audited DEFAULT_AUDITED = new Audited() {
		@Override
		public RelationTargetAuditMode targetAuditMode() {
			return RelationTargetAuditMode.AUDITED;
		}

		@Override
		public RelationTargetNotFoundAction targetNotFoundAction() {
			return RelationTargetNotFoundAction.DEFAULT;
		}

		@Override
		public Class[] auditParents() {
			return new Class[0];
		}

		@Override
		public boolean withModifiedFlag() {
			return false;
		}

		@Override
		public String modifiedColumnName() {
			return "";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return this.getClass();
		}
	};
}
