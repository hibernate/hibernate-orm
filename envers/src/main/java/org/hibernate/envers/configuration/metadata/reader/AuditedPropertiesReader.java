package org.hibernate.envers.configuration.metadata.reader;

import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.configuration.GlobalConfiguration;
import static org.hibernate.envers.tools.Tools.*;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Value;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.XClass;
import org.jboss.envers.Versioned;

import javax.persistence.Version;
import javax.persistence.MapKey;
import javax.persistence.JoinColumn;
import java.util.Iterator;
import java.util.Set;
import java.lang.annotation.Annotation;

/**
 * Reads persistent properties form a
 * {@link org.hibernate.envers.configuration.metadata.reader.PersistentPropertiesSource}
 * and adds the ones that are audited to a
 * {@link org.hibernate.envers.configuration.metadata.reader.AuditedPropertiesHolder},
 * filling all the auditing data.
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditedPropertiesReader {
	private final ModificationStore defaultStore;
	private final PersistentPropertiesSource persistentPropertiesSource;
	private final AuditedPropertiesHolder auditedPropertiesHolder;
	private final GlobalConfiguration globalCfg;
	private final String propertyNamePrefix;

	private final Set<String> propertyAccessedPersistentProperties;
	private final Set<String> fieldAccessedPersistentProperties;

	public AuditedPropertiesReader(ModificationStore defaultStore,
								   PersistentPropertiesSource persistentPropertiesSource,
								   AuditedPropertiesHolder auditedPropertiesHolder,
								   GlobalConfiguration globalCfg,
								   String propertyNamePrefix) {
		this.defaultStore = defaultStore;
		this.persistentPropertiesSource = persistentPropertiesSource;
		this.auditedPropertiesHolder = auditedPropertiesHolder;
		this.globalCfg = globalCfg;
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
		if (!"java.lang.Object".equals(superclazz.getName())) {
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

					// TODO: component stuff
					PersistentPropertiesSource componentPropertiesSource = new ComponentPropertiesSource(
							property.getType(), (Component) propertyValue);
					new AuditedPropertiesReader(ModificationStore.FULL, componentPropertiesSource, componentData,
							globalCfg, propertyNamePrefix+ MappingTools.createComponentPrefix(property.getName()))
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
		Versioned ver = property.getAnnotation(Versioned.class);
		if (aud != null) {
			propertyData.setStore(aud.modStore());
		} else if (ver != null) {
			propertyData.setStore(ModificationStore.FULL);
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
		addPropertyMapKey(property, propertyData);

		return true;
	}

	private void addPropertyMapKey(XProperty property, PropertyAuditingData propertyData) {
		MapKey mapKey = property.getAnnotation(MapKey.class);
		if (mapKey != null) {
			propertyData.setMapKey(mapKey.name());
		}
	}

	private void addPropertyJoinTables(XProperty property, PropertyAuditingData propertyData) {
		AuditJoinTable joinTable = property.getAnnotation(AuditJoinTable.class);
		if (joinTable != null) {
			propertyData.setJoinTable(joinTable);
		} else {
			propertyData.setJoinTable(DEFAULT_AUDIT_JOIN_TABLE);
		}
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

		private ComponentPropertiesSource(XClass xclass, Component component) {
			this.xclass = xclass;
			this.component = component;
		}

		@SuppressWarnings({"unchecked"})
		public Iterator<Property> getPropertyIterator() { return component.getPropertyIterator(); }
		public Property getProperty(String propertyName) { return component.getProperty(propertyName); }
		public XClass getXClass() { return xclass; }
	}
}
