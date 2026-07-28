/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.securityannotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SecuredBook {
	@Id
	String isbn;

	String title;

	protected SecuredBook() {
	}
}
