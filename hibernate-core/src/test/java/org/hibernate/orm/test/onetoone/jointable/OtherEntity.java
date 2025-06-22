/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Christian Beikov
 */
@Entity
public class OtherEntity {
	@Id
	private String id;

	public OtherEntity() {
	}

	public OtherEntity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
