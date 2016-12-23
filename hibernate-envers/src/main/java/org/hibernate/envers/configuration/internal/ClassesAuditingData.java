/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesHolder;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.List;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.jboss.logging.Logger;

/**
 * A helper class holding auditing meta-data for all persistent classes.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class ClassesAuditingData {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			ClassesAuditingData.class.getName()
	);

	private final Map<String, ClassAuditingData> entityNameToAuditingData = new HashMap<>();
	private final Map<PersistentClass, ClassAuditingData> persistentClassToAuditingData = new LinkedHashMap<>();

	/**
	 * Stores information about auditing meta-data for the given class.
	 *
	 * @param pc Persistent class.
	 * @param cad Auditing meta-data for the given class.
	 */
	public void addClassAuditingData(PersistentClass pc, ClassAuditingData cad) {
		entityNameToAuditingData.put( pc.getEntityName(), cad );
		persistentClassToAuditingData.put( pc, cad );
	}

	/**
	 * @return A collection of all auditing meta-data for persistent classes.
	 */
	public Collection<Map.Entry<PersistentClass, ClassAuditingData>> getAllClassAuditedData() {
		return persistentClassToAuditingData.entrySet();
	}

	/**
	 * @param entityName Name of the entity.
	 *
	 * @return Auditing meta-data for the given entity.
	 */
	public ClassAuditingData getClassAuditingData(String entityName) {
		return entityNameToAuditingData.get( entityName );
	}

	/**
	 * After all meta-data is read, updates calculated fields. This includes:
	 * <ul>
	 * <li>setting {@code forceInsertable} to {@code true} for properties specified by {@code @AuditMappedBy}</li>
	 * <li>adding {@code synthetic} properties to mappedBy relations which have {@code IndexColumn} or {@code OrderColumn}.</li>
	 * </ul>
	 */
	public void updateCalculatedFields() {
		for ( Map.Entry<PersistentClass, ClassAuditingData> classAuditingDataEntry : persistentClassToAuditingData.entrySet() ) {
			final PersistentClass pc = classAuditingDataEntry.getKey();
			final ClassAuditingData classAuditingData = classAuditingDataEntry.getValue();
			for ( String propertyName : classAuditingData.getNonSyntheticPropertyNames() ) {
				final Property property = pc.getProperty( propertyName );
				updateCalculatedProperty( pc.getEntityName(), property, propertyName, classAuditingData );
			}
		}
	}

	private void updateCalculatedProperty(String entityName, Property property, String propertyName, AuditedPropertiesHolder propertyHolder) {
		final PropertyAuditingData propertyAuditingData = propertyHolder.getPropertyAuditingData( propertyName );
		final boolean isAuditMappedBy = propertyAuditingData.getAuditMappedBy() != null;
		final boolean isRelationMappedBy = propertyAuditingData.getRelationMappedBy() != null;

		// handle updating the property, if applicable.
		if ( isAuditMappedBy || isRelationMappedBy ) {
			final String referencedEntityName = MappingTools.getReferencedEntityName( property.getValue() );
			final ClassAuditingData referencedAuditData = entityNameToAuditingData.get( referencedEntityName );

			if ( isAuditMappedBy ) {
				// If a property had the @AuditMappedBy annotation, setting the referenced fields to be always insertable.
				setAuditMappedByInsertable( referencedEntityName, entityName, referencedAuditData, propertyAuditingData );
			}
			else if ( isRelationMappedBy && ( property.getValue() instanceof List ) ) {
				// If a property has mappedBy= and @Indexed and isn't @AuditMappedBy, add synthetic support.
				addSyntheticIndexProperty(
						(List) property.getValue(),
						property.getPropertyAccessorName(),
						referencedAuditData
				);
			}
		}

		// HHH-9108
		// Added support to handle nested property calculations for components.
		// This is useful for AuditMappedBy inside an Embeddable that holds a collection of entities.
		if ( propertyAuditingData instanceof ComponentAuditingData ) {
			final ComponentAuditingData componentAuditingData = ( ComponentAuditingData) propertyAuditingData;
			final Component component = (Component) property.getValue();
			for ( String componentPropertyName : componentAuditingData.getNonSyntheticPropertyNames() ) {
				final Property componentProperty = component.getProperty( componentPropertyName );
				updateCalculatedProperty( entityName, componentProperty, componentPropertyName, componentAuditingData );
			}
		}
	}

	private void setAuditMappedByInsertable(
			String referencedEntityName,
			String entityName,
			ClassAuditingData referencedAuditData,
			PropertyAuditingData propertyAuditingData) {
		forcePropertyInsertable(
				referencedAuditData,
				propertyAuditingData.getAuditMappedBy(),
				entityName,
				referencedEntityName
		);

		forcePropertyInsertable(
				referencedAuditData,
				propertyAuditingData.getPositionMappedBy(),
				entityName,
				referencedEntityName
		);
	}

	private void addSyntheticIndexProperty(List value, String propertyAccessorName, ClassAuditingData classAuditingData) {
		final Value indexValue = value.getIndex();
		if ( indexValue != null && indexValue.getColumnIterator().hasNext() ) {
			final String indexColumnName = indexValue.getColumnIterator().next().getText();
			if ( indexColumnName != null ) {
				final PropertyAuditingData auditingData = new PropertyAuditingData(
						indexColumnName,
						propertyAccessorName,
						ModificationStore.FULL,
						RelationTargetAuditMode.AUDITED,
						null,
						null,
						false,
						true,
						indexValue
				);
				classAuditingData.addPropertyAuditingData( indexColumnName, auditingData );
			}
		}
	}

	private void forcePropertyInsertable(
			ClassAuditingData classAuditingData, String propertyName,
			String entityName, String referencedEntityName) {
		if ( propertyName != null ) {
			if ( classAuditingData.getPropertyAuditingData( propertyName ) == null ) {
				throw new MappingException(
						"@AuditMappedBy points to a property that doesn't exist: " +
								referencedEntityName + "." + propertyName
				);
			}

			LOG.debugf(
					"Non-insertable property %s.%s will be made insertable because a matching @AuditMappedBy was found in the %s entity",
					referencedEntityName,
					propertyName,
					entityName
			);

			classAuditingData
					.getPropertyAuditingData( propertyName )
					.setForceInsertable( true );
		}
	}
}
