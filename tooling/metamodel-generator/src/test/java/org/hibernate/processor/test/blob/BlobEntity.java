/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.blob;

import java.sql.Blob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Table
public class BlobEntity {
	@Id
	private String id;

	@Lob
	private Blob blob;
}
