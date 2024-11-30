/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
