package org.hibernate.envers.configuration.metadata.reader;

import static org.hibernate.envers.tools.Tools.newHashSet;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.Version;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.envers.*;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.MappingException;

/**
 * Reads persistent properties form a
 * {@link org.hibernate.envers.configuration.metadata.reader.PersistentPropertiesSource}
 * and adds the ones that are audited to a
 * {@link org.hibernate.envers.configuration.metadata.reader.AuditedPropertiesHolder},
 * filling all the auditing data.
 * @author Adam Warski (adam at warski dot org)
 * @author Erik-Berndt Scheper
 */
public class AuditedPropertiesReader {
	private final ModificationStore defaultStore;
	private final PersistentPropertiesSource persistentPropertiesSource;
	private final AuditedPropertiesHolder auditedPropertiesHolder;
	private final GlobalConfiguration globalCfg;
	private final ReflectionManager reflectionManager;
	private final String propertyNamePrefix;

	private final Set<String> propertyAccessedPersistentProperties;
	private final Set<String> fieldAccessedPersistentProperties;

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
	}

	public void read() {
		// First reading the access types for the persistent properties.
		readPersistentPropertiesAccess();

		// Adding all properties from the given class.
		addPropertiesFromClass(persistentPropertiesSource.getXClass());
	}

	private void readPersistentPropertiesAccess() {
		Iterator propertyIter = persistentPropertiesSource.getPropertyIterator();
		while (propertyIter.hasNext()) {
			Property property = (Property) propertyIter.next();
			if ("field".equals(property.getPropertyAccessorName())) {
				fieldAccessedPersistentProperties.add(property.getName());
			} else {
				propertyAccessedPersistentProperties.add(property.getName());
			}
		}
	}

	private void addPropertiesFromClass(XClass clazz)  {
		XClass superclazz = clazz.getSuperclass();
		if (!clazz.isInterface() && !"java.lang.Object".equals(superclazz.getName())) {
			addPropertiesFromClass(superclazz);
		}

		addFromProperties(clazz.getDeclaredProperties("field"), "field", fieldAccessedPersistentProperties);
		addFromProperties(clazz.getDeclaredProperties("property"), "property", propertyAccessedPersistentProperties);
	}

	private void addFromProperties(Iterable<XProperty> properties, String accessType, Set<String> persistentProperties) {
		for (XProperty property : properties) {
			// If this is not a persistent property, with the same access type as currently checked,
			// it's not audited as well.
			if (persistentProperties.contains(property.getName())) {
				Value propertyValue = persistentPropertiesSource.getProperty(property.getName()).getValue();

				PropertyAuditingData propertyData;
				boolean isAudited;
				if (propertyValue instanceof Component) {
					ComponentAuditingData componentData = new ComponentAuditingData();
					isAudited = fillPropertyData(property, componentData, accessType);

					PersistentPropertiesSource componentPropertiesSource = new ComponentPropertiesSource(
							(Component) propertyValue);
					new AuditedPropertiesReader(ModificationStore.FULL, componentPropertiesSource, componentData,
							globalCfg, reflectionManager,
							propertyNamePrefix + MappingTools.createComponentPrefix(property.getName()))
							.read();

					propertyData = componentData;
				} else {
					propertyData = new PropertyAuditingData();
					isAudited = fillPropertyData(property, propertyData, accessType);
				}

				if (isAudited) {
					// Now we know that the property is audited
					auditedPropertiesHolder.addPropertyAuditingData(property.getName(), propertyData);
				}
			}
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
									 String accessType) {

		// check if a property is declared as not audited to exclude it
		// useful if a class is audited but some properties should be excluded
		NotAudited unVer = property.getAnnotation(NotAudited.class);
		if (unVer != null) {
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

		// Checking if this property is explicitly audited or if all properties are.
		Audited aud = property.getAnnotation(Audited.class);
		if (aud != null) {
			propertyData.setStore(aud.modStore());
			propertyData.setRelationTargetAuditMode(aud.targetAuditMode());
		} else {
			if (defaultStore != null) {
				propertyData.setStore(defaultStore);
			} else {
				return false;
			}
		}

		propertyData.setName(propertyNamePrefix + property.getName());
		propertyData.setBeanName(property.getName());
		propertyData.setAccessType(accessType);

		addPropertyJoinTables(property, propertyData);
		addPropertyAuditingOverrides(property, propertyData);
		if (!processPropertyAuditingOverrides(property, propertyData)) {
			return false; // not audited due to AuditOverride annotation
		}
		addPropertyMapKey(property, propertyData);
        setPropertyAuditMappedBy(property, propertyData);

		return true;
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

	private static AuditJoinTable DEFAULT_AUDIT_JOIN_TABLE = new AuditJoinTable() {
		public String name() { return ""; }
		public String schema() { return ""; }
		public String catalog() { return ""; }
		public JoinColumn[] inverseJoinColumns() { return new JoinColumn[0]; }
		public Class<? extends Annotation> annotationType() { return this.getClass(); }
	};

    private class ComponentPropertiesSource implements PersistentPropertiesSource {
		private final XClass xclass;
		private final Component component;

		private ComponentPropertiesSource(Component component) {
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
