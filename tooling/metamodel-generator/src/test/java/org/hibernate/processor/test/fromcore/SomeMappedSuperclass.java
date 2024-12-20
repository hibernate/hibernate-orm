/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public class SomeMappedSuperclass {
	private Long id;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
