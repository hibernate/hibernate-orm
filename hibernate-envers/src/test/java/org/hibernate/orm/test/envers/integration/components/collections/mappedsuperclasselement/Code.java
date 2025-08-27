/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.collections.mappedsuperclasselement;

import jakarta.persistence.Embeddable;

/**
 * @author Jakob Braeuchi.
 */
@Embeddable
public class Code extends AbstractCode {

	public Code() {
	}

	public Code(int code) {
		super( code );
	}
}
