/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EnumType;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Value;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class PropertyAuditingData {
	private String name;
	private String beanName;
	private ModificationStore store;
	private String mapKey;
	private EnumType mapKeyEnumType;
	private AuditJoinTable joinTable;
	private String accessType;
	private final List<AuditOverride> auditJoinTableOverrides = new ArrayList<>( 0 );
	private RelationTargetAuditMode relationTargetAuditMode;
	private String auditMappedBy;
	private String relationMappedBy;
	private String positionMappedBy;
	private boolean forceInsertable;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;
	private String explicitModifiedFlagName;
	private Value value;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean syntheic;

	public PropertyAuditingData() {
	}

	public PropertyAuditingData(
			String name, String accessType, ModificationStore store,
			RelationTargetAuditMode relationTargetAuditMode,
			String auditMappedBy, String positionMappedBy,
			boolean forceInsertable) {
		this(
				name,
				accessType,
				store,
				relationTargetAuditMode,
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
			ModificationStore store,
			RelationTargetAuditMode relationTargetAuditMode,
			String auditMappedBy,
			String positionMappedBy,
			boolean forceInsertable,
			boolean syntheic,
			Value value) {
		this.name = name;
		this.beanName = name;
		this.accessType = accessType;
		this.store = store;
		this.relationTargetAuditMode = relationTargetAuditMode;
		this.auditMappedBy = auditMappedBy;
		this.positionMappedBy = positionMappedBy;
		this.forceInsertable = forceInsertable;
		this.syntheic = syntheic;
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

	/**
	 * @deprecated since 5.2, to be removed in 6.0 with no replacement.
	 */
	@Deprecated
	public ModificationStore getStore() {
		return store;
	}

	/**
	 * @deprecated since 5.2, to be removed in 6.0 with no replacement.
	 */
	@Deprecated
	public void setStore(ModificationStore store) {
		this.store = store;
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

	// todo (6.0) - remove this and use #resolvePropertyData instead
	public PropertyData getPropertyData() {
		return resolvePropertyData( null );
	}

	public PropertyData resolvePropertyData(Type propertyType) {
		return new PropertyData(
				name,
				beanName,
				accessType,
				store,
				usingModifiedFlag,
				modifiedFlagName,
				syntheic,
				propertyType
		);
	}

	public PropertyData resolvePropertyData(Type propertyType, Type virtualType) {
		return new PropertyData(
				name,
				beanName,
				accessType,
				store,
				usingModifiedFlag,
				modifiedFlagName,
				syntheic,
				propertyType,
				virtualType.getReturnedClass()
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

	public boolean isSyntheic() {
		return syntheic;
	}

	public Value getValue() {
		return value;
	}
}
