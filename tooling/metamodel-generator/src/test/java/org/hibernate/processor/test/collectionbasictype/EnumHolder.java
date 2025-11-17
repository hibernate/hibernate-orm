/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Entity;

@Entity
public class EnumHolder {

	public <E extends Enum<E>> E getMyEnum() {
		return null;
	}
}
