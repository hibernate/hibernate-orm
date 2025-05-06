/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesHolder;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.List;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.jboss.logging.Logger;

/**
 * A helper class holding auditing meta-data for all persistent classes during boot-time.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class ClassesAuditingData {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversMessageLogger.class,
			ClassesAuditingData.class.getName()
	);

	private final Map<String, ClassAuditingData> entityNameToAuditingData = new HashMap<>();
	private final Map<PersistentClass, ClassAuditingData> persistentClassToAuditingData = new LinkedHashMap<>();

	/**
	 * Stores information about auditing meta-data for the given class.
	 *
	 * @param cad Auditing meta-data for the given class.
	 */
	public void addClassAuditingData(ClassAuditingData cad) {
		entityNameToAuditingData.put( cad.getEntityName(), cad );
		persistentClassToAuditingData.put( cad.getPersistentClass(), cad );
	}

	/**
	 * @return A collection of all auditing meta-data for persistent classes.
	 */
	public Collection<ClassAuditingData> getAllClassAuditedData() {
		return persistentClassToAuditingData.values();
	}

	/**
	 * After all meta-data is read, updates calculated fields. This includes:
	 * <ul>
	 * <li>setting {@code forceInsertable} to {@code true} for properties specified by {@code @AuditMappedBy}</li>
	 * <li>adding {@code synthetic} properties to mappedBy relations which have {@code IndexColumn} or {@code OrderColumn}.</li>
	 * </ul>
	 */
	public void updateCalculatedFields() {
		for ( ClassAuditingData classAuditingData : persistentClassToAuditingData.values() ) {
			final PersistentClass persistentClass = classAuditingData.getPersistentClass();
			for ( String propertyName : classAuditingData.getNonSyntheticPropertyNames() ) {
				final Property property = persistentClass.getProperty( propertyName );
				updateCalculatedProperty( persistentClass.getEntityName(), property, propertyName, classAuditingData );
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

		if ( propertyAuditingData.getMapKeyEnumType() != null ) {
			final String referencedEntityName = MappingTools.getReferencedEntityName( property.getValue() );
			if ( referencedEntityName != null ) {
				// If no entity could be determined, this means the enum type isn't an entity mapping and instead is one
				// to a basic type.  In this use case, there is nothing special to do.
				final ClassAuditingData referencedAuditingData = entityNameToAuditingData.get( referencedEntityName );
				addMapEnumeratedKey( property.getValue(), property.getPropertyAccessorName(), referencedAuditingData );
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
		if ( indexValue != null && indexValue.getSelectables().size() > 0 ) {
			final String indexColumnName = indexValue.getSelectables().get( 0 ).getText();
			if ( indexColumnName != null ) {
				final PropertyAuditingData auditingData = new PropertyAuditingData(
						indexColumnName,
						propertyAccessorName,
						RelationTargetNotFoundAction.ERROR,
						false,
						true,
						indexValue
				);
				classAuditingData.addPropertyAuditingData( indexColumnName, auditingData );
			}
		}
	}

	private void addMapEnumeratedKey(Value value, String propertyAccessorName, ClassAuditingData classAuditingData) {
		if ( value instanceof org.hibernate.mapping.Map ) {
			final Value indexValue = ( (org.hibernate.mapping.Map) value ).getIndex();
			if ( indexValue != null && indexValue.getSelectables().size() > 0 ) {
				final String indexColumnName = indexValue.getSelectables().get( 0 ).getText();
				if ( !StringTools.isEmpty( indexColumnName ) ) {
					final PropertyAuditingData propertyAuditingData = new PropertyAuditingData(
							indexColumnName,
							propertyAccessorName,
							RelationTargetNotFoundAction.ERROR,
							true,
							true,
							indexValue
					);

					classAuditingData.addPropertyAuditingData( indexColumnName, propertyAuditingData );
				}
			}
		}
	}

	private void forcePropertyInsertable(
			ClassAuditingData classAuditingData,
			String propertyName,
			String entityName,
			String referencedEntityName) {
		if ( propertyName != null ) {
			if ( classAuditingData.getPropertyAuditingData( propertyName ) == null ) {
				throw new EnversMappingException(
						String.format(
								Locale.ENGLISH,
								"@AuditMappedBy points to a property that doesn't exist: %s.%s",
								referencedEntityName,
								propertyName
						)
				);
			}

			LOG.debugf(
					"Non-insertable property %s.%s will be made insertable because a matching @AuditMappedBy was found in the %s entity",
					referencedEntityName,
					propertyName,
					entityName
			);

			classAuditingData.getPropertyAuditingData( propertyName ).setForceInsertable( true );
		}
	}
}
