/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class DataRestrictionBook {
	@Id
	String isbn;
	String title;
	int pages;
	@ManyToOne
	DataRestrictionPublisher publisher;
}
