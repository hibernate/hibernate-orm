/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import org.hibernate.envers.AuditOverride;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ModelsContext;

/**
 * A boot-time descriptor that represents a {@link AuditOverride} annotation.
 *
 * @author Chris Cranford
 */
public class AuditOverrideData {

	private final String name;
	private final boolean audited;
	private final Class<?> forClass;
	private final AuditJoinTableData auditJoinTableData;

	public AuditOverrideData(AuditOverride auditOverride) {
		this( auditOverride, null, null );
	}

	public AuditOverrideData(AuditOverride auditOverride, AnnotationTarget annotationTarget, ModelsContext modelsContext) {
		this.name = auditOverride.name();
		this.audited = auditOverride.isAudited();
		this.forClass = auditOverride.forClass();
		this.auditJoinTableData = new AuditJoinTableData( auditOverride.auditJoinTable(), annotationTarget, modelsContext );
	}

	public String getName() {
		return name;
	}

	public boolean isAudited() {
		return audited;
	}

	public Class<?> getForClass() {
		return forClass;
	}

	public AuditJoinTableData getAuditJoinTableData() {
		return auditJoinTableData;
	}
}
