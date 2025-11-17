/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.sortedcollection;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class PrintJob {
	@Id
	@GeneratedValue
	private long id;

	@Lob
	private byte[] data;
}
