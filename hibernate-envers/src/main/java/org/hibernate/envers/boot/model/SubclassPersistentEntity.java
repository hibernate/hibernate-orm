/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Chris Cranford
 */
public abstract class SubclassPersistentEntity extends PersistentEntity {

	private String extendsEntityName;

	public SubclassPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
	}

	public String getExtends() {
		return extendsEntityName;
	}

	public void setExtends(String extendsEntityName) {
		this.extendsEntityName = extendsEntityName;
	}

}
