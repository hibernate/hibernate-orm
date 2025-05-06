/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.sortedcollection;

import java.util.SortedMap;
import java.util.SortedSet;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.SortNatural;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Printer {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@SortNatural
	private SortedSet<PrintJob> printQueue;

	@OneToMany
	@SortNatural
	private SortedMap<String, PrintJob> printedJobs;
}
