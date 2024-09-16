/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Embeddable;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Embeddable
@Audited
public class AuditedEmbeddableWithNoDeclaredData extends AbstractAuditedEmbeddable {

	public AuditedEmbeddableWithNoDeclaredData(int code) {
		super( code );
	}

	// Needed for @Embeddable
	protected AuditedEmbeddableWithNoDeclaredData() {
	}
}
