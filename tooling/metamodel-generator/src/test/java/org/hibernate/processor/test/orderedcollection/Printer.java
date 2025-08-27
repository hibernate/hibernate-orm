/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.orderedcollection;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.Set;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Printer {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@OrderBy("id desc, data")
	private Set<PrintJob> printQueue;
}
