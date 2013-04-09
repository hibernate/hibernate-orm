package org.hibernate.envers.configuration.metadata.reader;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.configuration.metadata.MetadataTools;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.Tools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

import static org.hibernate.envers.tools.Tools.newHashMap;
import static org.hibernate.envers.tools.Tools.newHashSet;

/**
 * Reads persistent properties form a
 * {@link org.hibernate.envers.configuration.metadata.reader.PersistentPropertiesSource}
 * and adds the ones that are audited to a
 * {@link org.hibernate.envers.configuration.metadata.reader.AuditedPropertiesHolder},
 * filling all the auditing data.
 * @author Adam Warski (adam at warski dot org)
 * @author Erik-Berndt Scheper
 * @author Hern&aacut;n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class AuditedPropertiesReader {
	protected final ModificationStore defaultStore;
	private final PersistentPropertiesSource persistentPropertiesSource;
	private final AuditedPropertiesHolder auditedPropertiesHolder;
	private final GlobalConfiguration globalCfg;
	private final ReflectionManager reflectionManager;
	private final String propertyNamePrefix;

	private final Set<String> propertyAccessedPersistentProperties;
	private final Set<String> fieldAccessedPersistentProperties;
	// Mapping class field to corresponding <properties> element.
	private final Map<String, String> propertiesGroupMapping;

	private final Set<XProperty> overriddenAuditedProperties;
	private final Set<XProperty> overriddenNotAuditedProperties;

	private final Set<XClass> overriddenAuditedClasses;
	private final Set<XClass> overriddenNotAuditedClasses;

	public AuditedPropertiesReader(ModificationStore defaultStore,
								   PersistentPropertiesSource persistentPropertiesSource,
								   AuditedPropertiesHolder auditedPropertiesHolder,
								   GlobalConfiguration globalCfg,
								   ReflectionManager reflectionManager,
								   String propertyNamePrefix) {
		this.defaultStore = defaultStore;
		this.persistentPropertiesSource = persistentPropertiesSource;
		this.auditedPropertiesHolder = auditedPropertiesHolder;
		this.globalCfg = globalCfg;
		this.reflectionManager = reflectionManager;
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

        // Retrieve classes and properties that are explicitly marked for auditing process by any superclass
        // of currently mapped entity or itself.
        XClass clazz = persistentPropertiesSource.getXClass();
        readAuditOverrides(clazz);

        // Adding all properties from the given class.
        addPropertiesFromClass(clazz);
	}

    /**
     * Recursively constructs sets of audited and not audited properties and classes which behavior has been overridden
     * using {@link AuditOverride} annotation.
     * @param clazz Class that is being processed. Currently mapped entity shall be passed during first invocation.
     */
    private void readAuditOverrides(XClass clazz) {
        /* TODO: Code to remove with @Audited.auditParents - start. */
        Audited allClassAudited = clazz.getAnnotation(Audited.class);
        if (allClassAudited != null && allClassAudited.auditParents().length > 0) {
            for (Class c : allClassAudited.auditParents()) {
                XClass parentClass = reflectionManager.toXClass(c);
                checkSuperclass(clazz, parentClass);
                if (!overriddenNotAuditedClasses.contains(parentClass)) {
                    // If the class has not been marked as not audited by the subclass.
                    overriddenAuditedClasses.add(parentClass);
                }
            }
        }
        /* TODO: Code to remove with @Audited.auditParents - finish. */
        List<AuditOverride> auditOverrides = computeAuditOverrides(clazz);
        for (AuditOverride auditOverride : auditOverrides) {
            if (auditOverride.forClass() != void.class) {
                XClass overrideClass = reflectionManager.toXClass(auditOverride.forClass());
                checkSuperclass(clazz, overrideClass);
                String propertyName = auditOverride.name();
                if (!StringTools.isEmpty(propertyName)) {
                    // Override @Audited annotation on property level.
                    XProperty property = getProperty(overrideClass, propertyName);
                    if (auditOverride.isAudited()) {
                        if (!overriddenNotAuditedProperties.contains(property)) {
                            // If the property has not been marked as not audited by the subclass.
                            overriddenAuditedProperties.add(property);
                        }
                    } else {
                        if (!overriddenAuditedProperties.contains(property)) {
                            // If the property has not been marked as audited by the subclass.
                            overriddenNotAuditedProperties.add(property);
                        }
                    }
                } else {
                    // Override @Audited annotation on class level.
                    if (auditOverride.isAudited()) {
                        if (!overriddenNotAuditedClasses.contains(overrideClass)) {
                            // If the class has not been marked as not audited by the subclass.
                            overriddenAuditedClasses.add(overrideClass);
                        }
                    } else {
                        if (!overriddenAuditedClasses.contains(overrideClass)) {
                            // If the class has not been marked as audited by the subclass.
                            overriddenNotAuditedClasses.add(overrideClass);
                        }
                    }
                }
            }
        }
        XClass superclass = clazz.getSuperclass();
        if (!clazz.isInterface() && !Object.class.getName().equals(superclass.getName())) {
            readAuditOverrides(superclass);
        }
    }

    /**
     * @param clazz Source class.
     * @return List of @AuditOverride annotations applied at class level.
     */
    private List<AuditOverride> computeAuditOverrides(XClass clazz) {
        AuditOverrides auditOverrides = clazz.getAnnotation(AuditOverrides.class);
        AuditOverride auditOverride = clazz.getAnnotation(AuditOverride.class);
        if (auditOverrides == null && auditOverride != null) {
            return Arrays.asList(auditOverride);
        } else if (auditOverrides != null && auditOverride == null) {
            return Arrays.asList(auditOverrides.value());
        } else if (auditOverrides != null && auditOverride != null) {
            throw new MappingException("@AuditOverrides annotation should encapsulate all @AuditOverride declarations. " +
                                       "Please revise Envers annotations applied to class " + clazz.getName() + ".");
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Checks whether one class is assignable from another. If not {@link MappingException} is thrown.
     * @param child Subclass.
     * @param parent Superclass.
     */
    private void checkSuperclass(XClass child, XClass parent) {
        if (!parent.isAssignableFrom(child)) {
            throw new MappingException("Class " + parent.getName() + " is not assignable from " + child.getName() + ". " +
                                       "Please revise Envers annotations applied to " + child.getName() + " type.");
        }
    }

    /**
     * Checks whether class contains property with a given name. If not {@link MappingException} is thrown.
     * @param clazz Class.
     * @param propertyName Property name.
     * @return Property object.
     */
    private XProperty getProperty(XClass clazz, String propertyName) {
        XProperty property = Tools.getProperty(clazz, propertyName);
        if (property == null) {
            throw new MappingException("Property '" + propertyName + "' not found in class " + clazz.getName() + ". " +
                                       "Please revise Envers annotations applied to class " + persistentPropertiesSource.getXClass() + ".");
        }
        return property;
    }

	private void readPersistentPropertiesAccess() {
		Iterator<Property> propertyIter = persistentPropertiesSource.getPropertyIterator();
		while (propertyIter.hasNext()) {
			Property property = (Property) propertyIter.next();
			addPersistentProperty(property);
			if ("embedded".equals(property.getPropertyAccessorName()) && property.getName().equals(property.getNodeName())) {
				// If property name equals node name and embedded accessor type is used, processing component
				// has been defined with <properties> tag. See HHH-6636 JIRA issue.
				createPropertiesGroupMapping(property);
			}
		}
	}

    private void addPersistentProperty(Property property) {
        if ("field".equals(property.getPropertyAccessorName())) {
            fieldAccessedPersistentProperties.add(property.getName());
        } else {
            propertyAccessedPersistentProperties.add(property.getName());
        }
    }

    private void createPropertiesGroupMapping(Property property) {
        Component component = (Component) property.getValue();
        Iterator<Property> componentProperties = component.getPropertyIterator();
        while (componentProperties.hasNext()) {
            Property componentProperty = componentProperties.next();
            propertiesGroupMapping.put(componentProperty.getName(), component.getNodeName());
        }
    }

    /**
     * @param clazz Class which properties are currently being added.
     * @return {@link Audited} annotation of specified class. If processed type hasn't been explicitly marked, method
     *         checks whether given class exists in {@link AuditedPropertiesReader#overriddenAuditedClasses} collection.
     *         In case of success, {@link Audited} configuration of currently mapped entity is returned, otherwise
     *         {@code null}. If processed type exists in {@link AuditedPropertiesReader#overriddenNotAuditedClasses}
     *         collection, the result is also {@code null}.
     */
    private Audited computeAuditConfiguration(XClass clazz) {
        Audited allClassAudited = clazz.getAnnotation(Audited.class);
        // If processed class is not explicitly marked with @Audited annotation, check whether auditing is
        // forced by any of its child entities configuration (@AuditedOverride.forClass).
        if (allClassAudited == null && overriddenAuditedClasses.contains(clazz)) {
            // Declared audited parent copies @Audited.modStore and @Audited.targetAuditMode configuration from
            // currently mapped entity.
            allClassAudited = persistentPropertiesSource.getXClass().getAnnotation(Audited.class);
            if (allClassAudited == null) {
                // If parent class declares @Audited on the field/property level.
                allClassAudited = DEFAULT_AUDITED;
            }
        } else if (allClassAudited != null && overriddenNotAuditedClasses.contains(clazz)) {
            return null;
        }
        return allClassAudited;
    }

    /**
     * Recursively adds all audited properties of entity class and its superclasses.
     * @param clazz Currently processed class.
     */
	private void addPropertiesFromClass(XClass clazz)  {
		Audited allClassAudited = computeAuditConfiguration(clazz);

		//look in the class
		addFromProperties(clazz.getDeclaredProperties("field"), "field", fieldAccessedPersistentProperties, allClassAudited);
		addFromProperties(clazz.getDeclaredProperties("property"), "property", propertyAccessedPersistentProperties, allClassAudited);
		
		if(allClassAudited != null || !auditedPropertiesHolder.isEmpty()) {
			XClass superclazz = clazz.getSuperclass();
			if (!clazz.isInterface() && !"java.lang.Object".equals(superclazz.getName())) {
				addPropertiesFromClass(superclazz);
			}
		}
	}

	private void addFromProperties(Iterable<XProperty> properties, String accessType, Set<String> persistentProperties, Audited allClassAudited) {
		for (XProperty property : properties) {
			// If this is not a persistent property, with the same access type as currently checked,
			// it's not audited as well. 
			// If the property was already defined by the subclass, is ignored by superclasses
			if ((persistentProperties.contains(property.getName()) && (!auditedPropertiesHolder
					.contains(property.getName())))) {
				Value propertyValue = persistentPropertiesSource.getProperty(property.getName()).getValue();
				if (propertyValue instanceof Component) {
					this.addFromComponentProperty(property, accessType, (Component)propertyValue, allClassAudited);
				} else {
					this.addFromNotComponentProperty(property, accessType, allClassAudited);
				}
			} else if (propertiesGroupMapping.containsKey(property.getName())) {
				// Retrieve embedded component name based on class field.
				final String embeddedName = propertiesGroupMapping.get(property.getName());
				if (!auditedPropertiesHolder.contains(embeddedName)) {
					// Manage properties mapped within <properties> tag.
					Value propertyValue = persistentPropertiesSource.getProperty(embeddedName).getValue();
					this.addFromPropertiesGroup(embeddedName, property, accessType, (Component)propertyValue, allClassAudited);
				}
			}
		}
	}

	private void addFromPropertiesGroup(String embeddedName, XProperty property, String accessType, Component propertyValue,
										Audited allClassAudited) {
		ComponentAuditingData componentData = new ComponentAuditingData();
		boolean isAudited = fillPropertyData(property, componentData, accessType, allClassAudited);
		if (isAudited) {
			// EntityPersister.getPropertyNames() returns name of embedded component instead of class field.
			componentData.setName(embeddedName);
			// Marking component properties as placed directly in class (not inside another component).
			componentData.setBeanName(null);

			PersistentPropertiesSource componentPropertiesSource = new ComponentPropertiesSource( reflectionManager, propertyValue );
			AuditedPropertiesReader audPropReader = new AuditedPropertiesReader(
					ModificationStore.FULL, componentPropertiesSource, componentData, globalCfg, reflectionManager,
					propertyNamePrefix + MappingTools.createComponentPrefix(embeddedName)
			);
			audPropReader.read();

			auditedPropertiesHolder.addPropertyAuditingData(embeddedName, componentData);
		}
	}

	private void addFromComponentProperty(XProperty property, String accessType, Component propertyValue, Audited allClassAudited) {
		ComponentAuditingData componentData = new ComponentAuditingData();
		boolean isAudited = fillPropertyData( property, componentData, accessType, allClassAudited );

		if ( propertyValue.isDynamic() ) {
			if ( isAudited ) {
				throw new MappingException(
						"Audited dynamic-component properties are not supported. Consider applying @NotAudited annotation to "
								+ propertyValue.getOwner().getEntityName() + "#" + property + "."
				);
			}
			return;
		}

		PersistentPropertiesSource componentPropertiesSource = new ComponentPropertiesSource(
				reflectionManager, propertyValue
		);

		ComponentAuditedPropertiesReader audPropReader = new ComponentAuditedPropertiesReader(
				ModificationStore.FULL, componentPropertiesSource, componentData, globalCfg, reflectionManager,
				propertyNamePrefix + MappingTools.createComponentPrefix( property.getName() )
		);
		audPropReader.read();

		if ( isAudited ) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData( property.getName(), componentData );
		}
	}

	private void addFromNotComponentProperty(XProperty property, String accessType, Audited allClassAudited){
		PropertyAuditingData propertyData = new PropertyAuditingData();
		boolean isAudited = fillPropertyData(property, propertyData, accessType, allClassAudited);

		if (isAudited) {
			// Now we know that the property is audited
			auditedPropertiesHolder.addPropertyAuditingData(property.getName(), propertyData);
		}
	}
	
	
	/**
	 * Checks if a property is audited and if yes, fills all of its data.
	 * @param property Property to check.
	 * @param propertyData Property data, on which to set this property's modification store.
	 * @param accessType Access type for the property.
	 * @return False if this property is not audited.
	 */
	private boolean fillPropertyData(XProperty property, PropertyAuditingData propertyData,
									 String accessType, Audited allClassAudited) {

		// check if a property is declared as not audited to exclude it
		// useful if a class is audited but some properties should be excluded
		NotAudited unVer = property.getAnnotation(NotAudited.class);
		if ((unVer != null && !overriddenAuditedProperties.contains(property)) || overriddenNotAuditedProperties.contains(property)) {
			return false;
		} else {
			// if the optimistic locking field has to be unversioned and the current property
			// is the optimistic locking field, don't audit it
			if (globalCfg.isDoNotAuditOptimisticLockingField()) {
				Version jpaVer = property.getAnnotation(Version.class);
				if (jpaVer != null) {
					return false;
				}
			}
		}

		
		if(!this.checkAudited(property, propertyData, allClassAudited)){
			return false;
		}

		String propertyName = propertyNamePrefix + property.getName();
		propertyData.setName(propertyName);
		propertyData.setModifiedFlagName(
                MetadataTools.getModifiedFlagPropertyName(
                        propertyName,
                        globalCfg.getModifiedFlagSuffix()));
		propertyData.setBeanName(property.getName());
		propertyData.setAccessType(accessType);

		addPropertyJoinTables(property, propertyData);
		addPropertyAuditingOverrides(property, propertyData);
		if (!processPropertyAuditingOverrides(property, propertyData)) {
			return false; // not audited due to AuditOverride annotation
		}
        addPropertyMapKey(property, propertyData);
        setPropertyAuditMappedBy(property, propertyData);
        setPropertyRelationMappedBy(property, propertyData);

		return true;
	}


	protected boolean checkAudited(XProperty property,
			PropertyAuditingData propertyData, Audited allClassAudited) {
		// Checking if this property is explicitly audited or if all properties are.
		Audited aud = (property.isAnnotationPresent(Audited.class)) ? (property.getAnnotation(Audited.class)) : allClassAudited;
		if (aud == null && overriddenAuditedProperties.contains(property) && !overriddenNotAuditedProperties.contains(property)) {
			// Assigning @Audited defaults. If anyone needs to customize those values in the future,
			// appropriate fields shall be added to @AuditOverride annotation.
			aud = DEFAULT_AUDITED;
		}
		if (aud != null) {
			propertyData.setStore(aud.modStore());
			propertyData.setRelationTargetAuditMode(aud.targetAuditMode());
			propertyData.setUsingModifiedFlag(checkUsingModifiedFlag(aud));
			return true;
		} else {
			return false;
		}
	}

	protected boolean checkUsingModifiedFlag(Audited aud) {
		return globalCfg.hasSettingForUsingModifiedFlag() ?
				globalCfg.isGlobalWithModifiedFlag() : aud.withModifiedFlag();
	}

    private void setPropertyRelationMappedBy(XProperty property, PropertyAuditingData propertyData) {
        OneToMany oneToMany = property.getAnnotation(OneToMany.class);
        if (oneToMany != null && !"".equals(oneToMany.mappedBy())) {
            propertyData.setRelationMappedBy(oneToMany.mappedBy());
        }
    }

	private void setPropertyAuditMappedBy(XProperty property, PropertyAuditingData propertyData) {
        AuditMappedBy auditMappedBy = property.getAnnotation(AuditMappedBy.class);
        if (auditMappedBy != null) {
		    propertyData.setAuditMappedBy(auditMappedBy.mappedBy());
            if (!"".equals(auditMappedBy.positionMappedBy())) {
                propertyData.setPositionMappedBy(auditMappedBy.positionMappedBy());
            }
        }
    }

	private void addPropertyMapKey(XProperty property, PropertyAuditingData propertyData) {
		MapKey mapKey = property.getAnnotation(MapKey.class);
		if (mapKey != null) {
			propertyData.setMapKey(mapKey.name());
		}
	}

	private void addPropertyJoinTables(XProperty property, PropertyAuditingData propertyData) {
		// first set the join table based on the AuditJoinTable annotation
		AuditJoinTable joinTable = property.getAnnotation(AuditJoinTable.class);
		if (joinTable != null) {
			propertyData.setJoinTable(joinTable);
		} else {
			propertyData.setJoinTable(DEFAULT_AUDIT_JOIN_TABLE);
		}
	}

	/***
	 * Add the {@link org.hibernate.envers.AuditOverride} annotations.
	 *
	 * @param property the property being processed
	 * @param propertyData the Envers auditing data for this property
	 */
	private void addPropertyAuditingOverrides(XProperty property, PropertyAuditingData propertyData) {
		AuditOverride annotationOverride = property.getAnnotation(AuditOverride.class);
		if (annotationOverride != null) {
			propertyData.addAuditingOverride(annotationOverride);
		}
		AuditOverrides annotationOverrides = property.getAnnotation(AuditOverrides.class);
		if (annotationOverrides != null) {
			propertyData.addAuditingOverrides(annotationOverrides);
		}
	}

	/**
	 * Process the {@link org.hibernate.envers.AuditOverride} annotations for this property.
	 *
	 * @param property
	 *            the property for which the {@link org.hibernate.envers.AuditOverride}
	 *            annotations are being processed
	 * @param propertyData
	 *            the Envers auditing data for this property
	 * @return {@code false} if isAudited() of the override annotation was set to
	 */
	private boolean processPropertyAuditingOverrides(XProperty property, PropertyAuditingData propertyData) {
		// if this property is part of a component, process all override annotations
		if (this.auditedPropertiesHolder instanceof ComponentAuditingData) {
			List<AuditOverride> overrides = ((ComponentAuditingData) this.auditedPropertiesHolder).getAuditingOverrides();
			for (AuditOverride override : overrides) {
				if (property.getName().equals(override.name())) {
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

    private static Audited DEFAULT_AUDITED = new Audited() {
        public ModificationStore modStore() { return ModificationStore.FULL; }
        public RelationTargetAuditMode targetAuditMode() { return RelationTargetAuditMode.AUDITED; }
        public Class[] auditParents() { return new Class[0]; }
        public boolean withModifiedFlag() { return false; }
        public Class<? extends Annotation> annotationType() { return this.getClass(); }
    };

	private static AuditJoinTable DEFAULT_AUDIT_JOIN_TABLE = new AuditJoinTable() {
		public String name() { return ""; }
		public String schema() { return ""; }
		public String catalog() { return ""; }
		public JoinColumn[] inverseJoinColumns() { return new JoinColumn[0]; }
		public Class<? extends Annotation> annotationType() { return this.getClass(); }
	};

    public static class ComponentPropertiesSource implements PersistentPropertiesSource {
		private final XClass xclass;
		private final Component component;

		public ComponentPropertiesSource(ReflectionManager reflectionManager, Component component) {
			try {
				this.xclass = reflectionManager.classForName(component.getComponentClassName(), this.getClass());
			} catch (ClassNotFoundException e) {
				throw new MappingException(e);
			}

			this.component = component;
		}

		@SuppressWarnings({"unchecked"})
		public Iterator<Property> getPropertyIterator() { return component.getPropertyIterator(); }
		public Property getProperty(String propertyName) { return component.getProperty(propertyName); }
		public XClass getXClass() { return xclass; }
	}
}
