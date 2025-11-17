/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EnumType;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.CollectionAuditTable;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Value;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.Type;

/**
 * The boot-time representation of an audited property.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class PropertyAuditingData {
	private String name;
	private String beanName;
	private String mapKey;
	private EnumType mapKeyEnumType;
	private CollectionAuditTable collectionAuditTable;
	private AuditJoinTableData joinTable;
	private String accessType;
	private final List<AuditOverrideData> auditJoinTableOverrides = new ArrayList<>( 0 );
	private RelationTargetAuditMode relationTargetAuditMode;
	private RelationTargetNotFoundAction relationTargetNotFoundAction;
	private String auditMappedBy;
	private String relationMappedBy;
	private String positionMappedBy;
	private boolean forceInsertable;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;
	private String explicitModifiedFlagName;
	private Value value;
	private Type propertyType;
	private Type virtualPropertyType;
	private PropertyAccessStrategy propertyAccessStrategy;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean synthetic;

	public PropertyAuditingData() {
	}

	/**
	 * Create a property with the default {@link RelationTargetAuditMode} mode of AUDITED.
	 *
	 * @param name the property name
	 * @param accessType the access type
	 * @param forceInsertable whether the property is forced insertable
	 */
	public PropertyAuditingData(
			String name,
			String accessType,
			boolean forceInsertable) {
		this(
				name,
				accessType,
				RelationTargetAuditMode.AUDITED,
				RelationTargetNotFoundAction.DEFAULT,
				null,
				null,
				forceInsertable,
				false,
				null
		);
	}

	/**
	 * Create a property with the default {@link RelationTargetAuditMode} mode of AUDITED.
	 *
	 * @param name the property name
	 * @param accessType the access type
	 * @param relationTargetNotFoundAction the relation target not found action
	 * @param forceInsertable whether the property is forced insertable
	 * @param synthetic whether the property is a synthetic, non-logic column-based property
	 * @param value the mapping model's value
	 */
	public PropertyAuditingData(
			String name,
			String accessType,
			RelationTargetNotFoundAction relationTargetNotFoundAction,
			boolean forceInsertable,
			boolean synthetic,
			Value value) {
		this(
				name,
				accessType,
				RelationTargetAuditMode.AUDITED,
				relationTargetNotFoundAction,
				null,
				null,
				forceInsertable,
				synthetic,
				value
		);
	}

	public PropertyAuditingData(
			String name,
			String accessType,
			RelationTargetAuditMode relationTargetAuditMode,
			RelationTargetNotFoundAction relationTargetNotFoundAction,
			String auditMappedBy,
			String positionMappedBy,
			boolean forceInsertable,
			boolean synthetic,
			Value value) {
		this.name = name;
		this.beanName = name;
		this.accessType = accessType;
		this.relationTargetAuditMode = relationTargetAuditMode;
		this.relationTargetNotFoundAction = relationTargetNotFoundAction;
		this.auditMappedBy = auditMappedBy;
		this.positionMappedBy = positionMappedBy;
		this.forceInsertable = forceInsertable;
		this.synthetic = synthetic;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getMapKey() {
		return mapKey;
	}

	public void setMapKey(String mapKey) {
		this.mapKey = mapKey;
	}

	public EnumType getMapKeyEnumType() {
		return mapKeyEnumType;
	}

	public void setMapKeyEnumType(EnumType mapKeyEnumType) {
		this.mapKeyEnumType = mapKeyEnumType;
	}

	public AuditJoinTableData getJoinTable() {
		return joinTable;
	}

	public void setJoinTable(AuditJoinTableData joinTable) {
		this.joinTable = joinTable;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public List<AuditOverrideData> getAuditingOverrides() {
		return Collections.unmodifiableList( auditJoinTableOverrides );
	}

	public String getAuditMappedBy() {
		return auditMappedBy;
	}

	public boolean hasAuditedMappedBy() {
		return auditMappedBy != null;
	}

	public void setAuditMappedBy(String auditMappedBy) {
		this.auditMappedBy = auditMappedBy;
	}

	public String getRelationMappedBy() {
		return relationMappedBy;
	}

	public boolean hasRelationMappedBy() {
		return relationMappedBy != null;
	}

	public void setRelationMappedBy(String relationMappedBy) {
		this.relationMappedBy = relationMappedBy;
	}

	public String getPositionMappedBy() {
		return positionMappedBy;
	}

	public void setPositionMappedBy(String positionMappedBy) {
		this.positionMappedBy = positionMappedBy;
	}

	public boolean isForceInsertable() {
		return forceInsertable;
	}

	public void setForceInsertable(boolean forceInsertable) {
		this.forceInsertable = forceInsertable;
	}

	public boolean isUsingModifiedFlag() {
		return usingModifiedFlag;
	}

	public void setUsingModifiedFlag(boolean usingModifiedFlag) {
		this.usingModifiedFlag = usingModifiedFlag;
	}

	public String getModifiedFlagName() {
		return modifiedFlagName;
	}

	public void setModifiedFlagName(String modifiedFlagName) {
		this.modifiedFlagName = modifiedFlagName;
	}

	public boolean isModifiedFlagNameExplicitlySpecified() {
		return !StringTools.isEmpty( explicitModifiedFlagName );
	}

	public String getExplicitModifiedFlagName() {
		return explicitModifiedFlagName;
	}

	public void setExplicitModifiedFlagName(String modifiedFlagName) {
		this.explicitModifiedFlagName = modifiedFlagName;
	}

	public void addAuditingOverride(AuditOverride annotation) {
		if ( annotation != null ) {
			final String overrideName = annotation.name();
			boolean present = false;
			for ( AuditOverrideData current : auditJoinTableOverrides ) {
				if ( current.getName().equals( overrideName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				auditJoinTableOverrides.add( new AuditOverrideData( annotation ) );
			}
		}
	}

	public void addAuditingOverrides(AuditOverrides annotationOverrides) {
		if ( annotationOverrides != null ) {
			for ( AuditOverride annotation : annotationOverrides.value() ) {
				addAuditingOverride( annotation );
			}
		}
	}

	/**
	 * Get the relationTargetAuditMode property.
	 *
	 * @return the relationTargetAuditMode property value
	 */
	public RelationTargetAuditMode getRelationTargetAuditMode() {
		return relationTargetAuditMode;
	}

	/**
	 * Set the relationTargetAuditMode property value.
	 *
	 * @param relationTargetAuditMode the relationTargetAuditMode to set
	 */
	public void setRelationTargetAuditMode(RelationTargetAuditMode relationTargetAuditMode) {
		this.relationTargetAuditMode = relationTargetAuditMode;
	}

	public RelationTargetNotFoundAction getRelationTargetNotFoundAction() {
		return relationTargetNotFoundAction;
	}

	public void setRelationTargetNotFoundAction(RelationTargetNotFoundAction relationTargetNotFoundAction) {
		this.relationTargetNotFoundAction = relationTargetNotFoundAction;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public Type getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(Type propertyType) {
		this.propertyType = propertyType;
	}

	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccessStrategy;
	}

	public void setPropertyAccessStrategy(PropertyAccessStrategy propertyAccessStrategy) {
		this.propertyAccessStrategy = propertyAccessStrategy;
	}

	public Type getVirtualPropertyType() {
		return virtualPropertyType;
	}

	public void setVirtualPropertyType(Type virtualPropertyType) {
		this.virtualPropertyType = virtualPropertyType;
	}

	public CollectionAuditTable getCollectionAuditTable() {
		return collectionAuditTable;
	}

	public void setCollectionAuditTable(CollectionAuditTable collectionAuditTable) {
		this.collectionAuditTable = collectionAuditTable;
	}

	public PropertyData resolvePropertyData() {
		if ( propertyType != null && virtualPropertyType != null ) {
			return new PropertyData(
					name,
					beanName,
					accessType,
					usingModifiedFlag,
					modifiedFlagName,
					synthetic,
					propertyType,
					virtualPropertyType.getReturnedClass(),
					propertyAccessStrategy
			);
		}
		else if ( propertyType != null ) {
			return new PropertyData(
					name,
					beanName,
					accessType,
					usingModifiedFlag,
					modifiedFlagName,
					synthetic,
					propertyType,
					propertyAccessStrategy
			);
		}
		return new PropertyData(
				name,
				beanName,
				accessType,
				usingModifiedFlag,
				modifiedFlagName,
				synthetic,
				null,
				propertyAccessStrategy
		);
	}
}
