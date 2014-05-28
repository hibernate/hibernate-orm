/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.JoinColumn;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.event.spi.EnversDotNames;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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
 */
public class AuditedPropertiesReader {
	private final AuditConfiguration.AuditConfigurationContext context;
	// TODO: is AttributeBindingContainer actually needed or is Hierarchical sufficient?
	private final PersistentPropertiesSource processedPersistentPropertiesSource;
	private final ClassInfo processedClassInfo;
	private final AuditedPropertiesHolder auditedPropertiesHolder;
	private final String propertyNamePrefix;

	private final Set<String> propertyAccessedPersistentProperties;
	private final Set<String> fieldAccessedPersistentProperties;
	// Mapping class field to corresponding <properties> element.
	private final Map<String, String> propertiesGroupMapping;

	private final Set<Attribute> overriddenAuditedProperties;
	private final Set<Attribute> overriddenNotAuditedProperties;

	private final Set<Hierarchical> overriddenAuditedClasses;
	private final Set<Hierarchical> overriddenNotAuditedClasses;

	public AuditedPropertiesReader(
			AuditConfiguration.AuditConfigurationContext context,
			AuditedPropertiesHolder auditedPropertiesHolder,
			PersistentPropertiesSource processedPersistentPropertiesSource,
			String propertyNamePrefix) {
		this.context = context;
		this.auditedPropertiesHolder = auditedPropertiesHolder;
		this.processedPersistentPropertiesSource = processedPersistentPropertiesSource;
		this.processedClassInfo = context.getClassInfo(
				processedPersistentPropertiesSource.getAttributeBindingContainer().getAttributeContainer()
		);
		this.propertyNamePrefix = propertyNamePrefix;

		propertyAccessedPersistentProperties = newHashSet();
		fieldAccessedPersistentProperties = newHashSet();
		propertiesGroupMapping = newHashMap();

		overriddenAuditedProperties = newHashSet();
		overriddenNotAuditedProperties = newHashSet();

		overriddenAuditedClasses = newHashSet();
		overriddenNotAuditedClasses = newHashSet();
	}

	public void read() {
		// First reading the access types for the persistent properties.
		readPersistentPropertiesAccess();

		if ( processedPersistentPropertiesSource instanceof DynamicComponentSource ) {
			addPropertiesFromDynamicComponent( (DynamicComponentSource) processedPersistentPropertiesSource );
		}
		else {
			// Retrieve classes and properties that are explicitly marked for auditing process by any superclass
			// of currently mapped entity or itself.
			readAuditOverrides(
					processedClassInfo,
					(Hierarchical) processedPersistentPropertiesSource.getAttributeBindingContainer().getAttributeContainer()
			);

			// Adding all properties from the given class.
			addPropertiesFromClass( processedPersistentPropertiesSource.getAttributeBindingContainer() );
		}
	}

	protected AuditConfiguration.AuditConfigurationContext getContext() {
		return context;
	}

	/**
	 * Recursively constructs sets of audited and not audited properties and classes which behavior has been overridden
	 * using {@link AuditOverride} annotation.
	 *
	 * @param classInfo Class that is being processed. Currently mapped entity shall be passed during first invocation.
	 */
	private void readAuditOverrides(ClassInfo classInfo, Hierarchical hierarchical) {
		final ClassLoaderService classLoaderService = context.getClassLoaderService() ;
		// TODO: Remove auditParents.
		final List<AnnotationInstance> auditOverrides = computeAuditOverrides( classInfo );
		for (AnnotationInstance auditOverride : auditOverrides) {
			final String overrideClassName = JandexHelper.getValue(
					auditOverride, "forClass", String.class, classLoaderService
			);
			final boolean isAudited = JandexHelper.getValue(
					auditOverride, "isAudited", boolean.class, classLoaderService
			);
			if ( !void.class.getName().equals( overrideClassName )) {
				final ClassInfo overrideClassInfo = context.getClassInfo( overrideClassName );
				checkSuperclass( classInfo, overrideClassInfo );
				final Hierarchical overrideHierarchical = getSuperHierarchical( hierarchical, overrideClassName );
				final String propertyName = JandexHelper.getValue( auditOverride, "name", String.class, classLoaderService );
				if ( !StringTools.isEmpty( propertyName ) ) {
					// Override @Audited annotation on property level.
					final Attribute overrideAttribute = getAttribute( overrideHierarchical, propertyName );
					if ( isAudited ) {
						if ( !overriddenNotAuditedProperties.contains( overrideAttribute ) ) {
							// If the property has not been marked as not audited by the subclass.
							overriddenAuditedProperties.add( overrideAttribute );
						}
					}
					else {
						if ( !overriddenAuditedProperties.contains( overrideAttribute ) ) {
							// If the property has not been marked as audited by the subclass.
							overriddenNotAuditedProperties.add( overrideAttribute );
						}
					}
				} else {
					// Override @Audited annotation on class level.
					if ( isAudited ) {
						if (!overriddenNotAuditedClasses.contains( overrideHierarchical )) {
							// If the class has not been marked as not audited by the subclass.
							overriddenAuditedClasses.add( overrideHierarchical );
						}
					} else {
						if (!overriddenAuditedClasses.contains( overrideHierarchical )) {
							// If the class has not been marked as audited by the subclass.
							overriddenNotAuditedClasses.add( overrideHierarchical );
						}
					}
				}
			}
		}
		// TODO: how to distinguish interface: !clazz.isInterface() &&
		// if (!clazz.isInterface() && !Object.class.getName().equals(superclass.getName())) {
		if ( !Object.class.getName().equals( classInfo.superName().toString() ) ) {
			readAuditOverrides( context.getClassInfo( classInfo.superName() ), hierarchical.getSuperType() );
		}
	}

	private Hierarchical getSuperHierarchical(Hierarchical hierarchical, String superclassName) {
		Hierarchical superHierarchical = hierarchical.getSuperType();
		while ( superHierarchical != null ) {
			if ( superclassName.equals( superHierarchical.getDescriptor().getName().toString() ) ) {
				return superHierarchical;
			}
			superHierarchical = superHierarchical.getSuperType();
		}
		throw new IllegalStateException(
				String.format( "Hierarchical [%s] does not have a superclass with name [%s] ",
						hierarchical.getRoleBaseName(),
						superclassName
				)
		);
	}

	/**
	 * @param classInfo Source {@link ClassInfo}.
	 *
	 * @return List of @AuditOverride annotations applied at class level.
	 */
	private List<AnnotationInstance> computeAuditOverrides(ClassInfo classInfo) {
		AnnotationInstance auditOverrides = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.AUDIT_OVERRIDES );
		AnnotationInstance auditOverride = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.AUDIT_OVERRIDE );
		if ( auditOverrides == null && auditOverride != null ) {
			return Arrays.asList( auditOverride );
		}
		else if ( auditOverrides != null && auditOverride == null ) {
			return Arrays.asList(
					JandexHelper.getValue( auditOverrides, "value", AnnotationInstance[].class, context.getClassLoaderService() )
			);
		}
		else if ( auditOverrides != null && auditOverride != null ) {
			throw new MappingException(
					"@AuditOverrides annotation should encapsulate all @AuditOverride declarations. " +
							"Please revise Envers annotations applied to class " + classInfo.name() + "."
			);
		}
		return Collections.emptyList();
	}

	/**
	 * Checks whether one class is assignable from another. If not {@link MappingException} is thrown.
	 * @param child Subclass.
	 * @param parent Superclass.
	 */
	private void checkSuperclass(ClassInfo child, ClassInfo parent) {
		if ( !context.getJandexIndex().getAllKnownSubclasses( parent.name() ).contains( child ) ) {
			throw new MappingException(
					"Class " + parent.name() + " is not assignable from " + child.name() + ". " +
							"Please revise Envers annotations applied to " + child.name() + " type."
			);
		}
	}

	/**
	 * Checks whether class contains property with a given name. If not {@link MappingException} is thrown.
	 *
	 * @param hierarchical The Hierarchical domain object..
	 * @param propertyName Property name.
	 * @return the attribute binding.
	 */
	private Attribute getAttribute(Hierarchical hierarchical, String propertyName) {
		final Attribute attribute = hierarchical.locateAttribute( propertyName );
		if ( attribute == null ) {
			throw new MappingException(
					"Property '" + propertyName + "' not found in class " + hierarchical.getDescriptor().getName().toString() + ". " +
							"Please revise Envers annotations applied to class " + processedClassInfo.name() + "."
			);
		}
		return attribute;
	}

	private void readPersistentPropertiesAccess() {
		for ( AttributeBinding attributeBinding : processedPersistentPropertiesSource.getNonIdAttributeBindings() ) {
			addPersistentAttribute( attributeBinding );
//			TODO: if ("embedded".equals(property.getPropertyAccessorName()) && property.getName().equals(property.getNodeName())) {
//				// If property name equals node name and embedded accessor type is used, processing component
//				// has been defined with <properties> tag. See HHH-6636 JIRA issue.
//				createPropertiesGroupMapping(property);
//			}
		}
	}

	private void addPersistentAttribute(AttributeBinding attributeBinding) {
		if ( "field".equals( attributeBinding.getPropertyAccessorName() ) ) {
			fieldAccessedPersistentProperties.add( attributeBinding.getAttribute().getName() );
		}
		else {
			propertyAccessedPersistentProperties.add( attributeBinding.getAttribute().getName() );
		}
	}

	// TODO: add support for <properties/>
	//@SuppressWarnings("unchecked")
	//private void createPropertiesGroupMapping(Property property) {
	//	final Component component = (Component) property.getValue();
	//	final Iterator<Property> componentProperties = component.getPropertyIterator();
	//	while ( componentProperties.hasNext() ) {
	//		final Property componentProperty = componentProperties.next();
	//		propertiesGroupMapping.put( componentProperty.getName(), component.getNodeName() );
	//	}
	//}

	/**
	 * @param attributeBindingContainer Class which properties are currently being added.
	 * @return {@link Audited} annotation of specified class. If processed type hasn't been explicitly marked, method
	 *         checks whether given class exists in {@link AuditedPropertiesReader#overriddenAuditedClasses} collection.
	 *         In case of success, {@link Audited} configuration of currently mapped entity is returned, otherwise
	 *         {@code null}. If processed type exists in {@link AuditedPropertiesReader#overriddenNotAuditedClasses}
	 *         collection, the result is also {@code null}.
	 */
	private Audited computeAuditConfiguration(AttributeBindingContainer attributeBindingContainer) {
		final ClassInfo classInfo = context.getClassInfo( attributeBindingContainer.getAttributeContainer() );
		// TODO: Check this method if I have migrated everything correctly.
		final AnnotationInstance allClassAudited = JandexHelper.getSingleAnnotation(
				classInfo.annotations(),
				EnversDotNames.AUDITED,
				classInfo
		);
		// If processed class is not explicitly marked with @Audited annotation, check whether auditing is
		// forced by any of its child entities configuration (@AuditedOverride.forClass).
		final Hierarchical hierarchical = (Hierarchical) attributeBindingContainer.getAttributeContainer();
		if ( allClassAudited == null && overriddenAuditedClasses.contains( hierarchical  ) ) {
			// Declared audited parent copies @Audited.modStore and @Audited.targetAuditMode configuration from
			// currently mapped entity.
			if ( JandexHelper.getSingleAnnotation( processedClassInfo, EnversDotNames.AUDITED ) == null ) {
				// If parent class declares @Audited on the field/property level.
				return DEFAULT_AUDITED;
			}
		}
		else if ( allClassAudited != null && overriddenNotAuditedClasses.contains( hierarchical ) ) {
			return null;
		}

		return allClassAudited == null ? null : context.getAnnotationProxy( allClassAudited, Audited.class );
	}

	private void addPropertiesFromDynamicComponent(DynamicComponentSource dynamicComponentSource) {
		Audited audited = computeAuditConfiguration( dynamicComponentSource.getAttributeBindingContainer() );
		if ( !fieldAccessedPersistentProperties.isEmpty() ) {
			throw new MappingException(
					"Audited dynamic component cannot have properties with access=\"field\" for properties: " + fieldAccessedPersistentProperties + ". \n Change properties access=\"property\", to make it work)"
			);
		}
		for ( String property : propertyAccessedPersistentProperties ) {
			String accessType = AccessType.PROPERTY.getType();
			if ( !auditedPropertiesHolder.contains( property ) ) {
				final AttributeBinding attributeBinding = processedPersistentPropertiesSource.getAttributeBinding( property );
				if ( EmbeddedAttributeBinding.class.isInstance( attributeBinding ) ) {
					this.addFromComponentProperty(
							(EmbeddedAttributeBinding) attributeBinding,
							accessType,
							audited
					);
				}
				else {
					this.addFromNotComponentProperty(
							attributeBinding,
							accessType,
							audited
					);
				}
			}
		}
	}

	private Collection<AttributeBinding> getAttributeBindings(
			AttributeBindingContainer attributeBindingContainer,
			String propertyAccessorName) {
		// TODO: why a LinkedList?
		final List<AttributeBinding> attributes = new LinkedList<AttributeBinding>();
		for ( AttributeBinding attributeBinding : attributeBindingContainer.attributeBindings() ) {
			if ( propertyAccessorName.equals( attributeBinding.getPropertyAccessorName() ) ) {
				attributes.add( attributeBinding );
			}
		}
		return attributes;
	}

	/**
	 * Recursively adds all audited properties of entity class and its superclasses.
	 *
	 * @param attributeBindingContainer Currently processed attribute binding container..
	 */
	private void addPropertiesFromClass(AttributeBindingContainer attributeBindingContainer)  {
		final ClassInfo classInfo = context.getClassInfo( attributeBindingContainer.getAttributeContainer() );

		final Audited allClassAudited = computeAuditConfiguration( attributeBindingContainer );

		//look in the class
		addFromProperties( getAttributeBindings( attributeBindingContainer, "field" ), "field", fieldAccessedPersistentProperties, allClassAudited );
		addFromProperties( getAttributeBindings( attributeBindingContainer, "property" ), "property", propertyAccessedPersistentProperties, allClassAudited );

		if ( allClassAudited != null || !auditedPropertiesHolder.isEmpty() ) {
			if ( Hierarchical.class.isInstance( attributeBindingContainer.getAttributeContainer() ) ) {
				// attributeBindingContainer *should* always be an Hierarchical
				final Hierarchical hierarchical = (Hierarchical) attributeBindingContainer.getAttributeContainer();
				final ClassDescriptor classDescriptor = (ClassDescriptor) hierarchical.getDescriptor();
				if (  !Object.class.getName().equals( classDescriptor.getSuperType().getName().toString() ) && hierarchical.getSuperType() == null ) {
					throw new NotYetImplementedException(
							String.format(
									"Inconsistency for [%s]; Hierarchical.getSuperType() is null but classDescriptor.getSuperType() is [%s]",
									attributeBindingContainer.getRoleBase().getFullPath(),
									classDescriptor.getSuperType().getName().toString()
							)
					);
				}
				if ( MappedSuperclass.class.isInstance( hierarchical.getSuperType() ) ) {
					throw new NotYetImplementedException( "@MappedSuperclass not supported with new metamodel by envers yet. " );
				}
				else if ( EntityBinding.class.isInstance( attributeBindingContainer ) ) {
					final EntityBinding entityBinding = (EntityBinding) attributeBindingContainer;
					if ( entityBinding.getSuperEntityBinding() != null ) {
						addPropertiesFromClass( entityBinding.getSuperEntityBinding() );
					}
				}
			}
		}
	}

	private void addFromProperties(
			Iterable<AttributeBinding> attributes,
			String accessType,
			Set<String> persistentProperties,
			Audited allClassAudited) {
		for ( AttributeBinding attributeBinding : attributes ) {
				// If this is not a persistent property, with the same access type as currently checked,
				// it's not audited as well.
				// If the property was already defined by the subclass, is ignored by superclasses
				final String attributeName = attributeBinding.getAttribute().getName();
				if ( persistentProperties.contains( attributeName ) && !auditedPropertiesHolder.contains( attributeName ) ) {
					if ( attributeBinding instanceof EmbeddedAttributeBinding ) {
						this.addFromComponentProperty(
								(EmbeddedAttributeBinding) attributeBinding, accessType, allClassAudited
						);
					}
					else {
						// TODO: is "this." needed?
						this.addFromNotComponentProperty( attributeBinding, accessType, allClassAudited );
					}
				}
//			TODO: else if ( propertiesGroupMapping.containsKey( attributeName ) ) {
//				// Retrieve embedded component name based on class field.
//				final String embeddedName = propertiesGroupMapping.get(attributeName);
//				if (!auditedPropertiesHolder.contains(embeddedName)) {
//					// Manage properties mapped within <properties> tag.
//					Value propertyValue = persistentPropertiesSource.getProperty(embeddedName).getValue();
//					this.addFromPropertiesGroup( embeddedName, property, accessType, (Component) propertyValue, allClassAudited );
//				}
//			}
		}
	}

// TODO:	private void addFromPropertiesGroup(
//			String embeddedName,
//			XProperty property,
//			String accessType,
//			Component propertyValue,
//			Audited allClassAudited) {
//		final ComponentAuditingData componentData = new ComponentAuditingData();
//		final boolean isAudited = fillPropertyData( property, componentData, accessType, allClassAudited );
//		if ( isAudited ) {
//			// EntityPersister.getPropertyNames() returns name of embedded component instead of class field.
//			componentData.setName( embeddedName );
//			// Marking component properties as placed directly in class (not inside another component).
//			componentData.setBeanName( null );
//
//			final PersistentPropertiesSource componentPropertiesSource = new ComponentPropertiesSource(
//					reflectionManager,
//					propertyValue
//			);
//			final AuditedPropertiesReader audPropReader = new AuditedPropertiesReader(
//					componentPropertiesSource, componentData, globalCfg, reflectionManager,
//					propertyNamePrefix + MappingTools.createComponentPrefix( embeddedName )
//			);
//			audPropReader.read();
//
//			auditedPropertiesHolder.addPropertyAuditingData( embeddedName, componentData );
//		}
//	}

	private void addFromComponentProperty(
			EmbeddedAttributeBinding attributeBinding,
			String accessType,
			Audited allClassAudited) {
		final ComponentAuditingData componentData = new ComponentAuditingData();
		final boolean isAudited = fillPropertyData( attributeBinding.getAttribute(), componentData, accessType, allClassAudited );

		final PersistentPropertiesSource componentPropertiesSource;
		// TODO:
		//if ( attributeBinding.isDynamic() ) {
		//	componentPropertiesSource = new DynamicComponentSource( attributeBinding );
		//}
		//else {
			componentPropertiesSource = new ComponentPropertiesSource(
					context.getClassInfo( attributeBinding.getEmbeddableBinding().getAttributeContainer() ),
					attributeBinding.getEmbeddableBinding()
			);
		//}

		final ComponentAuditedPropertiesReader audPropReader = new ComponentAuditedPropertiesReader(
				context,
				componentData,
				componentPropertiesSource,
				propertyNamePrefix + MappingTools.createComponentPrefix( attributeBinding.getAttribute().getName() )
		);
		audPropReader.read();

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( attributeBinding.getAttribute().getName(), componentData );
		}
	}

	private void addFromNotComponentProperty(AttributeBinding attributeBinding, String accessType, Audited allClassAudited) {
		PropertyAuditingData propertyData = new PropertyAuditingData();
		boolean isAudited = fillPropertyData( attributeBinding.getAttribute(), propertyData, accessType, allClassAudited );

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( attributeBinding.getAttribute().getName(), propertyData );
		}
	}

	/**
	 * Checks if a property is audited and if yes, fills all of its data.
	 * @param attribute Property to check.
	 * @param propertyData Property data, on which to set this property's modification store.
	 * @param accessType Access type for the property.
	 * @return False if this property is not audited.
	 */
	private boolean fillPropertyData(
			Attribute attribute,
			PropertyAuditingData propertyData,
			String accessType,
			Audited allClassAudited) {
		// check if a property is declared as not audited to exclude it
		// useful if a class is audited but some properties should be excluded
		Map<DotName, List<AnnotationInstance>> attributeAnnotations = context.locateAttributeAnnotations(
				attribute
		);
		if ( ( attributeAnnotations.containsKey( EnversDotNames.NOT_AUDITED ) && !overriddenAuditedProperties.contains( attribute ) )
				|| overriddenNotAuditedProperties.contains( attribute ) ) {
			return false;
		}
		else {
			// if the optimistic locking field has to be unversioned and the current property
			// is the optimistic locking field, don't audit it
			if ( context.getGlobalConfiguration().isDoNotAuditOptimisticLockingField() ) {
				if ( attributeAnnotations.containsKey( JPADotNames.VERSION ) ) {
					return false;
				}
			}
		}

		// TODO: is "this." needed?
		if ( !this.checkAudited( attribute, propertyData, allClassAudited ) ) {
			return false;
		}

		String propertyName = propertyNamePrefix + attribute.getName();
		propertyData.setName( propertyName );
		propertyData.setModifiedFlagName(
				MetadataTools.getModifiedFlagPropertyName(
						propertyName,
						context.getGlobalConfiguration().getModifiedFlagSuffix()
				)
		);
		propertyData.setBeanName( attribute.getName() );
		propertyData.setAccessType( accessType );

		addPropertyJoinTables( attributeAnnotations, propertyData );
		addPropertyAuditingOverrides( attributeAnnotations, propertyData );
		if ( !processPropertyAuditingOverrides( attribute, propertyData ) ) {
			return false; // not audited due to AuditOverride annotation
		}
		addPropertyMapKey( attributeAnnotations, propertyData );
		setPropertyAuditMappedBy( attributeAnnotations, propertyData );
		setPropertyRelationMappedBy( attributeAnnotations, propertyData );

		return true;
	}

	protected boolean checkAudited(Attribute attribute, PropertyAuditingData propertyData, Audited allClassAudited) {
		Map<DotName, List<AnnotationInstance>> attributeAnnotations = context.locateAttributeAnnotations( attribute );
		// Checking if this property is explicitly audited or if all properties are.
		Audited aud = attributeAnnotations.containsKey( EnversDotNames.AUDITED )
				? context.getAnnotationProxy(
				attributeAnnotations.get( EnversDotNames.AUDITED ).get( 0 ),
				Audited.class
		)
				: allClassAudited;
		if ( aud == null &&
				overriddenAuditedProperties.contains( attribute ) &&
				!overriddenNotAuditedProperties.contains( attribute ) ) {
			// Assigning @Audited defaults. If anyone needs to customize those values in the future,
			// appropriate fields shall be added to @AuditOverride annotation.
			aud = DEFAULT_AUDITED;
		}
		if ( aud != null ) {
			propertyData.setStore( aud.modStore() );
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean checkUsingModifiedFlag(Audited aud) {
		return context.getGlobalConfiguration().hasSettingForUsingModifiedFlag() ?
				context.getGlobalConfiguration().isGlobalWithModifiedFlag() : aud.withModifiedFlag();
	}

	private void setPropertyRelationMappedBy(Map<DotName, List<AnnotationInstance>> attributeAnnotations,
											 PropertyAuditingData propertyData) {
		final AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation(
				attributeAnnotations,
				JPADotNames.ONE_TO_MANY
		);
		if ( oneToMany != null ) {
			final String mappedBy = JandexHelper.getValue(
					oneToMany, "mappedBy", String.class, context.getClassLoaderService()
			);
			if ( StringHelper.isNotEmpty( mappedBy ) ) {
				propertyData.setRelationMappedBy( mappedBy );
			}
		}
	}

	private void setPropertyAuditMappedBy(
			Map<DotName, List<AnnotationInstance>> attributeAnnotations,
			PropertyAuditingData propertyData) {
		final AnnotationInstance auditMappedBy = JandexHelper.getSingleAnnotation(
				attributeAnnotations,
				EnversDotNames.AUDIT_MAPPED_BY
		);
		if ( auditMappedBy != null ) {
			propertyData.setAuditMappedBy(
					JandexHelper.getValue( auditMappedBy, "mappedBy", String.class, context.getClassLoaderService() )
			);
			final String positionMappedBy = JandexHelper.getValue(
					auditMappedBy, "positionMappedBy", String.class, context.getClassLoaderService()
			);
			if ( StringHelper.isNotEmpty( positionMappedBy ) ) {
				propertyData.setPositionMappedBy( positionMappedBy );
			}
		}
	}

	private void addPropertyMapKey(
			Map<DotName, List<AnnotationInstance>> attributeAnnotations,
			PropertyAuditingData propertyData) {
		final AnnotationInstance mapKey = JandexHelper.getSingleAnnotation( attributeAnnotations, JPADotNames.MAP_KEY );
		if ( mapKey != null ) {
			propertyData.setMapKey(
					JandexHelper.getValue(
							mapKey,
							"name",
							String.class,
							context.getClassLoaderService()
					)
			);
		}
	}

	private void addPropertyJoinTables(Map<DotName, List<AnnotationInstance>> attributeAnnotations,
									   PropertyAuditingData propertyData) {
		// first set the join table based on the AuditJoinTable annotation
		final AnnotationInstance joinTable = JandexHelper.getSingleAnnotation(
				attributeAnnotations,
				EnversDotNames.AUDIT_JOIN_TABLE
		);
		if ( joinTable != null ) {
			propertyData.setJoinTable( context.getAnnotationProxy( joinTable, AuditJoinTable.class ) );
		} else {
			propertyData.setJoinTable( DEFAULT_AUDIT_JOIN_TABLE );
		}
	}

	/***
	 * Add the {@link org.hibernate.envers.AuditOverride} annotations.
	 *
	 * @param attributeAnnotations the property being processed
	 * @param propertyData the Envers auditing data for this property
	 */
	private void addPropertyAuditingOverrides(
			Map<DotName, List<AnnotationInstance>> attributeAnnotations,
			PropertyAuditingData propertyData) {
		final AnnotationInstance annotationOverride = JandexHelper.getSingleAnnotation( attributeAnnotations, EnversDotNames.AUDIT_OVERRIDE );
		if ( annotationOverride != null ) {
			propertyData.addAuditingOverride( context.getAnnotationProxy( annotationOverride, AuditOverride.class ) );
		}
		final AnnotationInstance annotationOverrides = JandexHelper.getSingleAnnotation( attributeAnnotations, EnversDotNames.AUDIT_OVERRIDES );
		if ( annotationOverrides != null ) {
			propertyData.addAuditingOverrides( context.getAnnotationProxy( annotationOverrides, AuditOverrides.class ) );
		}
	}

	/**
	 * Process the {@link org.hibernate.envers.AuditOverride} annotations for this property.
	 *
	 * @param attribute
	 *            the property for which the {@link org.hibernate.envers.AuditOverride}
	 *            annotations are being processed
	 * @param propertyData
	 *            the Envers auditing data for this property
	 * @return {@code false} if isAudited() of the override annotation was set to
	 */
	private boolean processPropertyAuditingOverrides(Attribute attribute, PropertyAuditingData propertyData) {
		// if this property is part of a component, process all override annotations
		if (this.auditedPropertiesHolder instanceof ComponentAuditingData) {
			List<AuditOverride> overrides = ((ComponentAuditingData) this.auditedPropertiesHolder).getAuditingOverrides();
			for (AuditOverride override : overrides) {
				if (attribute.getName().equals(override.name())) {
					// the override applies to this property
					if (!override.isAudited()) {
						return false;
					} else {
						if (override.auditJoinTable() != null) {
							propertyData.setJoinTable(override.auditJoinTable());
						}
					}
				}
			}

		}
		return true;
	}

	private static final Audited DEFAULT_AUDITED = new Audited() {
		@Override
		public ModificationStore modStore() {
			return ModificationStore.FULL;
		}

		@Override
		public RelationTargetAuditMode targetAuditMode() {
			return RelationTargetAuditMode.AUDITED;
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
		public Class<? extends Annotation> annotationType() {
			return this.getClass();
		}
	};

	private static final AuditJoinTable DEFAULT_AUDIT_JOIN_TABLE = new AuditJoinTable() {
		@Override
		public String name() {
			return "";
		}

		@Override
		public String schema() {
			return "";
		}

		@Override
		public String catalog() {
			return "";
		}

		@Override
		public JoinColumn[] inverseJoinColumns() {
			return new JoinColumn[0];
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return this.getClass();
		}
	};

	public static class ComponentPropertiesSource implements PersistentPropertiesSource {
		private final ClassInfo classInfo;
		private final AttributeBindingContainer attributeBindingContainer;

		public ComponentPropertiesSource(
				ClassInfo classInfo,
				AttributeBindingContainer attributeBindingContainer) {
			this.classInfo = classInfo;
			this.attributeBindingContainer = attributeBindingContainer;
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public Iterable<AttributeBinding> getNonIdAttributeBindings() {
			return attributeBindingContainer.attributeBindings();
		}

		@Override
		public AttributeBinding getAttributeBinding(String attributeName) {
			return attributeBindingContainer.locateAttributeBinding( attributeName );
		}

		@Override
		public AttributeBindingContainer getAttributeBindingContainer() {
			return attributeBindingContainer;
		}

		@Override
		public ClassInfo getClassInfo() {
			return classInfo;
		}
	}

	public static class DynamicComponentSource extends ComponentPropertiesSource {

		public DynamicComponentSource(
				AttributeBindingContainer attributeBindingContainer) {
			//TODO: need a ClassInfo for Map.
			//super( reflectionManager.toXClass( Map.class ), attributeBindingContainer );
			super( null, attributeBindingContainer );
			throw new NotYetImplementedException( "dynamic components are not supported yet." );
		}
	}
}
