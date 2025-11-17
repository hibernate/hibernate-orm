/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.mapping.PersistentClass;

/**
 * Contract for a persisted entity mapping.
 *
 * @author Chris Cranford
 */
public abstract class PersistentEntity implements AttributeContainer {

	private final AuditTableData auditTableData;
	private final PersistentClass persistentClass;

	public PersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		this.auditTableData = auditTableData;
		this.persistentClass = persistentClass;
	}

	public AuditTableData getAuditTableData() {
		return auditTableData;
	}

	protected PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public boolean isJoinAware() {
		return false;
	}

	public abstract void build(JaxbHbmHibernateMapping mapping);
}
