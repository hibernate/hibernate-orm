/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.stateful;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StatefulBook {
	@Id
	String isbn;
	String title;
}
