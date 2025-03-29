/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.deep;

import java.util.Date;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * A mapped super class that does not define an id attribute.
 *
 * @author Igor Vaynberg
 */
@MappedSuperclass
public abstract class PersistenceBase {
	Date created;

	@PrePersist
	void prePersist() {
		created = new Date();
	}
}
