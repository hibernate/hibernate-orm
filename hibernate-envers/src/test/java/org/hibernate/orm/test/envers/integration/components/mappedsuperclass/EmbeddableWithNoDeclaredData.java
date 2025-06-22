/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Embeddable;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@Embeddable
public class EmbeddableWithNoDeclaredData extends AbstractEmbeddable {

	public EmbeddableWithNoDeclaredData(int code) {
		super( code );
	}

	// Needed for @Embeddable
	protected EmbeddableWithNoDeclaredData() {
	}
}
