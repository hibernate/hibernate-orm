/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.async;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AsyncBook {
	@Id
	public String isbn;

	public String title;
}
