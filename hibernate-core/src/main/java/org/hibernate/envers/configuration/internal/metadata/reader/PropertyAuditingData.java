/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class PropertyAuditingData {
	private String name;
	private String beanName;
	private String mapKey;
	private AuditJoinTable joinTable;
	private String accessType;
	private final List<AuditOverride> auditJoinTableOverrides = new ArrayList<>( 0 );
	private RelationTargetAuditMode relationTargetAuditMode;
	private RelationTargetNotFoundAction relationTargetNotFoundAction;
	private String auditMappedBy;
	private String relationMappedBy;
	private String positionMappedBy;
	private boolean forceInsertable;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;
	private Value value;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean synthetic;

	public PropertyAuditingData() {
	}

	public PropertyAuditingData(
			String name,
			String accessType,
			RelationTargetAuditMode relationTargetAuditMode,
			String auditMappedBy,
			String positionMappedBy,
			boolean forceInsertable) {
		this(
				name,
				accessType,
				relationTargetAuditMode,
				RelationTargetNotFoundAction.ERROR,
				auditMappedBy,
				positionMappedBy,
				forceInsertable,
				false,
				null
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

	public AuditJoinTable getJoinTable() {
		return joinTable;
	}

	public void setJoinTable(AuditJoinTable joinTable) {
		this.joinTable = joinTable;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public PropertyData getPropertyData() {
		return resolvePropertyData( null );
	}

	public PropertyData resolvePropertyData(JavaTypeDescriptor javaTypeDescriptor) {
		return new PropertyData(
				name,
				beanName,
				accessType,
				usingModifiedFlag,
				modifiedFlagName,
				synthetic,
				javaTypeDescriptor
		);
	}

	public PropertyData resolvePropertyData(
			JavaTypeDescriptor propertyDescriptor,
			JavaTypeDescriptor virtualDescriptor) {
		return new PropertyData(
				name,
				beanName,
				accessType,
				usingModifiedFlag,
				modifiedFlagName,
				synthetic,
				propertyDescriptor,
				virtualDescriptor.getJavaType()
		);
	}

	public List<AuditOverride> getAuditingOverrides() {
		return auditJoinTableOverrides;
	}

	public String getAuditMappedBy() {
		return auditMappedBy;
	}

	public void setAuditMappedBy(String auditMappedBy) {
		this.auditMappedBy = auditMappedBy;
	}

	public String getRelationMappedBy() {
		return relationMappedBy;
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

	public void addAuditingOverride(AuditOverride annotation) {
		if ( annotation != null ) {
			final String overrideName = annotation.name();
			boolean present = false;
			for ( AuditOverride current : auditJoinTableOverrides ) {
				if ( current.name().equals( overrideName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				auditJoinTableOverrides.add( annotation );
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
}
