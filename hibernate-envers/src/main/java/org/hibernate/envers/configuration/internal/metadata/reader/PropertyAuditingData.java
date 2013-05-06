/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.internal.entities.PropertyData;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class PropertyAuditingData {
	private String name;
	private String beanName;
	private ModificationStore store;
	private String mapKey;
	private AuditJoinTable joinTable;
	private String accessType;
	private final List<AuditOverride> auditJoinTableOverrides = new ArrayList<AuditOverride>( 0 );
	private RelationTargetAuditMode relationTargetAuditMode;
	private String auditMappedBy;
	private String relationMappedBy;
	private String positionMappedBy;
	private boolean forceInsertable;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;

	public PropertyAuditingData() {
	}

	public PropertyAuditingData(
			String name, String accessType, ModificationStore store,
			RelationTargetAuditMode relationTargetAuditMode,
			String auditMappedBy, String positionMappedBy,
			boolean forceInsertable) {
		this.name = name;
		this.beanName = name;
		this.accessType = accessType;
		this.store = store;
		this.relationTargetAuditMode = relationTargetAuditMode;
		this.auditMappedBy = auditMappedBy;
		this.positionMappedBy = positionMappedBy;
		this.forceInsertable = forceInsertable;
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

	public ModificationStore getStore() {
		return store;
	}

	public void setStore(ModificationStore store) {
		this.store = store;
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
		return new PropertyData(
				name, beanName, accessType, store,
				usingModifiedFlag, modifiedFlagName
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

}
