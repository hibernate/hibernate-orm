/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.internal.entities.EntityConfiguration;

/**
 * A registry of audited and not-audited entity runtime configurations.
 *
 * @author Chris Cranford
 */
public class AuditEntityConfigurationRegistry {

	private final Map<String, EntityConfiguration> auditedEntityConfigurations = new HashMap<>();
	private final Map<String, EntityConfiguration> notAuditedEntityConfigurations = new HashMap<>();

	public Map<String, EntityConfiguration> getAuditedEntityConfigurations() {
		return Collections.unmodifiableMap( auditedEntityConfigurations );
	}

	public Map<String, EntityConfiguration> getNotAuditedEntityConfigurations() {
		return Collections.unmodifiableMap( notAuditedEntityConfigurations );
	}

	public boolean hasAuditedEntityConfiguration(String entityName) {
		return auditedEntityConfigurations.containsKey( entityName );
	}

	public boolean hasNotAuditedEntityConfiguration(String entityName) {
		return notAuditedEntityConfigurations.containsKey( entityName );
	}

	public EntityConfiguration getAuditedEntityConfiguration(String entityName) {
		return auditedEntityConfigurations.get( entityName );
	}

	public EntityConfiguration getNotAuditedEntityConfiguration(String entityName) {
		return notAuditedEntityConfigurations.get( entityName );
	}

	public void addAuditedEntityConfiguration(String entityName, EntityConfiguration entityConfiguration) {
		auditedEntityConfigurations.put( entityName, entityConfiguration );
	}

	public void addNotAuditedEntityConfiguration(String entityName, EntityConfiguration entityConfiguration) {
		notAuditedEntityConfigurations.put( entityName, entityConfiguration );
	}

}
